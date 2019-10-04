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

        final Integer port = config().getInteger("PORT");
        vertx.createHttpServer().requestHandler(router::accept)
                .listen(port, httpServerAsyncResult -> {
                    if (httpServerAsyncResult.succeeded()) {
                        LOGGER.info("Http verticle deployed successfully at port: {}", port);
                        startFuture.complete();
                    } else {
                        LOGGER.info("Http verticle failed to deploy at port: {}", port);
                        startFuture.failed();
                    }
                });
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
        Single.just(routingContext)
                .map(rc -> rc.getBodyAsJson().mapTo(DataDto.class))
                .map(Json::encodePrettily)
                .doOnSuccess(data -> vertx.eventBus().send("CLIENT", data))
                .subscribe(
                        data -> routingContext.response().end(Json.encodePrettily(data)),
                        error -> routingContext.response().end(error.getMessage())
                );
    }
}