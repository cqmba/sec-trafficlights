package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.TrafficLight;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TLControllerVerticleTest {


    private Vertx vertx;


    private final String truststorepath = "tlc_truststore.jks";
    private final String truststorepass = "UedJ6AtmjcwF7qNQ";

    private final String VALID_MANAGER_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiJmMjRkNmJjZC0xNjA0LTQ2MTEtYWJkOS0yYzQ1N2FjNDVmYTQiLCJleHAiOjE1ODA3NDE5OTUsIm5iZiI6MCwiaWF0IjoxNTgwNzQxNjk1LCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6Ijg3ZGU5NDcxLWVlZDYtNDQ3ZS1hOTFmLTk2YmQ1ZjRhYWNkNSIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXRsYzIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiIwNTZiZmE5NC1kMjdjLTRjNTktODE2OS1iNDU4NzNhY2JhZTIiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImV2IiwibWFuYWdlciIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJtYW5hZ2VyMiJ9.VhHDSvtESgl9W0bJNScQ-bwC6Don4NLC2j_9-zB9imXPpb477yoFgEFmVUPFFCPnTzNZbzNoZoK1XrMLAAPcQDLz6hZK71vLT1Rk1ImESLJvixose144uuYuzvAsw6cTxKtgiSeoFN0nYdT85zPgalWB_dsHjclgdAWuPFubneappVMal_OlGlGL8Kn5yFsOWWXBltjhmCYgsgLyF8TMg34dpnOKwokOyQ6YhE19nhjJO5fB7gVqzeUmtlw-qR39G1JQbRelFmgu2w_ZSRUtSF3zxLx059KuC6wGywhrom35QS5QyHPOKCVcvO75pjPqWmL0M6dKTg15IdNnc628iA";
    private final String VALID_EV_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiJjOTViZThkNS0wNGY5LTQ4MjYtYTBiMy04MWRiYTk4NTVjNDYiLCJleHAiOjE1ODA5MzA4MDAsIm5iZiI6MCwiaWF0IjoxNTgwOTMwNTAwLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImU3NzU2Y2Y0LWQ2YmYtNDIwYy1iM2Q2LWEwMDJhN2UxNTRlMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXRsYzIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiJkY2M1NmFiNC1lZmMzLTRmNzUtOWY5Ni05OWIwNGJkMTU5MmUiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImV2Iiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImV2dXNlciJ9.deJlnGsd5bkKwlfN9XHVuNVM0TK3uSFjUAycq6q0POxJ0YWP2iO7bBm3cOo73r24nYmQrphgCLzjNXMG7qN9bTQd1nfmgoT1cJWROrdG_IFKg3QlGt-sgWokN1JBRQu3yZG-D9fuyWK2NysrWQQDh2-t7yXDDPYSq7QtDsfi33BTRjR6c3Z_QNBL4tFJJWfgpBUmJaEYnPWhMyQGSry-Mxe6Zpuj6jIMCrj9mf7igAr49g4knV_4Ucl_wFA_7wZYV5OKQJwqT5s741eK6WV7Kn6qU5iUSAjra2Wda78-PmHrdUGJEUigEnvDRnrW0o3t30w8Xur8R6EBu_rVlADK6Q";
    private final String UNPRIVILEGED_USER_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiJjNGQzY2RlNi1hYmI5LTQzMTYtYTc4ZC1hMWVlMWI0OGQ2YTYiLCJleHAiOjE1ODA5MzE4OTIsIm5iZiI6MCwiaWF0IjoxNTgwOTMxNTkyLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4Iiwic3ViIjoiZTNlYjEzNzEtMDIxNS00ZTZjLTgyMTQtM2M5M2UyMGUzNmYyIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidmVydHgtdGxjMiIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjA4ZWNkYjJhLTA2MDMtNDljYy05Mjg3LTA4YWYyMmJlNjBlMiIsImFjciI6IjEiLCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6Im5vcmlnaHRzdXNlciJ9.ZDwxPRBBt34IHvdDOURCEXCS-NCsghg25GjcjJZbnBZo5cJe7nM0xePF_SUtZ_EqjzohSI9vHNFYJR0KeUz5-jBJyFnpFoFm4AnzFTFZ9AtKF5uP_1PyffnHJW1xhGTCuKDeDElga96axke1L_gFyeNZCLy0hLEIHPx6jchdeAoISrH0P01021RecSGhOMLOQSb20VuVOgMU3Bai3NioT0nHXVFMUJmWvUmT8-cCf22p3_zR4GX_ujyPwapx7ksTIuYytOefSPIVPsfvL6_31pNEUUDYbOxZg1Ls1MSwu33eX40VJh1QvSleQaNF5A__LfnGprWRWVU7v2HhcHbVpQ";
    private final String VALID_OBSERVER_TOKEN = "";
    private final String VALID_ADMIN_TOKEN = "";

    private WebClientOptions validWebClientOptions = new WebClientOptions()
            .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
            .removeEnabledSecureTransportProtocol("TLSv1")
            .removeEnabledSecureTransportProtocol("TLSv1.1")
            .removeEnabledSecureTransportProtocol("TLSv1.2")
            .addEnabledSecureTransportProtocol("TLSv1.3")
            .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
            .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256");

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        JsonObject keycloakJson = new JsonObject()
                .put("api.name", "traffic-light-service")
                .put("keystore.password", "UedJ6AtmjcwF7qNQ")
                .put("keystore.path", "tlc_keystore.jks");

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(keycloakJson);
        vertx.deployVerticle(TLControllerVerticle.class.getName(), options,
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        TrafficLight.resetCounter();
    }


    @Test
    public void testIfConnectionClosedNoSSL(TestContext context) {
        final Async async = context.async();

        WebClient.create(vertx).get(8086, "localhost", "/lights")
                .send( ar -> {
                    if (ar.failed()){
                        context.assertTrue(ar.cause().getMessage().contains("Connection was closed"));
                        async.complete();
                    }
                    else {
                        //fail, should be closed
                        context.fail();
                        async.isFailed();
                    }
                });
    }

    @Test
    public void testOpenConnectionWithTLSv1_3AndReturnUnauthorized(TestContext context){
        final Async async = context.async();

        WebClient.create(vertx, new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256")
        ).get(8086, "localhost", "/lights")
        .send( ar -> {
            if (ar.succeeded()){
                context.assertTrue(ar.result().statusCode() == 401);
                async.complete();
            }
        });
    }

    @Test
    public void testRejectTLSv1Connection(TestContext context){
        final Async async = context.async();

        WebClient.create(vertx, new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1")
        ).get(8086, "localhost", "/lights")
                .send( ar -> {
                    if (ar.succeeded()){
                        context.fail();
                        async.complete();
                    } else {
                        context.assertTrue(ar.cause().toString().contains("javax.net.ssl.SSLHandshakeException"));
                        async.complete();
                    }
                });
    }

    @Test
    public void testRejectTLSv1_1Connection(TestContext context){
        final Async async = context.async();

        WebClient.create(vertx, new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .addEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .removeEnabledSecureTransportProtocol("TLSv1")
        ).get(8086, "localhost", "/lights")
                .send( ar -> {
                    if (ar.succeeded()){
                        context.fail();
                        async.complete();
                    } else {
                        context.assertTrue(ar.cause().toString().contains("javax.net.ssl.SSLHandshakeException"));
                        async.complete();
                    }
                });
    }

    @Test
    public void testRejectTLSv1_2Connection(TestContext context){
        final Async async = context.async();

        WebClient.create(vertx, new WebClientOptions()
                .setSsl(true).setTrustStoreOptions(new JksOptions().setPath(truststorepath).setPassword(truststorepass))
                .addEnabledSecureTransportProtocol("TLSv1.2")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1")
        ).get(8086, "localhost", "/lights")
                .send( ar -> {
                    if (ar.succeeded()){
                        context.fail();
                        async.complete();
                    } else {
                        context.assertTrue(ar.cause().toString().contains("javax.net.ssl.SSLHandshakeException"));
                        async.complete();
                    }
                });
    }

    @Test
    public void testAccessAPIGetAllWhenAuthorizedAsManagerWithValidToken(TestContext context){

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.GET, 8086, "localhost", "/lights");

        request.addQueryParam("token", VALID_MANAGER_TOKEN)
                .send( ar -> {
                    if (ar.succeeded()){
                        HttpResponse<Buffer> result = ar.result();
                        context.assertTrue(result.statusCode() == 200);
                        //answer contains something
                        context.assertTrue(result.bodyAsString().contains("color"));
                        async.complete();
                    } else {
                        context.fail();
                        async.complete();
                    }
                });
    }

    @Test
    public void testChangeColorSuccessfullWhenAuthorizedAsManager(TestContext context){

        final int SENSOR_ID = 3;

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.PUT, 8086, "localhost", "/lights/"+SENSOR_ID+"/colors");

        JsonObject payload = new JsonObject().put("group", 1).put("color", "GREEN");
        request.addQueryParam("token", VALID_MANAGER_TOKEN);
        request.putHeader("Content-Type", "application/json");
        request.sendJsonObject(payload, ar -> {
            if (ar.succeeded()){
                HttpResponse<Buffer> result = ar.result();
                context.assertTrue(result.statusCode() == 200);
                //answer contains something
                context.assertTrue(result.bodyAsString().contains("\"id\" : " + SENSOR_ID));
                context.assertTrue(result.bodyAsString().contains("\"color\" : \"GREEN\""));
                async.complete();
            } else {
                context.fail();
                async.complete();
            }
        });
    }

    @Test
    public void testChangeColorSuccessfullWhenAuthorizedAsEV(TestContext context){

        final int SENSOR_ID = 3;

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.PUT, 8086, "localhost", "/lights/"+SENSOR_ID+"/colors");

        JsonObject payload = new JsonObject().put("group", 1).put("color", "GREEN");
        request.addQueryParam("token", VALID_EV_TOKEN);
        request.putHeader("Content-Type", "application/json");
        request.sendJsonObject(payload, ar -> {
            if (ar.succeeded()){
                HttpResponse<Buffer> result = ar.result();
                context.assertTrue(result.statusCode() == 200);
                //answer contains something
                context.assertTrue(result.bodyAsString().contains("success"));
                async.complete();
            } else {
                context.fail();
                async.complete();
            }
        });
    }

    @Test
    public void testMaliciousInputReturnsHttp400(TestContext context){

        final int SENSOR_ID = 3;

        final String VERY_MALICIOUS_JS = "<script>alert('XSS')<script>";

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.PUT, 8086, "localhost", "/lights/"+SENSOR_ID+"/colors");

        JsonObject payload = new JsonObject().put("group", 1).put("color", VERY_MALICIOUS_JS);
        request.addQueryParam("token", VALID_EV_TOKEN);
        request.putHeader("Content-Type", "application/json");
        request.sendJsonObject(payload, ar -> {
            if (ar.succeeded()){
                HttpResponse<Buffer> result = ar.result();
                context.assertTrue(result.statusCode() == 400);
                async.complete();
            } else {
                context.fail();
                async.complete();
            }
        });
    }


    @Test
    public void testUnpriviledgedUserRequestColorChangeReturns403(TestContext context){

        final int SENSOR_ID = 3;

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.PUT, 8086, "localhost", "/lights/"+SENSOR_ID+"/colors");

        JsonObject payload = new JsonObject().put("group", 1).put("color", "GREEN");
        request.addQueryParam("token", UNPRIVILEGED_USER_TOKEN);
        request.putHeader("Content-Type", "application/json");
        request.sendJsonObject(payload, ar -> {
            if (ar.succeeded()){
                HttpResponse<Buffer> result = ar.result();
                context.assertTrue(result.statusCode() == 403);
                async.complete();
            } else {
                context.fail();
                async.complete();
            }
        });
    }

    /*
    @Test
    public void testObserver(TestContext context){

        final int SENSOR_ID = 3;

        final Async async = context.async();

        HttpRequest<Buffer> request = WebClient.create(vertx, validWebClientOptions)
                .request(HttpMethod.PUT, 8086, "localhost", "/lights/"+SENSOR_ID+"/colors");

        JsonObject payload = new JsonObject().put("group", 1).put("color", "GREEN");
        request.addQueryParam("token", UNPRIVILEGED_USER_TOKEN);
        request.putHeader("Content-Type", "application/json");
        request.sendJsonObject(payload, ar -> {
            if (ar.succeeded()){
                HttpResponse<Buffer> result = ar.result();
                context.assertTrue(result.statusCode() == 403);
                async.complete();
            } else {
                context.fail();
                async.complete();
            }
        });
    }*/

    @Test
    public void testRejectTLSv13ConnectionWithWeakCipher(TestContext context){

    }
}