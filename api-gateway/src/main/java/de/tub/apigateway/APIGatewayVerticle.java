package de.tub.apigateway;

import de.tub.apigateway.security.AuthenticationException;
import de.tub.apigateway.security.AuthorizationHandler;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main Verticle Class for the API Gateway, implementing Service Discovery, Circuit Breaker,
 * Dispatching Requests, Health Check and Authentication/Authorization
 */
public class APIGatewayVerticle extends AbstractVerticle {

    private static final int DEFAULT_PORT_GW = 8787;
    private static final int DEFAULT_PORT_TLC = 8086;
    private static final int DEFAULT_PORT_EV = 8087;
    private static final int DEFAULT_PORT_DB = 3306;
    private static final int DEFAULT_PORT_KC = 8080;

    private final String tlcService = "traffic-light-service";
    private final String evService = "ev-service";
    private final String dbService = "database";
    private final String keycloakService = "keycloak";

    private ServiceDiscovery discovery;
    private CircuitBreaker circuitBreaker;
    private Set<Record> registeredRecords = new ConcurrentHashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(APIGatewayVerticle.class);

    /**This is called when the Verticle Instance is deployed.
     * @param promise A promise which is called when the startup is complete.
     * @throws Exception
     */
    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start();
        discovery = ServiceDiscovery.create(vertx);
        initCircuitBreaker();
        mockDiscoveryEndpoints();

        String host = config().getString("api.gateway.http.address", "localhost");
        int port = config().getInteger("api.gateway.http.port", DEFAULT_PORT_GW);

        Router router = Router.router(vertx);
        HealthCheckHandler healthHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

        router.route().handler(BodyHandler.create());
        router.get("/api/v").handler(this::apiVersion);

        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>This is the gateway, please use the API</h1>");
        });
        JsonObject keycloakJson = config().getJsonObject("keycloak.json");

        OAuth2Auth oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, keycloakJson);
        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(oauth2);

        authHandler.setupCallback(router.get("/callback"));
        
        router.route("/*").handler(this::addHeaders);
        router.options("/*").handler(this::sendDefaultOptions);
        router.route("/api/*").handler(authHandler);
        router.route("/api/*").handler(this::dispatchRequests);

        //these are default values, actual secrets are loaded from the config
        final String keystorepass = config().getString("keystore.password", "password");
        final String keystorepath = config().getString("keystore.path", "gateway_keystore.jks");

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
                .setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath));

        //register and protect health api
        registerHealthEndpoints(healthHandler);
        router.route("/health").handler(authHandler);
        router.route("/health").handler(this::handleIfAdmin);
        router.get("/health").handler(healthHandler);

        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(port, host, ar -> {
                    if (ar.succeeded()) {
                        publishApiGateway(host, port);
                        promise.complete();
                        logger.debug("API Gateway is running on port " + port);
                    } else {
                        promise.fail(ar.cause());
                    }
                });
    }

    /*private void initConfig(){
        ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "config.json"));
        ConfigRetrieverOptions configOptions = new ConfigRetrieverOptions().addStore(fileStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, configOptions);
        retriever.getConfig(ar -> {
            if (ar.succeeded() && !config().containsKey("keystore.password")){
                logger.debug("Config successfully loaded");
                JsonObject result = ar.result();
                vertx.close();
                // Create a new Vert.x instance using the retrieve configuration
                VertxOptions options = new VertxOptions(result);
                Vertx newVertx = Vertx.vertx(options);
                newVertx.deployVerticle(APIGatewayVerticle.class.getName());
            }else {
                logger.error("Could not load config.");
            }
        });
    }*/
    
    private void addHeaders(RoutingContext rc) {
 	   rc.response().headers().add("Access-Control-Allow-Origin", "https://localhost");
 	   rc.next();
    }
    
    private void sendDefaultOptions(RoutingContext rc) {
 	   rc.response().headers().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT")
 	   .add("Access-Control-Allow-Headers", "authorization, content-type");
 	   rc.response().end();
    }
    private void initCircuitBreaker(){
        JsonObject cbOptions = config().getJsonObject("circuit-breaker") != null ?
                config().getJsonObject("circuit-breaker") : new JsonObject();
        circuitBreaker = CircuitBreaker.create(cbOptions.getString("name", "circuit-breaker"), vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(cbOptions.getInteger("max-failures", 5))
                        .setTimeout(cbOptions.getLong("timeout", 10000L))
                        .setFallbackOnFailure(true)
                        .setResetTimeout(cbOptions.getLong("reset-timeout", 30000L))
        );
    }

    private void handleIfAdmin(RoutingContext rc){
        Set<String> acceptedRoles = new HashSet<>(Collections.singletonList("admin"));
        logger.info("Source IP " + rc.request().remoteAddress() +" requests resource " +rc.request().absoluteURI());
        if (AuthorizationHandler.isAuthorized(rc.user().principal(), acceptedRoles)){
            rc.next();
        } else {
            rc.fail(403);
        }
    }

    private void registerHealthEndpoints(HealthCheckHandler healthHandler){
        for (String service : Stream.of(tlcService, evService, dbService, keycloakService).collect(Collectors.toList())){
            registerSingleHealthEndpoint(healthHandler, service);
        }
    }

    private void registerSingleHealthEndpoint(HealthCheckHandler healthHandler, String service){
        healthHandler.register(service,
                future -> HttpEndpoint.getClient(discovery,
                        (rec) -> service.equals(rec.getName()),
                        client -> {
                            if (client.failed()) {
                                future.fail(client.cause());
                            } else {
                                client.result().close();
                                future.complete(Status.OK());
                            }
                        }));
    }

    private void dispatchRequests(RoutingContext context) {
        try {
            logger.info("Source IP " + context.request().remoteAddress() +" requests resource " +context.request().absoluteURI());
            AuthorizationHandler.authenticateAndLogRequest(context.user().principal());
        } catch (AuthenticationException e) {
            context.fail(401, e);
        }
        int initialOffset = 5; // length of `/api/`
        circuitBreaker.execute(promise -> getAllEndpoints().setHandler(ar -> {
            if (ar.succeeded()) {
                List<Record> recordList = ar.result();
                // get relative path and retrieve prefix to dispatch client
                String path = context.request().uri();

                if (path.length() <= initialOffset) {
                    notFound(context);
                    promise.complete();
                    return;
                }
                String prefix = (path.substring(initialOffset)
                        .split("/"))[0];
                // generate new relative path
                String newPath = path.substring(initialOffset + prefix.length());
                // a relevant http client endpoint
                Optional<Record> client = recordList.stream()
                        .filter(record -> record.getMetadata().getString("api.name") != null)
                        .filter(record -> record.getMetadata().getString("api.name").equals(prefix) ||
                                record.getMetadata().getString("api.alt").equals(prefix))
                        .findAny();

                if (client.isPresent() && client.get().getLocation().containsKey("endpoint")) {
                    String endpoint = client.get().getLocation().getString("endpoint");
                    doDispatch(context, prefix+newPath, endpoint, promise);
                } else {
                    notFound(context);
                    promise.complete();
                    logger.debug("Could not retrieve API Endpoint");
                }
            } else {
                promise.fail(ar.cause());
            }
        })).setHandler(ar -> {
            if (ar.failed()) {
                badGateway(ar.cause(), context);
                logger.info("Circuit Breaker timeout on Request " + context.request().absoluteURI());
            }
        });
    }

    private void doDispatch(RoutingContext routingContext, String path, String endpoint, Promise<Object> promise) {
        String absURI = endpoint+path;
        logger.info("Dispatching request to target service " + absURI);

        final String truststorepath = config().getString("truststore.path", "gateway_truststore.jks");
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

        WebClient webClient = WebClient.create(vertx, options);
        HttpRequest<Buffer> request = webClient.requestAbs(routingContext.request().method(), absURI);
        request.addQueryParam("token", routingContext.user().principal().getString("access_token"));
        if (routingContext.request().headers().size() >= 1){
            request.putHeaders(routingContext.request().headers());
        }
        if (routingContext.user() != null) {
            request.putHeader("user-principal", routingContext.user().principal().encode());
        }
        if (routingContext.getBody() == null){
            request.send(ar -> handleAsyncResult(routingContext, promise, ar));
        } else {
            request.sendBuffer(routingContext.getBody(), ar -> handleAsyncResult(routingContext, promise, ar));
        }
    }

    private void handleAsyncResult(RoutingContext context, Promise<Object> promise, AsyncResult<HttpResponse<Buffer>> ar) {
        if (ar.succeeded()) {
            HttpResponse<Buffer> result = ar.result();
            HttpServerResponse toRsp = context.response()
                    .setStatusCode(result.statusCode());
            result.headers().forEach(header -> toRsp.putHeader(header.getKey(), header.getValue()));
            toRsp.end(result.body());
            promise.tryComplete();
            logger.info("Request successfully handled");
        } else {
            promise.tryFail("No response; timeout");
            logger.info("Timeout on request");
        }
    }

    private void apiVersion(RoutingContext context) {
        context.response()
                .end(new JsonObject().put("version", "v1").encodePrettily());
    }

    private Future<List<Record>> getAllEndpoints() {
        Promise<List<Record>> promise = Promise.promise();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                promise);
        return promise.future();
    }

    private void mockDiscoveryEndpoints(){
        final boolean ssl = true;
        final String DEFAULT_HOSTNAME = "localhost";
        final String DEFAULT_WEBROOT = "";

        Record record1 = HttpEndpoint.createRecord(tlcService, ssl, DEFAULT_HOSTNAME, DEFAULT_PORT_TLC, DEFAULT_WEBROOT, new JsonObject().put("api.name", "lights").put("api.alt", "groups"));
        Record record3 = HttpEndpoint.createRecord(evService, ssl, DEFAULT_HOSTNAME, DEFAULT_PORT_EV, DEFAULT_WEBROOT, new JsonObject());
        Record record4 = HttpEndpoint.createRecord(dbService, ssl, DEFAULT_HOSTNAME, DEFAULT_PORT_DB, DEFAULT_WEBROOT, new JsonObject());
        Record record5 = HttpEndpoint.createRecord(keycloakService, ssl, DEFAULT_HOSTNAME, DEFAULT_PORT_KC, DEFAULT_WEBROOT, new JsonObject());
        //publish all records
        for (Record record: Stream.of(record1, record3, record4, record5).collect(Collectors.toList())){
            publish(record);
        }
    }
    private void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    private void badGateway(Throwable ex, RoutingContext context) {
        ex.printStackTrace();
        context.response()
                .setStatusCode(502)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "bad_gateway")
                        //.put("message", ex.getMessage())
                        .encodePrettily());
    }

    private Future<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("api-gateway", true, host, port, "/", null)
                .setType("api-gateway");
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

    /**Stops the verticle when the instance is undeployed and does cleanup.
     * @param promise A promise that is called when the cleanup is complete.
     * @throws Exception
     */
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
