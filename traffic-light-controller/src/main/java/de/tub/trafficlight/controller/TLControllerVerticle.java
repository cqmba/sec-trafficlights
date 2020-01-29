package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.exception.AuthenticationException;
import de.tub.trafficlight.controller.exception.BadRequestException;
import de.tub.trafficlight.controller.security.AuthorizationHandler;
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

import java.util.*;

public class TLControllerVerticle extends AbstractVerticle {

    private static final String SERVICE_NAME = "traffic-light-service";
    private static final String API_STATES = "/lights";
    private static final String API_SINGLE_STATE = "/lights/:tlId";
    private static final String API_SINGLE_COLOR = "/lights/:tlId/colors";
    private static final String API_GROUPS = "/groups/:grId";

    private TLControllerService service;
    private ServiceDiscovery discovery;
    private Set<Record> registeredRecords = new ConcurrentHashSet<>();

    private static final Logger logger = LogManager.getLogger(TLControllerVerticle.class);

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);
        discovery = ServiceDiscovery.create(vertx);

        this.service = TLControllerService.createService(vertx);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get(API_STATES).handler(this::apiGetAll);
        router.get(API_SINGLE_STATE).handler(this::apiGetSingle);
        router.post(API_SINGLE_STATE).handler(this::apiPostSingle);
        router.put(API_SINGLE_STATE).handler(this::apiChangeColor);
        router.delete(API_SINGLE_STATE).handler(this::apiDeleteSingle);
        router.put(API_SINGLE_COLOR).handler(this::apiChangeColor);
        router.get(API_GROUPS).handler(this::apiGetGroup);
        router.put(API_GROUPS).handler(this::apiPutGroupMode);

        //config
        String host = "localhost";
        int port = 8086;

        //default values, the real values need to be parsed through config values
        final String keystorepass = config().getString("keystore.password", "password");
        final String keystorepath = config().getString("keystore.path", "tlc_keystore.jks");

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

    private void apiGetGroup(RoutingContext routingContext){
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Arrays.asList("manager", "observer"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        try{
            int groupId = retrieveGroupId(routingContext);
            TLMode mode = service.getGroupMode(groupId);
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new JsonObject().put("mode", mode.toString())));
        } catch (BadRequestException ex){
            badRequest(routingContext, ex);
        }
    }

    private void apiPutGroupMode(RoutingContext routingContext){
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Arrays.asList("manager", "admin"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        String grId = routingContext.request().getParam("grId");
        int groupId;
        try {
            groupId = Integer.parseInt(grId);
        } catch (NumberFormatException e) {
            logger.debug("ID could not be parsed to int");
            badRequest(routingContext, new Exception("ID could not be parsed to int"));
            return;
        }
        JsonObject json = routingContext.getBodyAsJson();
        if (json.containsKey("mode") ){
            TLMode oldMode = service.getGroupMode(groupId);
            TLMode newMode = getEnumFromString(TLMode.class, json.getString("mode"));
            if (newMode != null && newMode != oldMode && newMode != TLMode.ASSIGNED){
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(service.switchGroupMode(groupId, newMode)));
                return;
            }
        }
        logger.debug("New Mode is invalid or not set");
        badRequest(routingContext, new Exception("New Mode is invalid or not set"));
    }

    private void apiDeleteSingle(RoutingContext routingContext) {
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Collections.singletonList("manager"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        try {
            int id = retrieveTLId(routingContext);
            if(service.getSingleTLState(id).isPresent() && service.getSingleTLState(id).get().getGroup() == 2){
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
        } catch (BadRequestException ex){
            badRequest(routingContext, ex);
        }

    }

    private void apiPostSingle(RoutingContext routingContext) {
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Collections.singletonList("manager"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        try {
            int group = routingContext.getBodyAsJson().getInteger("group");
            int id = retrieveTLId(routingContext);
            TLColor color = retrieveTLColor(routingContext);
            TLPosition position = retrieveTLPosition(routingContext);
            TLType type = retrieveTLType(routingContext);
            if (service.getSingleTLState(id).isPresent() || group == 1){
                logger.debug("Traffic Light with given ID already exits or group ID is bad");
                badRequest(routingContext, new BadRequestException("Traffic Light with given ID already exits or group ID is bad"));
                return;
            }
            TrafficLight created = service.addTL(id, color, position, type);
            logger.debug("New traffic light created " + created.getId());
            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(created));
        }catch (BadRequestException ex){
            badRequest(routingContext, ex);
        }
    }

    private void apiGetSingle(RoutingContext routingContext) {
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Arrays.asList("manager", "observer"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        try {
            int id = retrieveTLId(routingContext);
            if (service.getSingleTLState(id).isEmpty()){
                logger.debug("Traffic Light for given ID could not be found");
                notFound(routingContext);
                return;
            }
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(service.getSingleTLState(id).get()));
        } catch (BadRequestException ex){
            badRequest(routingContext, ex);
        }
    }

    private void apiGetAll(RoutingContext routingContext) {
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Arrays.asList("manager", "observer"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        List<TrafficLight> tlList = service.getTLList();
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Access-Control-Allow-Origin", "*")
                .end(Json.encodePrettily(tlList));
    }

    private void apiChangeColor(RoutingContext routingContext){
        try {
            AuthorizationHandler.authenticateAndLogUser(routingContext);
        } catch (AuthenticationException e){
            routingContext.fail(401, e);
        }
        final Set<String> acceptedRoles = new HashSet<>(Arrays.asList("manager", "ev"));
        if(!AuthorizationHandler.isAuthorized(routingContext, acceptedRoles)){
            logger.info("User is not authorized to access resource");
            routingContext.fail(403);
        }
        try {
            int group = routingContext.getBodyAsJson().getInteger("group");
            int id = retrieveTLId(routingContext);
            TLColor color = retrieveTLColor(routingContext);
            if (service.getSingleTLState(id).isEmpty() || service.getSingleTLState(id).get().getGroup() != group){
                logger.debug("Traffic Light ID doesnt exist or Group ID is wrong");
                badRequest(routingContext, new BadRequestException("Traffic Light ID doesnt exist or Group ID is wrong"));
                return;
            }
            Set<String> roles = AuthorizationHandler.getRolesFromToken(routingContext.request().params().get("token"));
            if (roles.contains("manager")){
                logger.debug("TLC-Manager Requested Color Assignment");
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(service.changeToGenericColorOnManagerRequest(id, color)));
            } else if(roles.contains("ev") && color.equals(TLColor.GREEN)){
                if (service.changeToGreenOnEVRequest(id)){
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(new JsonObject().put("message", "success")));
                } else {
                    logger.debug("Couldnt execute green Light change.");
                    internalError(routingContext, new RuntimeException("Unable to switch to green light"));
                }
            } else {
                routingContext.fail(403);
            }
         } catch (BadRequestException ex){
             badRequest(routingContext, ex);
         }
    }

    private int retrieveGroupId(RoutingContext routingContext) throws BadRequestException{
        String grId = routingContext.request().getParam("grId");
        try {
            int groupId = Integer.parseInt(grId);
            //only groups 1 and 2 accepted
            if (groupId == 1 || groupId == 2){
                return groupId;
            }
        } catch (NumberFormatException e) {
            logger.debug("GroupId is invalid");
        }
        throw new BadRequestException("GroupId is invalid");
    }

    private int retrieveTLId(RoutingContext routingContext) throws BadRequestException {
        String tlId = routingContext.request().getParam("tlId");
        try {
            return Integer.parseInt(tlId);
        } catch (NumberFormatException ex){
            logger.debug("ID could not be parsed to int");
            throw new BadRequestException("ID could not be parsed to int");
        }
    }

    private TLColor retrieveTLColor(RoutingContext routingContext) throws BadRequestException{
        TLColor color = getEnumFromString(TLColor.class, routingContext.getBodyAsJson().getString("color", "GREEN"));
        if (color == null){
            logger.debug("Color not matching possible options");
            throw new BadRequestException("Color not matching possible options");
        } else return color;
    }

    private TLPosition retrieveTLPosition(RoutingContext routingContext) throws BadRequestException{
        TLPosition position = getEnumFromString(TLPosition.class, routingContext.getBodyAsJson().getString("position", TLPosition.UNSPECIFIED.toString()));
        if (position == null){
            logger.debug("Position not matching possible options");
            throw new BadRequestException("Position not matching possible options");
        } else return position;
    }

    private TLType retrieveTLType(RoutingContext routingContext) throws BadRequestException{
        TLType type = getEnumFromString(TLType.class, routingContext.getBodyAsJson().getString("type", TLType.VEHICLE.toString()));
        if (type == null){
            logger.debug("Type not matching possible options");
            throw new BadRequestException("Type not matching possible options");
        } else return type;
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

    private Future<HttpServer> createHttpServer(Router router, String host, int port, HttpServerOptions options) {
        Promise<HttpServer> httpServerPromise = Promise.promise();
        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(port, host, httpServerPromise);
        logger.info("Http Server started at " + host +port);
        return httpServerPromise.future().map(r -> null);
    }

    private void badRequest(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    private void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    private void internalError(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    private Future<Void> publishHttpEndpoint(String name, String host, int port) {
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
