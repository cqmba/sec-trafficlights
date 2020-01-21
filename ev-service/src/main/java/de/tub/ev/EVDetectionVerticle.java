package de.tub.ev;

import de.tub.common.RestAPIVerticle;
import de.tub.ev.dispatch.EVDispatchService;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EVDetectionVerticle extends RestAPIVerticle {

    private static final String BASE_TLC_API = "/api/lights/";
    private static final String SERVICE_NAME = "ev-service";
    private static final String API_MOCK_SENSOR_DETECT = "/sensors/:tlId";
    private static final String TARGET_SERVICE = "api-gateway";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8087;

    private static final Logger logger = LogManager.getLogger(EVDetectionVerticle.class);

    private EVDispatchService service;
    private HttpClient client;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        mockDiscoveryEndpoints();
        //service = EVDispatchService.getInstance(client, vertx);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_MOCK_SENSOR_DETECT).handler(this::apiRequestOnEVDetection);

        createHttpServer(router,DEFAULT_HOST,DEFAULT_PORT, new HttpServerOptions())
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, DEFAULT_HOST, DEFAULT_PORT))
                .setHandler(promise.future().completer());
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
        //final String path = BASE_TLC_API + id +"/colors";
        final String pathAbs = "http://localhost:8787/api/lights/" + id + "/colors";
        JsonObject payload = new JsonObject().put("color", "GREEN").put("group", 1);
        WebClient webClient = WebClient.create(vertx);
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
}
