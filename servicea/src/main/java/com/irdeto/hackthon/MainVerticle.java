package com.irdeto.hackthon;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(final String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        VertxOptions options = new VertxOptions().setInternalBlockingPoolSize(1).setWorkerPoolSize(1);
        final Vertx vertx = Vertx.vertx(options);
        vertx.rxDeployVerticle(MainVerticle.class.getName())
                .subscribe(
                        success -> LOGGER.info("Application deployed successfully."),
                        Throwable::printStackTrace);
    }

    @Override
    public void init(final io.vertx.core.Vertx vertx, final Context context) {
        super.init(vertx, context);
        JacksonConfig.registerModulesToJackson();
    }

    @Override
    public void start(final Future<Void> startFuture) {

        // config
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "application.properties"));

        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");

        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions();
        retrieverOptions.addStore(fileStore).addStore(envStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, retrieverOptions);

        retriever.rxGetConfig()
                .map(config -> new DeploymentOptions().setConfig(config))
                .flatMap(options -> vertx.rxDeployVerticle(HttpVerticle.class.getName(), options).map(any -> options))
                .subscribe(
                        httpDeployId -> startFuture.complete(),
                        startFuture::fail);
    }
}
