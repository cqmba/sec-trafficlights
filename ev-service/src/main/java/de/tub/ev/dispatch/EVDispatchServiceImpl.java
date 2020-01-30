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

/**
 * Implements the EV Dispatch Service
 */
public class EVDispatchServiceImpl implements EVDispatchService {

    private static final String BASE_TLC_API = "api/lights/";

    private static final Logger logger = LogManager.getLogger(EVDispatchServiceImpl.class);

    private String endpoint;
    private Vertx vertx;
    private WebClientOptions webClientOptions;
    private OAuth2Auth oauth2;
    private static final boolean MOCKED_SENSOR_RESULT = false;
    private static final int MOCKED_SENSOR_ID = 1;
    private static final int SENSOR_INTERVAL = 1000;

    /**Constructor of the Dispatch Service, which returns a new Instance with configured TLS and Auth Settings, loaded from Vertx Config.
     * @param endpoint The absolute URI of the targeted Service
     * @param vertx The current Vertx instance
     * @param config The passed Vertx config
     */
    public EVDispatchServiceImpl(String endpoint, Vertx vertx, JsonObject config) {
        this.endpoint = endpoint;
        this.vertx = vertx;
        String token_path = config.getString("token.path");
        String auth_clientId = config.getString("auth.clientid");
        String client_secret = config.getString("client.secret");
        //the real values are loaded from vertx config, these are default values
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
                .setFlow(OAuth2FlowType.CLIENT).setClientID(auth_clientId)
                .setTokenPath(token_path).setClientSecret(client_secret);

        this.oauth2 = OAuth2Auth.create(vertx, oAuth2ClientOptions);
        pullSensorPeriodically();
    }


    /**
     * Mocks how a periodical pulling of a sensor could look like.
     * If a true value is detected, the Green-Light Request is delegated.
     */
    private void pullSensorPeriodically(){
        vertx.setPeriodic(SENSOR_INTERVAL, event -> {
            if (MOCKED_SENSOR_RESULT){
                authenticateAndDelegateToAPI(MOCKED_SENSOR_ID);
            }
        });
    }

    /**Mocks a detected Emergency Vehicle and initiates a Green-Light request at the corresponding Traffic Light.
     * @param id Id of the targeted Traffic Light.
     * @param routingContext Current Request RoutingContext
     */
    @Override
    public void sendSensorDetection(int id, RoutingContext routingContext) {
        //manually triggered
        authenticateAndDelegateToAPIonRequest(id, routingContext);
    }

    /**Authenticates the Service by leveraging the Auth Service. On success, delegates to dispatching the Request.
     * @param id The targeted Traffic Light Id.
     */
    private void authenticateAndDelegateToAPI(int id){
        JsonObject tokenConfig = new JsonObject();
        oauth2.authenticate(tokenConfig, res -> {
            if (res.failed()) {
                logger.info("Error when trying to authenticate: " + res.cause().getMessage());
            } else {
                User user = res.result();
                if (user.principal().containsKey("access_token")){
                    logger.debug("User " + user + "authenticated, contains a token");
                    doDispatchToAPI(user, id);
                } else {
                    logger.info("User has no access token");
                }
            }
        });
    }

    /**Authenticates the Service by leveraging the Auth Service. On success, delegates to dispatching the Request.
     * @param id The targeted Traffic Light Id.
     * @param routingContext The current Routing Context.
     */
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

    /**Dispatches the EV Green-Light Requests that was mocked by a API Request for Presentation purposes
     * @param user The Authenticated User.
     * @param id The targeted Traffic Light Id.
     * @param routingContext The current RoutingContext
     */
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

    /**Dispatches a EV Green-Light Request.
     * @param user The authenticated User.
     * @param id The targeted Traffic Light.
     */
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
