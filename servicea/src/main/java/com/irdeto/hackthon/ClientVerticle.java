package com.irdeto.hackthon;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.WebClient;

public class ClientVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientVerticle.class);

    private WebClient webClient;
    private String host;

    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer("CLIENT", this::sendToServiceb);
        WebClientOptions webClientOptions = new WebClientOptions().setMaxPoolSize(100);
        webClient = WebClient.create(vertx, webClientOptions);
        host = config().getString("SERVICE_HOST");
        startFuture.complete();
    }

    private void sendToServiceb(Message<String> message) {
        Single.just(message)
                .map(Message::body)
                .flatMap(data -> webClient.postAbs(host + "/data").rxSendJson(data))
                .subscribe(
                        response -> {
                            LOGGER.info("Successful sent request");
                            message.reply("done");
                        },
                        error -> {
                            error.printStackTrace();
                            message.fail(1, error.getMessage());
                        }
                );
    }
}
