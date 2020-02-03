package de.tub.apigateway;

import org.junit.Before;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class DispatcherTest {
	
	private Vertx vertx;
	
	@Before
	public void prep(TestContext context) {
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
	}
    
	
}
