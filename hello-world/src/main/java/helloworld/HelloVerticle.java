package helloworld;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelloVerticle extends AbstractVerticle {

    private static final int DEFAULT_PORT = 8080;

    private static final Logger logger = LoggerFactory.getLogger(HelloVerticle.class);

    private OAuth2Auth oauth2;

    // Store our readingList
    private Map<Integer, Article> readingList = new LinkedHashMap<>();

    @Override
    public void start(Future<Void> fut) {
        // Populate our set of article
        createSomeData();

        // Create a router object.
        Router router = Router.router(vertx);

        // body handler
        router.route().handler(BodyHandler.create());

        JsonObject jsonObject;
        try {
            String filePath = new File("").getAbsolutePath().concat("/src/main/conf/keycloak.json");
            String jsonStr = IOUtils.toString(new FileReader(filePath));
            jsonObject = new JsonObject(jsonStr);
            //authHandler.setupCallback(router.get("/callback");
        } catch (IOException e) {
            jsonObject = new JsonObject("{\n" +
                    "  \"realm\": \"master\",\n" +
                    "  \"auth-server-url\": \"http://localhost:38080/auth\",\n" +
                    "  \"ssl-required\": \"external\",\n" +
                    "  \"resource\": \"hello-vertx\",\n" +
                    "  \"credentials\": {\n" +
                    "    \"secret\": \"be643556-44eb-4193-9aae-40325fd75138\"\n" +
                    "  },\n" +
                    "  \"confidential-port\": 0\n" +
                    "}");
        }

        oauth2 = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, jsonObject);
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)).setAuthProvider(oauth2));
        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(oauth2);

        String hostURI = buildHostURI();

        // set auth callback handler
        router.route("/callback").handler(context -> authCallback(oauth2, hostURI, context));

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first vertx application</h1>");
        });

        // Serve static resources from the /assets directory
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.get("/api/articles").handler(this::getAll);
        router.get("/api/articles/:id").handler(this::getOne);
        router.route("/api/articles*").handler(BodyHandler.create());
        router.post("/api/articles").handler(this::addOne);
        router.delete("/api/articles/:id").handler(this::deleteOne);
        router.put("/api/articles/:id").handler(this::updateOne);


        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(
                config -> {
                    if (config.failed()) {
                        fut.fail(config.cause());
                    } else {
                        // Create the HTTP server and pass the "accept" method to the request handler.
                        vertx
                                .createHttpServer()
                                .requestHandler(router::accept)
                                .listen(
                                        // Retrieve the port from the configuration,
                                        // default to 8080.
                                        config.result().getInteger("HTTP_PORT", 8080),
                                        result -> {
                                            if (result.succeeded()) {
                                                fut.complete();
                                            } else {
                                                fut.fail(result.cause());
                                            }
                                        }
                                );
                    }
                }
        );
    }


    // Create a readingList
    private void createSomeData() {
        Article article1 = new Article("Fallacies of distributed computing", "https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing");
        readingList.put(article1.getId(), article1);
        Article article2 = new Article("Reactive Manifesto", "https://www.reactivemanifesto.org/");
        readingList.put(article2.getId(), article2);
    }

    private void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(readingList.values()));
    }

    private void addOne(RoutingContext routingContext) {
        Article article = routingContext.getBodyAsJson().mapTo(Article.class);
        readingList.put(article.getId(), article);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(article));
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        try {
            Integer idAsInteger = Integer.valueOf(id);
            readingList.remove(idAsInteger);
            routingContext.response().setStatusCode(204).end();
        } catch (NumberFormatException e) {
            routingContext.response().setStatusCode(400).end();
        }
    }


    private void getOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        try {
            Integer idAsInteger = Integer.valueOf(id);
            Article article = readingList.get(idAsInteger);
            if (article == null) {
                // Not found
                routingContext.response().setStatusCode(404).end();
            } else {
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(article));
            }
        } catch (NumberFormatException e) {
            routingContext.response().setStatusCode(400).end();
        }
    }

    private void updateOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        try {
            Integer idAsInteger = Integer.valueOf(id);
            Article article = readingList.get(idAsInteger);
            if (article == null) {
                // Not found
                routingContext.response().setStatusCode(404).end();
            } else {
                JsonObject body = routingContext.getBodyAsJson();
                article.setTitle(body.getString("title")).setUrl(body.getString("url"));
                readingList.put(idAsInteger, article);
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(article));
            }
        } catch (NumberFormatException e) {
            routingContext.response().setStatusCode(400).end();
        }

    }

    private String buildHostURI() {
        //int port = config().getInteger("api.gateway.http.port", DEFAULT_PORT);
        int port = 8080;
        //final String host = config().getString("api.gateway.http.address.external", "localhost");
        final String host = "localhost";
        return String.format("https://%s:%d", host, port);
    }

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
}

