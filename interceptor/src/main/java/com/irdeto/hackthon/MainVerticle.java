package com.irdeto.hackthon;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private KafkaProducer<String, String> requestProducer;
    private KafkaProducer<String, String> responseProducer;

    private final Map<String, RoutingContext> contextMap = new HashMap<>();
    private WebClient client;

    public static void main(final String[] args) {
        VertxOptions vertxOptions =
                new VertxOptions().setInternalBlockingPoolSize(1).setWorkerPoolSize(1);
        final Vertx vertx = Vertx.vertx(vertxOptions);

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
                .flatMap(options -> vertx
                        .rxDeployVerticle(MainVerticle.class.getName(), options)
                        .map(any -> options))
                .subscribe(
                        httpDeployId -> LOGGER.info("Application deployed successfully."),
                        Throwable::printStackTrace
                );
    }

    @Override
    public void start(Promise<Void> startPromise) {

        client = WebClient.create(vertx, new WebClientOptions().setMaxPoolSize(100));
        responseProducer = createProducer();

        requestProducer = createProducer();
        KafkaConsumer<String, String> responseConsumer = createConsumer();
        responseConsumer.subscribe(config().getString("RESPONSE_TOPIC"));
        responseConsumer.handler(this::handleKafkaResponse);

        KafkaConsumer<String, String> requestConsumer = createConsumer();
        requestConsumer.subscribe(config().getString("REQUEST_TOPIC"));
        requestConsumer.handler(this::handleKafkaRequest);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(this::handleRequest);
        int port = config().getInteger("PORT");
        vertx.createHttpServer().requestHandler(router).listen(port, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                System.out.println("HTTP server started on port " + port);
            } else {
                startPromise.fail(http.cause());
            }
        });
    }

    private void handleRequest(RoutingContext routingContext) {
        String requestId = UUID.randomUUID().toString();
        KafkaProducerRecord<String, String> requestRecord =
                KafkaProducerRecord.create(
                        config().getString("REQUEST_TOPIC"),
                        requestId,
                        routingContext.getBodyAsString()
                );
        requestProducer.send(requestRecord);
        contextMap.put(requestId, routingContext);
    }

    private void handleKafkaResponse(
            KafkaConsumerRecord<String, String> kafkaRecord
    ) {
        RoutingContext routingContext = contextMap.remove(kafkaRecord.key());
        if (routingContext != null) {
            routingContext.response().write(kafkaRecord.value()).end();
        }
    }

    private void handleKafkaRequest(
            KafkaConsumerRecord<String, String> kafkaRecord
    ) {
        KafkaProducerRecord<String, String> responseRecord =
                KafkaProducerRecord.create(
                        config().getString("RESPONSE_TOPIC"),
                        kafkaRecord.key(),
                        kafkaRecord.value()
                );
        client
                .post(config().getString("SERVICE_API"))
                .sendBuffer(Buffer.buffer(kafkaRecord.value()), response ->
                        responseProducer.send(responseRecord));
    }

    private KafkaConsumer<String, String> createConsumer() {
        Map<String, String> consumerConfig = new HashMap<>();
        consumerConfig.put("bootstrap.servers", config().getString("KAFKA_ADDR"));
        consumerConfig.put(
                "key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer"
        );
        consumerConfig.put(
                "value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer"
        );
        consumerConfig.put("group.id", "serviceA");
        consumerConfig.put("auto.offset.reset", "earliest");
        consumerConfig.put("enable.auto.commit", "false");
        return KafkaConsumer.create(vertx, consumerConfig);
    }

    private KafkaProducer<String, String> createProducer() {
        Map<String, String> producerConfig = new HashMap<>();
        producerConfig.put("bootstrap.servers", config().getString("KAFKA_ADDR"));
        producerConfig.put(
                "key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer"
        );
        producerConfig.put(
                "value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer"
        );
        producerConfig.put("acks", "1");
        return KafkaProducer.create(vertx, producerConfig);
    }
}
