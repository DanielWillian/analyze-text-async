package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.SingleHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainVerticle extends AbstractVerticle {
    private AnalyzeService analyzeService;
    private TextRepository textRepository;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

        textRepository = new TextRepositoryImpl();
        analyzeService = new AnalyzeServiceImpl(textRepository);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/analyze").respond(context -> {
            String text = context.body().asJsonObject().getString("text");

            log.info("Handling analyzing of text: {}", text);

            Single<AnalyzeResponse> response = analyzeService.analyze(Single.just(text));

            return SingleHelper.toFuture(response)
                    .onFailure(t -> log.error("Could not analyze text", t));
        });

        vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                System.out.println("HTTP server started on port 8888");
            } else {
                startPromise.fail(http.cause());
            }
        });
    }
}
