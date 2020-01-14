package de.tub.ev;

import de.tub.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;

public class EVDetectionVerticle extends RestAPIVerticle {
    private static final String SERVICE_NAME = "ev-service";

    private EVDetectionService service;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start(promise);

        final Router router = Router.router(vertx);
        // body handler
        router.route().handler(BodyHandler.create());

        router.get("/test").handler(this::getResource);
        //config
        String host = "localhost";
        int port = 8087;

        ServiceDiscovery.create(vertx).registerServiceImporter(new KubernetesServiceImporter(), new JsonObject());
        //can change default oauth token and namespace

        createHttpServer(router,host,port, new HttpServerOptions())
                .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
                .setHandler(promise.future().completer());
    }

    private void getResource(RoutingContext routingContext) {
        getAllEndpoints().setHandler(ar -> {
            if (ar.succeeded()) {
                List<Record> recordList = ar.result();
                for (Record record : recordList
                     ) {
                    System.out.println(record.getName());

                }
                System.out.println(recordList);
            } else {
                //fail
            }
        });

        discovery.getRecord( r -> r.getName().equals("traffic-light-service"), ar -> {
            if(ar.succeeded()){
                if (ar.result() != null){
                    System.out.println("Success in locating resource");
                    ServiceReference reference = discovery.getReference(ar.result());
                    HttpClient client = reference.getAs(HttpClient.class);
                    doDispatchEVRequest(client);
                    reference.release();
                } else {
                    System.out.println("failed: no matching service");
                }
            } else {
                System.out.println("lookup failed, going to default location");
                doDispatchEVRequest("localhost", 8086);
            }
        });
    }

    private void doDispatchEVRequest(HttpClient httpClient){
        //, String recordHost, int recordPort
        //TODO placeholder for testing
        final String path = "/lights";
        WebClient client = WebClient.wrap(httpClient);
        client
                .get(path)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        System.out.println("Success, status code " + response.statusCode());
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                    }
                });
    }

    private void doDispatchEVRequest(String host, int port){
        final String path = "/lights";
        WebClient client = WebClient.create(vertx);
        client
                .get(port, host, path)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        System.out.println("Success, status code " + response.statusCode());
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                    }
                });
    }

    private Future<List<Record>> getAllEndpoints() {
        Future<List<Record>> future = Future.future();
        discovery.getRecords(record -> record.getType().equals(HttpEndpoint.TYPE),
                future.completer());
        return future;
    }
}
