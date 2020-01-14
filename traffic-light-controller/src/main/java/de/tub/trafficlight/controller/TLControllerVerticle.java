package de.tub.trafficlight.controller;

import de.tub.microservice.common.RestAPIVerticle;
import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TrafficLight;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TLControllerVerticle extends RestAPIVerticle {
    //TODO bugs: bei REST delete auf Id achten

    private static final String SERVICE_NAME = "traffic-light-service";
    private static final String API_STATES = "/lights";
    private static final String API_SINGLE_STATE = "/lights/:tlId";
    private static final String API_SINGLE_COLOR = "/lights/:tlId/colors";

    private TLControllerService service;

    //TODO log every action and failure and check for secure information leakage
    private static final Logger logger = LogManager.getLogger(TLControllerVerticle.class);

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        //TODO retrieve config

        this.service = TLControllerService.createService(vertx);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_STATES).handler(this::apiGetAll);
        router.get(API_SINGLE_STATE).handler(this::apiGetSingle);
        router.post(API_SINGLE_STATE).handler(this::apiPostSingle);
        router.put(API_SINGLE_STATE).handler(this::apiPutSingle);
        router.delete(API_SINGLE_STATE).handler(this::apiDeleteSingle);
        router.put(API_SINGLE_COLOR).handler(this::apiChangeColor);

        //config
        String host = "localhost";
        int port = 8086;

        final String keystorepass = config().getString("keystore.password", "UedJ6AtmjcwF7qNQ");
        final String keystorepath = config().getString("keystore.path", "src/main/resources/server_keystore.jks");
        final String truststorepath = config().getString("truststore.path", "src/main/resources/server_truststore.jks");

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true).setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath))
                .setTrustStoreOptions(new JksOptions().setPassword(keystorepass).setPath(truststorepath));

        createHttpServer(router,host,port, options)
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
                .setHandler(promise);

    }

    private void apiPutSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        TrafficLight tlLight = routingContext.getBodyAsJson().mapTo(TrafficLight.class);
        int id;
        try {
            id = Integer.parseInt(tlId);
            if (service.updateTL(id, tlLight)){
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(tlLight));
            } else {
                logger.error("Error: Request " + routingContext.request().absoluteURI() + " from User " + routingContext.user().toString());
                routingContext.fail(404);
            }
        } catch (NumberFormatException ex){
            routingContext.fail(400);
        }
    }

    private void apiDeleteSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        int id;
        try {
            id = Integer.parseInt(tlId);
            if (service.removeTL(id)) {
                routingContext.response().setStatusCode(204).end();
            } else {
                routingContext.fail(400);
            }
        } catch (NumberFormatException e) {
            routingContext.response().setStatusCode(404).end();
        }
    }

    private void apiPostSingle(RoutingContext routingContext) {
        TrafficLight tlLight = routingContext.getBodyAsJson().mapTo(TrafficLight.class);
        service.addTL(tlLight.getPosition());
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(tlLight));
    }

    private void apiGetSingle(RoutingContext routingContext) {
        try {
            String tlId = routingContext.request().getParam("tlId");
            int id;
            id = Integer.parseInt(tlId);
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(service.getSingleTLState(id)));
        } catch (Exception ex){
            routingContext.fail(400);
        }
    }

    private void apiGetAll(RoutingContext routingContext) {
        try {
            List<TrafficLight> tlList = service.getTLList();
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(tlList));
        } catch (Exception ex){
            routingContext.fail(400);
        }
    }

    private void apiChangeColor(RoutingContext routingContext){
        //TODO test query param color
        try {
            String tlId = routingContext.request().getParam("tlId");
            int id;
            id = Integer.parseInt(tlId);
            String color = "";
            TLColor tlcolor = null;
            if(routingContext.request().params().contains("color")){
                color = routingContext.request().params().get("color");
                if(getEnumFromString(TLColor.class, color) != null){
                    tlcolor = getEnumFromString(TLColor.class, color);
                }
            }
            if(isEmergencyVehicleSensor()){
                if(isAuthorized()){
                    if (service.changeToGreen(id)){
                        routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(service.getSingleTLState(id)));
                    } else {
                        throw new Exception("Couldnt execute green Light change.");
                    }
                } else {
                    throw new Exception("Unauthorized Request");
                }
            } else {
                if (tlcolor != null) {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(service.changeColor(id, tlcolor)));
                }
            }
        } catch (Exception ex){
            routingContext.fail(400);
        }
    }

    //TODO implement
    private boolean isEmergencyVehicleSensor() {
        return true;
    }

    //TODO implement
    private boolean isAuthorized(){
        return true;
    }

    private static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if( c != null && string != null ) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            } catch(IllegalArgumentException ex) {
            }
        }
        return null;
    }
}
