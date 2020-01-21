package de.tub.apigateway;

import de.tub.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;
import java.util.Optional;

public class APIGatewayVerticle extends RestAPIVerticle {

    private static final int DEFAULT_PORT = 8787;

    private static final Logger logger = LoggerFactory.getLogger(APIGatewayVerticle.class);

    //private OAuth2Auth oauth2;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start();
        super.start(promise);
        //Kubernetes Service Discovery might look like something like this
        //ServiceDiscovery.create(vertx).registerServiceImporter(new KubernetesServiceImporter(), new JsonObject());
        //TODO remove this when Discovery is working
        mockDiscoveryEndpoints();

        // get HTTP host and port from configuration, or use default value
        String host = config().getString("api.gateway.http.address", "localhost");
        int port = config().getInteger("api.gateway.http.port", DEFAULT_PORT);

        Router router = Router.router(vertx);
        // cookie and session handler
        enableLocalSession(router);

        // body handler
        router.route().handler(BodyHandler.create());

        // version handler
        router.get("/api/v").handler(this::apiVersion);

        // create OAuth 2 instance for Keycloak
        //oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config());
        //OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(oauth2);
        //authHandler.setupCallback(router.get("/callback"));

        //router.route().handler(UserSessionHandler.create(oauth2));

        String hostURI = buildHostURI();

        // set auth callback handler
        //router.route("/callback").handler(context -> authCallback(oauth2, hostURI, context));

        //router.route("/protected/*").handler(authHandler);
        //router.get("/uaa").handler(this::authUaaHandler);
        //router.get("/login").handler(this::loginEntryHandler);
        //router.post("/logout").handler(this::logoutHandler);

        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello vertx</h1>");
        });

        // Serve static resources from the /assets directory
        router.route("/assets/*").handler(StaticHandler.create("assets"));
        // api dispatcher
        //router.route("/api/*").handler(authHandler);
        router.route("/api/*").handler(this::dispatchRequests);

        final String keystorepass = config().getString("keystore.password", "4mB8nqJd5YEHFkw6");
        final String keystorepath = config().getString("keystore.path", "src/main/resources/server_keystore.jks");
        final String truststorepath = config().getString("truststore.path", "src/main/resources/server_truststore.jks");

        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true).setKeyStoreOptions(new JksOptions().setPassword(keystorepass).setPath(keystorepath))
                .setTrustStoreOptions(new JksOptions().setPassword(keystorepass).setPath(truststorepath));

        vertx.createHttpServer(new HttpServerOptions())
                .requestHandler(router)
                .listen(port, host, ar -> {
                    if (ar.succeeded()) {
                        publishApiGateway(host, port);
                        promise.complete();
                        logger.info("API Gateway is running on port " + port);
                        // publish log
                        publishGatewayLog("api_gateway_init_success:" + port);
                    } else {
                        promise.fail(ar.cause());
                    }
                });
    }

    private void dispatchRequests(RoutingContext context) {
        int initialOffset = 5; // length of `/api/`
        // run with circuit breaker in order to deal with failure
        circuitBreaker.execute(future -> {
            getAllEndpoints().setHandler(ar -> {
                if (ar.succeeded()) {
                    List<Record> recordList = ar.result();
                    // get relative path and retrieve prefix to dispatch client
                    String path = context.request().uri();

                    if (path.length() <= initialOffset) {
                        notFound(context);
                        future.complete();
                        return;
                    }
                    String prefix = (path.substring(initialOffset)
                            .split("/"))[0];
                    // generate new relative path
                    String newPath = path.substring(initialOffset + prefix.length());
                    // get one relevant HTTP client, may not exist
                    Optional<Record> client = recordList.stream()
                            .filter(record -> record.getMetadata().getString("api.name") != null)
                            .filter(record -> record.getMetadata().getString("api.name").equals(prefix))
                            .findAny(); // simple load balance

                    if (client.isPresent()) {
                        doDispatch(context, "/"+prefix+newPath, discovery.getReference(client.get()).get(), future);
                    } else {
                        notFound(context);
                        future.complete();
                    }
                } else {
                    future.fail(ar.cause());
                }
            });
        }).setHandler(ar -> {
            if (ar.failed()) {
                badGateway(ar.cause(), context);
            }
        });
    }

    /**
     * Dispatch the request to the downstream REST layers.
     *  @param context routing context instance
     * @param path    relative path
     * @param client  relevant HTTP client
     * @param cbPromise
     */
    private void doDispatch(RoutingContext context, String path, HttpClient client, Promise<Object> cbPromise) {
        HttpClientRequest toReq = client
                .request(context.request().method(), path, response -> {
                    response.bodyHandler(body -> {
                        if (response.statusCode() >= 500) { // api endpoint server error, circuit breaker should fail
                            cbPromise.fail(response.statusCode() + ": " + body.toString());
                        } else {
                            HttpServerResponse toRsp = context.response()
                                    .setStatusCode(response.statusCode());
                            response.headers().forEach(header -> {
                                toRsp.putHeader(header.getKey(), header.getValue());
                            });
                            // send response
                            toRsp.end(body);
                            cbPromise.complete();
                        }
                        ServiceDiscovery.releaseServiceObject(discovery, client);
                    });
                });
        // set headers
        context.request().headers().forEach(header -> {
            toReq.putHeader(header.getKey(), header.getValue());
        });
        if (context.user() != null) {
            toReq.putHeader("user-principal", context.user().principal().encode());
        }
        // send request
        if (context.getBody() == null) {
            toReq.end();
        } else {
            toReq.end(context.getBody());
        }
    }

    private void apiVersion(RoutingContext context) {
        context.response()
                .end(new JsonObject().put("version", "v1").encodePrettily());
    }

    /**
     * Get all REST endpoints from the service discovery infrastructure.
     *
     * @return async result
     */
    private Future<List<Record>> getAllEndpoints() {
        Future<List<Record>> future = Future.future();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                future.completer());
        return future;
    }

    // log methods

    private void publishGatewayLog(String info) {
        JsonObject message = new JsonObject()
                .put("info", info)
                .put("time", System.currentTimeMillis());
        publishLogEvent("gateway", message);
    }

    private void publishGatewayLog(JsonObject msg) {
        JsonObject message = msg.copy()
                .put("time", System.currentTimeMillis());
        publishLogEvent("gateway", message);
    }

    // auth

    private void authCallback(OAuth2Auth oauth2, String hostURL, RoutingContext context) {
        final String code = context.request().getParam("code");
        // code is a require value
        if (code == null) {
            context.fail(400);
            return;
        }
        final String redirectTo = context.request().getParam("redirect_uri");
        final String redirectURI = hostURL + context.currentRoute().getPath() + "?redirect_uri=" + redirectTo;
        oauth2.getToken(new JsonObject().put("code", code).put("redirect_uri", redirectURI), ar -> {
            if (ar.failed()) {
                logger.warn("Auth fail");
                context.fail(ar.cause());
            } else {
                logger.info("Auth success");
                context.setUser(ar.result());
                context.response()
                        .putHeader("Location", redirectTo)
                        .setStatusCode(302)
                        .end();
            }
        });
    }

    private void authUaaHandler(RoutingContext context) {
        if (context.user() != null) {
            JsonObject principal = context.user().principal();
            /*
            String username = null;  // TODO: Only for demo. Complete this in next version.
            // String username = KeycloakHelper.preferredUsername(principal);
            if (username == null) {
                context.response()
                        .putHeader("content-type", "application/json")
                        .end(new Account().setId("TEST666").setUsername("Eric").toString()); // TODO: no username should be an error
            } else {
                Future<AccountService> future = Future.future();
                EventBusService.getProxy(discovery, AccountService.class, future.completer());
                future.compose(accountService -> {
                    Future<Account> accountFuture = Future.future();
                    accountService.retrieveByUsername(username, accountFuture.completer());
                    return accountFuture.map(a -> {
                        ServiceDiscovery.releaseServiceObject(discovery, accountService);
                        return a;
                    });
                })
                        .setHandler(resultHandlerNonEmpty(context)); // if user does not exist, should return 404
            }

             */
        } else {
            context.fail(401);
        }
    }

    /*private void loginEntryHandler(RoutingContext context) {
        context.response()
                .putHeader("Location", generateAuthRedirectURI(buildHostURI()))
                .setStatusCode(302)
                .end();
    }

    private void logoutHandler(RoutingContext context) {
        context.clearUser();
        context.session().destroy();
        context.response().setStatusCode(204).end();
    }*/

    /*private String generateAuthRedirectURI(String from) {
        return oauth2.authorizeURL(new JsonObject()
                .put("redirect_uri", from + "/callback?redirect_uri=" + from)
                .put("scope", "")
                .put("state", ""));
    }*/

    private String buildHostURI() {
        int port = config().getInteger("api.gateway.http.port", DEFAULT_PORT);
        final String host = config().getString("api.gateway.http.address.external", "localhost");
        return String.format("https://%s:%d", host, port);
    }

    private void mockDiscoveryEndpoints(){
        //TODO make discovery work
        //API gateway default port 8787
        final boolean ssl = false;

        final String tlcDefaultHost = "localhost";
        final int tlcDefaultPort = 8086;
        final String tlcServiceName = "traffic-light-service";
        final String tlcRoot = "";

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
    }
}