package de.tub.apigateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ApiGatewayTest {
	
	private WebClient c;
    private Vertx vertx;
    
	private String adminToken;
	private String validToken;
	private String invalidToken;
	
	private static final String ADMIN_USERNAME = "admin_test";
	private static final String ADMIN_PASSWORD = "password";
	
	private static final String MANAGER_USERNAME = "manager_test";
	private static final String MANAGER_PASSWORD = "password";
	
	

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        Async admTokenAsync = context.async();
        Async normalTokenAsync = context.async();
        
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
        
        c = WebClient.create(vertx, new WebClientOptions().setSsl(true)
        		.setTrustStoreOptions(new JksOptions()
        				.setPath(truststorepath)
        				.setPassword(truststorepass))
                .removeEnabledSecureTransportProtocol("TLSv1")
                .removeEnabledSecureTransportProtocol("TLSv1.1")
                .removeEnabledSecureTransportProtocol("TLSv1.2")
                .addEnabledSecureTransportProtocol("TLSv1.3")
                .addEnabledCipherSuite("TLS_AES_256_GCM_SHA384")
                .addEnabledCipherSuite("TLS_AES_128_GCM_SHA256"));

      //get valid admin token
        
        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.PASSWORD, new OAuth2ClientOptions()
  			  .setClientID("vertx-test2")
  			  .setClientSecret("ab4bf0ae-6b51-47e6-ba89-4455c5a0a825")
  			  .setTokenPath("https://localhost:8443/auth/realms/vertx/protocol/openid-connect/token")
);

        JsonObject tokenConfig = new JsonObject()
          .put("username", ApiGatewayTest.ADMIN_USERNAME)
          .put("password", ApiGatewayTest.ADMIN_PASSWORD);

        // Callbacks
        // Save the access token
        oauth2.authenticate(tokenConfig, res -> {
          if (res.failed()) {
            System.err.println("Access Token Error: " + res.cause().getMessage());
            context.fail();
          } else {
            // Get the access token object (the authorization code is given from the previous step).
            User token = res.result();
            adminToken = token.principal().getString("access_token");
            admTokenAsync.complete();
          }
        });
        
	    
        //get valid token without permission
        
        tokenConfig = new JsonObject()
                .put("username", ApiGatewayTest.MANAGER_USERNAME)
                .put("password", ApiGatewayTest.MANAGER_PASSWORD);
        
        oauth2.authenticate(tokenConfig, res -> {
            if (res.failed()) {
              System.err.println("Access Token Error: " + res.cause().getMessage());
              context.fail();
            } else {
              // Get the access token object (the authorization code is given from the previous step).
              User token = res.result();
              validToken = token.principal().getString("access_token");
               normalTokenAsync.complete();
            }
          });
        
	    //token without verification
	    invalidToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiJiY2I3NTFjYy1lMjE3LTQyM2ItYjYxMi1iMGM4MmJkNTIyNDQiLCJleHAiOjE1ODA3NzM0NTgsIm5iZiI6MCwiaWF0IjoxNTgwNzczMTU4LCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjA2YTJhODNhLTVhMTItNDc1OS04YjgwLTYyNDFmMjczNmNlNyIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXRsYzIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiI0ZTI4ZDYyZi01ZGRkLTQ1NjctYWQ0ZC02MTE4YjY0NDc2ZGQiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm1hbmFnZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoibWFuYWdlcl90ZXN0In0.LFW5MW0L1zenwdO7oOiwVyBvgDKh4kN8zoF9Db8sd7X3nn8VGSKPLj3WC3Te5My3y7vCpuyXYbLkWVjgSCn8OhUZF6baIKcDXVw20-3pXJY1V5tKctwMuMf3nCijCP__VKNFYGArBR-ajGx4UwUVQOvpjVLQaj__VE3_fuqCDFOBnNvMUk4D7bkKCUArcluX82KHbP3XyaxPEdj4DmhN4G_qVK0iF0Ok4YWGT5kKT4WHKAfMoFYjEvA9ti784KxzH9yqf03R2UT3pQEkKX0OtO9Aj-FWaqVacIMRMjG5isxkGDYmohi6U0M50f6h420UkUgS6_SvzMg8BIUeDZZBnw";
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
    
    @Test
	public void dispatchableRequest(TestContext context) {
    	final Async async = context.async();
    	
    	c.get(8787, "localhost", "/api/lights")
		.bearerTokenAuthentication(this.adminToken)
		.send(ar ->{
			context.assertFalse(ar.failed());
			//502, since the called microservice is not reachable. Anyways, this assures the request will be forwarded correctly.
        	context.assertTrue(ar.result().statusCode() == 502, "Expected 502 but got "+ar.result().statusCode());
            async.complete();
		});
	}
	
	@Test
	public void notDispatchableRequest(TestContext context) {
		final Async async = context.async();
		
		c.get(8787, "localhost", "/api/keks")
		.bearerTokenAuthentication(this.adminToken)
		.send(ar ->{
			context.assertFalse(ar.failed());
        	context.assertTrue(ar.result().statusCode() == 404, "Expected 404 but got "+ar.result().statusCode());
            async.complete();
		});
	}
	
	@Test
	public void validHealthAPiCall(TestContext context) {
		final Async async = context.async();
		
		c.get(8787, "localhost", "/health")
		.bearerTokenAuthentication(this.adminToken)
		.send(ar ->{
			context.assertFalse(ar.failed());
        	context.assertTrue(ar.result().statusCode() == 200, "Expected 200 but got "+ar.result().statusCode());
            async.complete();
		});
		
	}
	
	@Test
	public void notPermittedHealthAPICall(TestContext context) {
		final Async async = context.async();
		
		c.get(8787, "localhost", "/health")
		.bearerTokenAuthentication(this.validToken)
		.send(ar ->{
			context.assertFalse(ar.failed());
        	context.assertTrue(ar.result().statusCode() == 403, "Expected 403 but got "+ar.result().statusCode());
            async.complete();
		});
	}
	
	@Test
	public void invalidTokenAPICall(TestContext context) {
		final Async async = context.async();
		
		c.get(8787, "localhost", "/health")
		.bearerTokenAuthentication(this.invalidToken)
		.send(ar ->{
			context.assertFalse(ar.failed());
        	context.assertTrue(ar.result().statusCode() == 401, "Expected 401 but got "+ar.result().statusCode());
            async.complete();
		});
	}
    
}
