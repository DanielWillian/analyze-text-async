package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rxjava3.SingleHelper;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

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
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config().getInteger("PGPORT", 5432))
                .setHost(config().getString("PGHOST", "localhost"))
                .setDatabase(config().getString("PGDATABASE", "postgres"))
                .setUser(config().getString("PGUSER", "postgres"))
                .setPassword(config().getString("PGPASSWORD", "postgres"));

        PoolOptions poolOptions = new PoolOptions().setMaxSize(config().getInteger("PGCONNECTIONS", 20));

        SqlClient client = PgPool.client(vertx, connectOptions, poolOptions);

        textRepository = new TextRepositoryImpl(client);
        analyzeService = new AnalyzeServiceImpl(textRepository);

        Future<Void> loadTexts =  textRepository.loadTexts();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/analyze").respond(context -> {
            String text = context.body().asJsonObject().getString("text");

            log.info("Handling analyzing of text: {}", text);

            Single<AnalyzeResponse> response = analyzeService.analyze(Single.just(text));

            return SingleHelper.toFuture(response)
                    .onFailure(t -> log.error("Could not analyze text", t));
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
}
