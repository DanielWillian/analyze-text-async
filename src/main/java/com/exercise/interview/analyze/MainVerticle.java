package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rxjava3.SingleHelper;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MainVerticle extends AbstractVerticle {
    private AnalyzeService analyzeService;
    private TextRepository textRepository;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        Future<Void> loadTexts = configRetriever.getConfig()
                .flatMap(c -> {
                    SqlClient client = createSqlClient(c);

                    textRepository = new TextRepositoryImpl(client);
                    analyzeService = new AnalyzeServiceImpl(textRepository);

                    return loadCache(c);
                });

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/analyze").respond(context -> {
            String text = Optional.of(context.body())
                    .map(RequestBody::asJsonObject)
                    .map(j -> j.getString("text"))
                    .orElseThrow(InvalidRequestException::new);

            log.debug("Handling analyzing of text: {}", text);

            Single<AnalyzeResponse> response = analyzeService.analyze(text);

            return SingleHelper.toFuture(response)
                    .onFailure(t -> log.error("Could not analyze text", t));
        }).failureHandler(context -> {
            Throwable t = context.failure();
            log.error("Failed handling request", t);
            int statusCode = t instanceof InvalidRequestException ? 400 : 500;
            context.response()
                    .setStatusCode(statusCode)
                    .end();
        });

        Future<HttpServer> httpServer = vertx.createHttpServer().requestHandler(router).listen(8888)
                .onSuccess(http -> log.info("HTTP server started on port 8888"));

        Future.join(loadTexts, httpServer)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(ar.cause());
                    }
                });
    }

    private SqlClient createSqlClient(JsonObject config) {
        if (!isToUseDb(config)) return null;

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config.getInteger("PGPORT", 5432))
                .setHost(config.getString("PGHOST", "localhost"))
                .setDatabase(config.getString("PGDATABASE", "postgres"))
                .setUser(config.getString("PGUSER", "postgres"))
                .setPassword(config.getString("PGPASSWORD", "postgres"));

        PoolOptions poolOptions = new PoolOptions().setMaxSize(config.getInteger("PGCONNECTIONS", 20));

        return PgPool.client(vertx, connectOptions, poolOptions);
    }

    private Future<Void> loadCache(JsonObject config) {
        if (!isToUseDb(config)) return Future.succeededFuture();

       return textRepository.loadTexts();
    }

    private boolean isToUseDb(JsonObject config) {
        return !"false".equalsIgnoreCase(config.getString("USE_DB", "true"));
    }
}
