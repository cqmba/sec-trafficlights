package de.tub.trafficlight.controller;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;
@RunWith(VertxUnitRunner.class)
public class TLControllerVerticleTest {


    private Vertx vertx;

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
                        context.assertTrue(false);
                        async.isFailed();
                    }
                });
    }

    @Test
    public void testIfConnectionIsOpenWithSSL(TestContext context){
        final Async async = context.async();

        final String truststorepath = "tlc_truststore.jks";
        final String truststorepass = "UedJ6AtmjcwF7qNQ";

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
}