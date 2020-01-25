package de.tub.ev;

import de.tub.ev.dispatch.EVDispatchService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.JDBCDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EVDetectionVerticle extends AbstractVerticle {

    private static final String BASE_TLC_API = "/api/lights/";
    private static final String SERVICE_NAME = "ev-service";
    private static final String API_MOCK_SENSOR_DETECT = "/sensors/:tlId";
    private static final String TARGET_SERVICE = "api-gateway";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8087;

    private static final Logger logger = LogManager.getLogger(EVDetectionVerticle.class);

    private EVDispatchService service;
    private HttpClient client;
    private ServiceDiscovery discovery;
    protected Set<Record> registeredRecords = new ConcurrentHashSet<>();

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        discovery = ServiceDiscovery.create(vertx);

        mockDiscoveryEndpoints();
        //service = EVDispatchService.getInstance(client, vertx);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_MOCK_SENSOR_DETECT).handler(this::apiRequestOnEVDetection);

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
        retrieveEndpoint(TARGET_SERVICE, id, routingContext);
    }

    //TODO make this work with discovery
    private void doDispatch(RoutingContext routingContext, int id){
        final String truststorepath = config().getString("truststore.path", "ev_truststore.jks");
        final String truststorepass = config().getString("truststore.pass", "password");

        WebClientOptions options = new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
                .setVerifyHost(true);

        //final String path = BASE_TLC_API + id +"/colors";
        final String pathAbs = "https://localhost:8787/api/lights/" + id + "/colors";
        JsonObject payload = new JsonObject().put("color", "GREEN").put("group", 1);
        WebClient webClient = WebClient.create(vertx, options);
        webClient
                .putAbs(pathAbs)
                .sendJsonObject(payload, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        logger.debug("Successfully reported EV on sensor "+ id+", status code " + response.statusCode() + "\n " +response.bodyAsString());
                        routingContext.response().end(Json.encodePrettily(new JsonObject().put("message", "success")));
                    } else {
                        logger.error("Something went wrong " + ar.cause().getMessage());
                        routingContext.fail(400);
                    }
                });
    }

    private void retrieveEndpoint(String service, int id, RoutingContext routingContext){
        discovery.getRecord( r -> r.getName().equals(service), ar -> {
            if(ar.succeeded()){
                if (ar.result() != null){
                    logger.debug("Success in locating resource");
                    ServiceReference reference = discovery.getReference(ar.result());
                    this.client = reference.getAs(HttpClient.class);
                    doDispatch(routingContext, id);
                    reference.release();
                } else {
                    logger.debug("failed: no matching service");
                }
            } else {
                logger.debug("lookup failed, going to default location");
            }
        });
    }

    private void mockDiscoveryEndpoints(){
        //TODO make discovery work
        //API gateway default port 8787
        final boolean ssl = false;

        final String tlcDefaultHost = "localhost";
        final int tlcDefaultPort = 8086;
        final String tlcServiceName = "traffic-light-service";
        final String tlcRoot = "";

        final String gwDefaultHost = "localhost";
        final int gwDefaultPort = 8787;
        final String gwServiceName = "api-gateway";
        final String gwRoot = "";

        final String evDefaultHost = "localhost";
        final int evDefaultPort = 8087;
        final String evServiceName = "ev-service";

        final String keycloakDefaultHost = "localhost";
        final int keycloakDefaultPort = 38080;

        Record record = HttpEndpoint.createRecord(tlcServiceName, ssl, tlcDefaultHost, tlcDefaultPort, tlcRoot, new JsonObject().put("api.name", "lights"));
        discovery.publish(record, ar -> {
            if (ar.succeeded()){
                Record publishedRecord = ar.result();
            } else {
                //couldnt create record
            }
        });

        Record record2 = HttpEndpoint.createRecord(gwServiceName, ssl, gwDefaultHost, gwDefaultPort, gwRoot, new JsonObject().put("api.name", "lights"));
        discovery.publish(record2, ar -> {
            if (ar.succeeded()){
                Record publishedRecord = ar.result();
            } else {
                //couldnt create record
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

    protected void enableLocalSession(Router router) {
        //router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(
                LocalSessionStore.create(vertx, "shopping.user.session")));
    }

    protected void badRequest(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    protected void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    protected void internalError(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    protected void badGateway(Throwable ex, RoutingContext context) {
        ex.printStackTrace();
        context.response()
                .setStatusCode(502)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "bad_gateway")
                        //.put("message", ex.getMessage())
                        .encodePrettily());
    }


    protected Future<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", config().getString("api.name", ""))
        );
        return publish(record);
    }

    protected Future<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("api-gateway", true, host, port, "/", null)
                .setType("api-gateway");
        return publish(record);
    }

    protected Future<Void> publishJDBCDataSource(String name, JsonObject location) {
        Record record = JDBCDataSource.createRecord(name, location, new JsonObject());
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
