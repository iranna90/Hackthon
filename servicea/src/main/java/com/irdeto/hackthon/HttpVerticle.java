package com.irdeto.hackthon;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpVerticle.class);
    private static final String CONTEXT_ROOT = "/servicea";

    @Override
    public void start(final Future<Void> startFuture) {

        Router router = Router.router(vertx);
        router.route()
                .handler(BodyHandler.create())
                .failureHandler(this::handleError);
        // Heartbeat
        router.post(CONTEXT_ROOT + "/post")
                .method(HttpMethod.POST)
                .consumes("application/json")
                .handler(this::handlePost);

        router.post(CONTEXT_ROOT + "/get")
                .method(HttpMethod.GET)
                .handler(this::handleGet);

        vertx.createHttpServer().requestHandler(router)
                .rxListen(config().getInteger("PORT"))
                .subscribe(
                        any -> {
                            startFuture.complete();
                            LOGGER.info("Successfully deployed");
                        },
                        error -> {
                            error.printStackTrace();
                            startFuture.fail(error);
                        }
                );

    }

    private void handleError(RoutingContext routingContext) {
        routingContext.response().setStatusCode(503).end("Error occurred");
    }

    private void handleGet(RoutingContext routingContext) {

        DataDto data = new DataDto(1);
        LOGGER.info("Data is {}", data);
        routingContext.response().end(Json.encodePrettily(data));
    }

    private void handlePost(RoutingContext routingContext) {
        DataDto dataDto = routingContext.getBodyAsJson().mapTo(DataDto.class);
        Single.just(dataDto)
                .map(Json::encodePrettily)
                .flatMap(data -> vertx.eventBus().rxSend("CLIENT", data))
                .subscribe(
                        data -> routingContext.response().end(Json.encodePrettily(dataDto)),
                        error -> routingContext.response().end(error.getMessage())
                );
    }
}