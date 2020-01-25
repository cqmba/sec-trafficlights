package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLPosition;
import de.tub.trafficlight.controller.entity.TLType;
import de.tub.trafficlight.controller.entity.TrafficLight;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.Json;
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

public class TLControllerVerticle extends AbstractVerticle {

    private static final String SERVICE_NAME = "traffic-light-service";
    private static final String API_STATES = "/lights";
    private static final String API_SINGLE_STATE = "/lights/:tlId";
    private static final String API_SINGLE_COLOR = "/lights/:tlId/colors";

    private TLControllerService service;
    private ServiceDiscovery discovery;
    protected Set<Record> registeredRecords = new ConcurrentHashSet<>();

    private static final Logger logger = LogManager.getLogger(TLControllerVerticle.class);

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        //TODO retrieve config
        discovery = ServiceDiscovery.create(vertx);

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
                .setSsl(true)
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
                .setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath));

        createHttpServer(router,host,port, options)
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port));
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
            logger.debug("Color not matching possible options");
            badRequest(routingContext, new Exception("Color not matching possible options"));
            return;
        }
        doChangeColor(routingContext, id, color);
    }

    private void apiDeleteSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (NumberFormatException e) {
            logger.debug("ID could not be parsed to int");
            badRequest(routingContext, new Exception("ID could not be parsed to int"));
            return;
        }
        if(service.getSingleTLState(id).isPresent() && service.getSingleTLState(id).get().getGroup() >= 2){
            if (service.removeTL(id)) {
                logger.info("Traffic Light deleted " + id);
                routingContext.response().setStatusCode(204).end();
            } else {
                logger.error("Unable to delete Traffic Light");
                internalError(routingContext, new Exception("Unable to delete Traffic Light"));
            }
        } else {
            logger.debug("Intersection 1 Traffic Light cannot be deleted");
            badRequest(routingContext, new Exception("Intersection 1 Traffic Light cannot be deleted"));
        }

    }

    private void apiPostSingle(RoutingContext routingContext) {
        String tlId = routingContext.request().getParam("tlId");
        JsonObject json = routingContext.getBodyAsJson();
        TLPosition position = getEnumFromString(TLPosition.class, json.getString("position", "UNSPECIFIED"));
        TLType type = getEnumFromString(TLType.class, json.getString("type", "VEHICLE"));
        TLColor color = getEnumFromString(TLColor.class, json.getString("color", "GREEN"));
        if (color == null || type == null || position == null){
            logger.debug("Type, Position or Color not passed correctly");
            badRequest(routingContext, new Exception("Type, Position or Color not passed correctly"));
            return;
        }
        int group = json.getInteger("group");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            logger.debug("ID could not be parsed to int");
            badRequest(routingContext, new Exception("ID could not be parsed to int"));
            return;
        }
        if (service.getSingleTLState(id).isPresent() || group == 1){
            logger.debug("Traffic Light with given ID already exits or group ID is bad");
            badRequest(routingContext, new Exception("Traffic Light with given ID already exits or group ID is bad"));
            return;
        }
        TrafficLight created = service.addTL(id, color, position, type, group);
        logger.debug("New traffic light created " + created.getId());
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
            logger.debug("ID could not be parsed to int");
            badRequest(routingContext, new Exception("ID could not be parsed to int"));
            return;
        }
        if (service.getSingleTLState(id).isEmpty()){
            logger.debug("Traffic Light for given ID could not be found");
            notFound(routingContext);
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
            internalError(routingContext, new Exception("Unable to Retrieve the traffic lights"));
        }
    }

    private void apiChangeColor(RoutingContext routingContext){
        String tlId = routingContext.request().getParam("tlId");
        JsonObject json = routingContext.getBodyAsJson();
        TLColor color = getEnumFromString(TLColor.class, json.getString("color", "GREEN"));
        if (color == null){
            logger.debug("Color not matching possible options");
            badRequest(routingContext, new Exception("Color not matching possible options"));
            return;
        }
        int group = json.getInteger("group");
        int id;
        try {
            id = Integer.parseInt(tlId);
        } catch (Exception ex){
            logger.debug("ID could not be parsed to int");
            badRequest(routingContext, new Exception("ID could not be parsed to int"));
            return;
        }
        if (service.getSingleTLState(id).isEmpty() || service.getSingleTLState(id).get().getGroup() != group){
            logger.debug("Traffic Light ID doesnt exist or Group ID is wrong");
            badRequest(routingContext, new Exception("Traffic Light ID doesnt exist or Group ID is wrong"));
            return;
        }
        doChangeColor(routingContext, id, color);
    }

    private void doChangeColor(RoutingContext routingContext, int id, TLColor color){
        //now we have working ID, group and new color
        if(isEmergencyVehicleSensor() && color.equals(TLColor.GREEN)){
            if(isAuthorized()){
                if (service.changeToGreen(id)){
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(new JsonObject().put("message", "success")));
                } else {
                    logger.debug("Couldnt execute green Light change.");
                    internalError(routingContext, new Exception("Unable to switch to green light"));
                }
            } else {
                logger.debug("Unauthorized Request");
                badRequest(routingContext, new Exception("Unauthorized Request"));
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
        return false;
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
                logger.info("Unable to convert String to Enum");
            }
        }
        return null;
    }

    protected Future<HttpServer> createHttpServer(Router router, String host, int port, HttpServerOptions options) {
        Promise<HttpServer> httpServerPromise = Promise.promise();
        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(port, host, httpServerPromise);
        logger.info("Http Server started at " + host +port);
        return httpServerPromise.future().map(r -> null);
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
    public void stop(Promise<Void> promise) {
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
