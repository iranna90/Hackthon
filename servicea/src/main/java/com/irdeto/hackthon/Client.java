package com.irdeto.hackthon;

import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;

public class Client extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        vertx.eventBus().consumer("CLIENT", this::sendToServiceb);
    }

    private void sendToServiceb(Message<String> message) {

    }
}
