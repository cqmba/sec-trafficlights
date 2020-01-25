package org.testclient;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;


public class Server extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    HttpServer server = vertx.createHttpServer();
    WebClient client = WebClient.create(vertx);

    server.requestHandler(req -> {
    	System.out.println("Hello req");
    	OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.CLIENT, new OAuth2ClientOptions()
    			  .setClientID("vertx-test2")
    			  .setClientSecret("ab4bf0ae-6b51-47e6-ba89-4455c5a0a825")
    			  .setTokenPath("http://localhost:8080/auth/realms/vertx/protocol/openid-connect/token")
    			);
    	
    	JsonObject tokenConfig = new JsonObject();
    	System.out.println("start authenticate");
    	oauth2.authenticate(tokenConfig, res -> {
    		  if (res.failed()) {
    		    System.err.println("Access Token Error: " + res.cause().getMessage());
    		  } else {
    		    // Get the access token object (the authorization code is given from the previous step).
    			  User token = res.result();
    			  System.out.println(token.principal().getString("access_token"));
    			  System.out.println(token);
    			  
    			  client.get(8787, "localhost", "/protected/test")
    			  .bearerTokenAuthentication(token.principal().getString("access_token"))
    			  .send(ar -> {
    				  	if (ar.succeeded()) {
    	      // Obtain response
    				  		HttpResponse<Buffer> response = ar.result();
   
    				  		System.out.println("Received response with status code" + response.statusCode());
    				  		System.out.println(response.bodyAsString());
    				  	} else {
    				  		System.out.println("Something went wrong " + ar.cause().getMessage());
    				  	}
    			  });
   	    	
  	    	
  	      req.response().putHeader("content-type", "text/html")
    	      .end("<h1>Hello vertx</h1>");
    		  }
    		});
    	
    	
    	
//    	client
//    	  .get(8787, "localhost", "/protected/test")
//    	  .send(ar -> {
//    	    if (ar.succeeded()) {
//    	      // Obtain response
//    	      HttpResponse<Buffer> response = ar.result();
//
//    	      System.out.println("Received response with status code" + response.statusCode());
//    	      System.out.println(response.bodyAsString());
//    	    } else {
//    	      System.out.println("Something went wrong " + ar.cause().getMessage());
//    	    }
//    	  });
//    	
//    	
//      req.response().putHeader("content-type", "text/html")
//      .end("<h1>Hello vertx</h1>");
    });

    server.listen(9090, ar -> {
      startFuture.completer().handle(ar.map((Void)null));
    });

  }
}
