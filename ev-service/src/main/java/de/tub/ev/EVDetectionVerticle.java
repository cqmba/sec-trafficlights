package de.tub.ev;

import de.tub.ev.dispatch.EVDispatchService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EVDetectionVerticle extends AbstractVerticle {

    private static final String SERVICE_NAME = "ev-service";
    private static final String API_MOCK_SENSOR_DETECT = "/sensors/:tlId";
    private static final String TARGET_SERVICE = "api-gateway";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8087;

    private static final Logger logger = LogManager.getLogger(EVDetectionVerticle.class);

    private EVDispatchService service;
    private ServiceDiscovery discovery;
    private String endpoint;
    protected Set<Record> registeredRecords = new ConcurrentHashSet<>();

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        discovery = ServiceDiscovery.create(vertx);

        mockDiscoveryEndpoints();
        retrieveEndpoint(TARGET_SERVICE);
        service = EVDispatchService.getInstance(endpoint, vertx, config());

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_MOCK_SENSOR_DETECT).handler(this::apiRequestOnEVDetection);

        //these are default values, the real values are loaded from config
        final String keystorepass = config().getString("keystore.password", "password");
        final String keystorepath = config().getString("keystore.path", "ev_keystore.jks");

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
                .setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath));

        createHttpServer(router,DEFAULT_HOST,DEFAULT_PORT, options)
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, DEFAULT_HOST, DEFAULT_PORT));
    }

    private void apiRequestOnEVDetection(RoutingContext routingContext) {
        int id;
        try {
            id = Integer.parseInt(routingContext.pathParam("tlId"));
        } catch (Exception ex){
            routingContext.fail(400);
            return;
        }
        if (id < 0 || id > 3){
            logger.debug("Sensor was requested for wrong id");
            routingContext.fail(400);
        }
        service.sendSensorDetection(id, routingContext);
    }

    private void retrieveEndpoint(String service){
        final String defaultEndpoint = "https://localhost:8787/";
        discovery.getRecord( r -> r.getName().equals(service), ar -> {
            if(ar.succeeded()){
                if (ar.result() != null){
                    logger.debug("Success in locating resource");
                    Record retrieved = ar.result();
                    if (retrieved.getLocation().containsKey("endpoint")){
                        this.endpoint = retrieved.getLocation().getString("endpoint");
                    } else {
                        logger.debug("Could not retrieve an endpoint for the Record");
                    }
                } else {
                    logger.debug("failed: no matching service");
                }
            } else {
                logger.debug("lookup failed, going to default location");
            }
            if(endpoint == null){
                this.endpoint = defaultEndpoint;
            }
        });
    }

    private void mockDiscoveryEndpoints(){
        final boolean ssl = true;

        final String gwDefaultHost = "localhost";
        final int gwDefaultPort = 8787;
        final String gwServiceName = "api-gateway";
        final String gwRoot = "";

        Record record = HttpEndpoint.createRecord(gwServiceName, ssl, gwDefaultHost, gwDefaultPort, gwRoot, new JsonObject().put("api.name", "lights"));
        discovery.publish(record, ar -> {
            if (ar.succeeded()){
                Record publishedRecord = ar.result();
                logger.debug("Service "+ publishedRecord.getName() + " published on endpoint " + publishedRecord.getLocation().getString("endpoint"));
            } else {
                logger.error(" Service" + record.getName() + " could not be published");
            }
        });
    }

    protected Future<HttpServer> createHttpServer(Router router, String host, int port, HttpServerOptions options) {
        Promise<HttpServer> httpServerPromise = Promise.promise();
        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(port, host, httpServerPromise);
        logger.info("Http Server started at " + host +port);
        return httpServerPromise.future().map(r -> null);
    }

    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", config().getString("api.name", ""))
        );
        return publish(record);
    }

    private Future<Void> publish(Record record) {
        if (discovery == null) {
            try {
                start();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create discovery service");
            }
        }
        Promise<Void> promise = Promise.promise();
        // publish the service
        discovery.publish(record, ar -> {
            if (ar.succeeded()) {
                registeredRecords.add(record);
                logger.info("Service <" + ar.result().getName() + "> published");
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        List<Promise> promises = new ArrayList<>();
        registeredRecords.forEach(record -> {
            Promise<Void> cleanupPromise = Promise.promise();
            promises.add(cleanupPromise);
            discovery.unpublish(record.getRegistration(), cleanupPromise);
        });
        List<Future> allFutures = new ArrayList<>();
        promises.forEach(p -> allFutures.add(p.future()));
        if (promises.isEmpty()) {
            discovery.close();
            promise.complete();
        } else {
            CompositeFuture.all(allFutures)
                    .setHandler(ar -> {
                        discovery.close();
                        if (ar.failed()) {
                            promise.fail(ar.cause());
                        } else {
                            promise.complete();
                        }
                    });
        }
    }
}
