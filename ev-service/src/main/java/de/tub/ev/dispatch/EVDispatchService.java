package de.tub.ev.dispatch;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.RoutingContext;

public interface EVDispatchService {

    static EVDispatchService getInstance(HttpClient client, Vertx vertx){
        return new EVDispatchServiceImpl(client, vertx);
    }

    void doDispatchEV(int id);

}
