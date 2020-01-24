package de.tub.trafficlight.controller;

import de.tub.microservice.common.RestAPIVerticle;
import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLPosition;
import de.tub.trafficlight.controller.entity.TLType;
import de.tub.trafficlight.controller.entity.TrafficLight;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
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
    //TODO color change should be mapped to position, id is unknown to sensor
    private static final String API_SINGLE_COLOR = "/lights/:tlId/colors";

    private TLControllerService service;

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
        //final String truststorepath = config().getString("truststore.path", "src/main/resources/server_truststore.jks");

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true).setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath));

        createHttpServer(router,host,port, options)
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
                .setHandler(promise);

    }

    private void apiPutSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            routingContext.fail(400);
            logger.debug("Id was no int");
            return;
        }
        if (service.getSingleTLState(id).isEmpty()){
            routingContext.fail(404);
            return;
        }
        TrafficLight oldTl = service.getSingleTLState(id).get();
        JsonObject json = routingContext.getBodyAsJson();
        TLColor color = getEnumFromString(TLColor.class, json.getString("color"));
        if (color != null){
            oldTl.setColor(color);
        } else {
            routingContext.fail(400);
            logger.debug("Color not matching possible options");
            return;
        }
        if (service.updateTL(id, oldTl)){
            //TODO change mode isAssigned
            routingContext.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(oldTl));
        } else {
            logger.error("Error: Request " + routingContext.request().absoluteURI() + " from User " + routingContext.user().toString());
            routingContext.fail(404);
        }

    }

    private void apiDeleteSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (NumberFormatException e) {
            routingContext.response().setStatusCode(404).end();
            return;
        }
        if(service.getSingleTLState(id).isPresent() && service.getSingleTLState(id).get().getGroup() >= 2){
            if (service.removeTL(id)) {
                routingContext.response().setStatusCode(204).end();
            } else {
                routingContext.fail(400);
            }
        } else {
            routingContext.fail(400);
            logger.debug("Intersection 1 Traffic Light cannot be deleted");
        }

    }

    private void apiPostSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        JsonObject json = routingContext.getBodyAsJson();
        TLPosition position = getEnumFromString(TLPosition.class, json.getString("position", "UNSPECIFIED"));
        TLType type = getEnumFromString(TLType.class, json.getString("type", "VEHICLE"));
        TLColor color = getEnumFromString(TLColor.class, json.getString("color", "GREEN"));
        int group = json.getInteger("group");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            routingContext.fail(400);
            logger.debug("Id was no int");
            return;
        }
        if (service.getSingleTLState(id).isPresent() || group == 1){
            routingContext.fail(400);
            logger.debug("Already exists or wrong group");
            return;
        }
        TrafficLight created = service.addTL(id, color, position, type, group);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(created));
    }

    private void apiGetSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            routingContext.fail(400);
            logger.debug("Id was no int");
            return;
        }
        if (service.getSingleTLState(id).isEmpty()){
            routingContext.fail(404, new Exception("Not Found"));
            return;
        }
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(service.getSingleTLState(id).get()));
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
        String tlId = routingContext.request().getParam("tlId");
        JsonObject json = routingContext.getBodyAsJson();
        //TLPosition position = getEnumFromString(TLPosition.class, json.getString("position", "UNSPECIFIED"));
        TLColor color = getEnumFromString(TLColor.class, json.getString("color", "GREEN"));
        int group = json.getInteger("group");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            routingContext.fail(400);
            logger.debug("Id was no int");
            return;
        }
        if (service.getSingleTLState(id).isEmpty() || service.getSingleTLState(id).get().getGroup() != group){
            routingContext.fail(404);
            return;
        }
        if (color == null){
            routingContext.fail(400);
            logger.debug("Color not matching possible options");
            return;
        }
        //now we have working ID, group and new color
        if(isEmergencyVehicleSensor()){
            if(isAuthorized()){
                if (service.changeToGreen(id)){
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(new JsonObject().put("message", "success")));
                } else {
                    logger.debug("Couldnt execute green Light change.");
                    routingContext.fail(400);
                }
            } else {
                logger.debug("Unauthorized Request");
                routingContext.fail(400);
            }
        } else {
            logger.debug("TLC-User Requested Color Assignment");
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(service.changeColor(id, color)));

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
                //TODO handle
            }
        }
        return null;
    }

    /*private void test(RoutingContext context){
        Optional<Integer> integer = getValueFromRequestParams("id", context.request().params(), Integer.class);
    }

    private static <T> Optional<T> getValueFromRequestParams(String key, MultiMap params, Class<T> c){
        if (params.contains(key)){
            String value = params.get(key);
            if (c.isEnum()){
                if (
                return Optional.ofNullable(getEnumFromString(c, value));
            } else if (c.)
        } else {
            return Optional.empty();
        }
    }*/
}
