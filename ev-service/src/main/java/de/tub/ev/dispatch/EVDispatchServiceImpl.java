package de.tub.ev.dispatch;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EVDispatchServiceImpl implements EVDispatchService {

    private static final String BASE_TLC_API = "api/lights/";
    private static final String TOKEN_PATH = "http://localhost:8080/auth/realms/vertx/protocol/openid-connect/token";
    private static final String AUTH_CLIENTID = "vertx-test2";

    private static final Logger logger = LogManager.getLogger(EVDispatchServiceImpl.class);

    private String endpoint;
    private Vertx vertx;
    private WebClientOptions webClientOptions;
    private OAuth2Auth oauth2;
    private static final boolean MOCKED_SENSOR_RESULT = false;
    private static final int MOCKED_SENSOR_ID = 1;
    private static final int SENSOR_INTERVAL = 1000;

    public EVDispatchServiceImpl(String endpoint, Vertx vertx, JsonObject config) {
        this.endpoint = endpoint;
        this.vertx = vertx;
        String truststorepath = config.getString("truststore.path", "ev_truststore.jks");
        String truststorepass = config.getString("truststore.pass", "password");
        this.webClientOptions = new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
                .setVerifyHost(true);

        OAuth2ClientOptions oAuth2ClientOptions = new OAuth2ClientOptions()
                .setFlow(OAuth2FlowType.CLIENT).setClientID(AUTH_CLIENTID)
                .setTokenPath(TOKEN_PATH);

        this.oauth2 = OAuth2Auth.create(vertx, oAuth2ClientOptions);
        pullSensorPeriodically();
    }

    private void pullSensorPeriodically(){
        vertx.setPeriodic(SENSOR_INTERVAL, event -> {
            if (MOCKED_SENSOR_RESULT){
                authenticateAndDelegateToAPI(MOCKED_SENSOR_ID);
            }
        });
    }


    @Override
    public void sendSensorDetection(int id, RoutingContext routingContext) {
        //manually triggered
        authenticateAndDelegateToAPIonRequest(id, routingContext);
    }

    private void authenticateAndDelegateToAPI(int id){
        JsonObject tokenConfig = new JsonObject();
        oauth2.authenticate(tokenConfig, res -> {
            if (res.failed()) {
                logger.info("Error when trying to authenticate: " + res.cause().getMessage());
            } else {
                User user = res.result();
                if (user.principal().containsKey("access_token")){
                    logger.debug("User " + user + "authenticated, Token received: "+ user.principal().getString("access_token"));
                    doDispatchToAPI(user, id);
                } else {
                    logger.info("User has no access token");
                }
            }
        });
    }

    private void authenticateAndDelegateToAPIonRequest(int id, RoutingContext routingContext){
        JsonObject tokenConfig = new JsonObject();
        oauth2.authenticate(tokenConfig, res -> {
            if (res.failed()) {
                logger.info("Error when trying to authenticate: " + res.cause().getMessage());
            } else {
                User user = res.result();
                if (user.principal().containsKey("access_token")){
                    logger.debug("User " + user + "authenticated, Token received: "+ user.principal().getString("access_token"));
                    doDispatchToAPIonRequest(user, id, routingContext);
                } else {
                    logger.info("User has no access token");
                }
            }
        });
    }

    private void doDispatchToAPIonRequest(User user, int id, RoutingContext routingContext){
        final String path = BASE_TLC_API + id +"/colors";
        JsonObject payload = new JsonObject().put("color", "GREEN").put("group", 1);

        WebClient webClient = WebClient.create(vertx, webClientOptions);
        webClient
                .putAbs(endpoint + path)
                .bearerTokenAuthentication(user.principal().getString("access_token"))
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

    private void doDispatchToAPI(User user, int id){
        //sensor triggered
        final String path = BASE_TLC_API + id +"/colors";
        JsonObject payload = new JsonObject().put("color", "GREEN").put("group", 1);
        WebClient webClient = WebClient.create(vertx, webClientOptions);
        webClient
                .putAbs(endpoint + path)
                .bearerTokenAuthentication(user.principal().getString("access_token"))
                .sendJsonObject(payload, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        logger.debug("Successfully reported EV on sensor "+ id+", status code " + response.statusCode() + "\n " +response.bodyAsString());
                    } else {
                        logger.error("Something went wrong " + ar.cause().getMessage());
                    }
                });
    }
}
