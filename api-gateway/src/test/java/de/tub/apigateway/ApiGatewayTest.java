package de.tub.apigateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.report.ReportOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ApiGatewayTest {
	
	private WebClient c;
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        JsonObject keycloakJson = new JsonObject()
        		.put("keystore.password", "4mB8nqJd5YEHFkw6")
        		.put("keystore.path", "gateway_keystore.jks")
        		.put("truststore.path", "gateway_truststore.jks")
        		.put("truststore.pass", "4mB8nqJd5YEHFkw6")
        		.put("keycloak.json", new JsonObject()
        				.put("realm", "vertx")
        				.put("auth-server-url", "https://localhost:8443/auth")
        				.put("ssl-required", "external")
        				.put("resource", "vertx-tlc2")
        				.put("verify-token-audience", true)
        				.put("credentials", new JsonObject().put("secret", "682d858d-0875-4ff2-93b3-bcd6af4c5b1d"))
        				.put("use-resource-role-mappings", true)
        				.put("confidential-port", 0)
        				.put("policy-enforcer", new JsonObject()));
       
        DeploymentOptions options = new DeploymentOptions().setConfig(keycloakJson);
        vertx.deployVerticle(APIGatewayVerticle.class.getName(), options,
                context.asyncAssertSuccess());
        
        final String truststorepath = "gateway_truststore.jks";
        final String truststorepass = "4mB8nqJd5YEHFkw6";
        
        this.c = WebClient.create(vertx, new WebClientOptions().setSsl(true)
        		.setTrustStoreOptions(new JksOptions()
        				.setPath(truststorepath)
        				.setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256"));	
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void apiGatewayReachable(TestContext context) {
        final Async async = context.async();
                		
        c.get(8787, "localhost", "/").send( ar -> {
        	try {
                	context.assertFalse(ar.failed());
                	context.assertTrue(ar.result().body().toString().contains("gateway"));
                    async.complete();	
                } catch (RuntimeException e){
                	e.printStackTrace();
                	fail();
                }
        	});
    }
    
    @Test
    public void corsHeaderAvailable(TestContext context) {
    	final Async async = context.async();

    	c.request(HttpMethod.OPTIONS, 8787, "localhost", "/lights").send( ar -> {
            context.assertFalse(ar.failed());
            context.assertNotNull(ar.result().getHeader("Access-Control-Allow-Methods"), "Header \"Access-Control-Allow-Methods\" missing");
            context.assertNotNull(ar.result().getHeader("Access-Control-Allow-Headers"), "Header \"Access-Control-Allow-Headers\" missing");
            context.assertNotNull(ar.result().getHeader("Access-Control-Allow-Origin"), "Header \"Access-Control-Allow-Origin\" missing");
            async.complete();	
            
    	});
    }
    
}
