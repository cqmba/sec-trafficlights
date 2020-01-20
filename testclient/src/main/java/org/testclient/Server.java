package org.testclient;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.client.HttpResponse;


public class Server extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    HttpServer server = vertx.createHttpServer();
    WebClient client = WebClient.create(vertx);

    server.requestHandler(req -> {
    	
    	OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, new OAuth2ClientOptions()
    			  .setClientID("vertx-test")
    			  .setClientSecret("98ba74b5-e0ff-4a23-b59e-b4e9c8658d09")
    			  .setSite("http://localhost:8080/auth/")
    			  .setTokenPath("/oauth/access_token")
    			  .setAuthorizationPath("/oauth/authorize")
    			);
    	
    	
    	
    	client
    	  .get(8787, "localhost", "/protected/test")
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
    });

    server.listen(9090, ar -> {
      startFuture.completer().handle(ar.map((Void)null));
    });

  }
}
