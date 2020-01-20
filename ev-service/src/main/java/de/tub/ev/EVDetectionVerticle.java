package de.tub.ev;

import de.tub.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerOptions;
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

import java.util.List;

public class EVDetectionVerticle extends RestAPIVerticle {
    private static final String SERVICE_NAME = "ev-service";
    private static final String BASE_TLC_API = "/api/lights/";
    private static final String API_MOCK_SENSOR_DETECT = "/sensors/:tlId";
    private static final String TARGET_SERVICE = "api-gateway";

    private static final Logger logger = LogManager.getLogger(EVDetectionVerticle.class);

    private EVDetectionService service;
    private HttpClient client;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        //TODO might still need to start Abstract Verticle
        super.start(promise);
        mockDiscoveryEndpoints();
        //TODO fix HttpClient
        retrieveEndpoint(TARGET_SERVICE);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_MOCK_SENSOR_DETECT).handler(this::apiRequestOnEVDetection);
        //config
        String host = "localhost";
        int port = 8087;

        createHttpServer(router,host,port, new HttpServerOptions())
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
                .setHandler(promise.future().completer());
    }

    private void apiRequestOnEVDetection(RoutingContext routingContext) {
        int id = 0;
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
        doDispatchEVRequest(routingContext, id);
    }

    private void retrieveEndpoint(String service){
        discovery.getRecord( r -> r.getName().equals(service), ar -> {
            if(ar.succeeded()){
                if (ar.result() != null){
                    logger.debug("Success in locating resource");
                    ServiceReference reference = discovery.getReference(ar.result());
                    this.client = reference.getAs(HttpClient.class);
                    reference.release();
                } else {
                    logger.debug("failed: no matching service");
                }
            } else {
                logger.debug("lookup failed, going to default location");
            }
        });
    }

    private void doDispatchEVRequest(RoutingContext routingContext, int id){
        //TODO change to BASE_TLC_API when using gateway
        final String path = BASE_TLC_API + id;
        //final String path = "/lights/"+id;
        WebClient webClient = WebClient.wrap(client);
        webClient
                .get(path)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        logger.debug("Success, status code " + response.statusCode() + "\n " +response.bodyAsString());
                        routingContext.response().end(response.bodyAsString());
                    } else {
                        logger.debug("Something went wrong " + ar.cause().getMessage());
                        routingContext.fail(400);
                    }
                });
    }

    private Future<List<Record>> getAllEndpoints() {
        Future<List<Record>> future = Future.future();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                future.completer());
        return future;
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

        Record record2 = HttpEndpoint.createRecord(gwServiceName, ssl, gwDefaultHost, gwDefaultPort, gwRoot, new JsonObject().put("api.name", "gateway"));
        discovery.publish(record2, ar -> {
            if (ar.succeeded()){
                Record publishedRecord = ar.result();
            } else {
                //couldnt create record
            }
        });
    }
}
