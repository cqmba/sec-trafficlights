package de.tub.ev.dispatch;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface EVDispatchService {

    static EVDispatchService getInstance(String endpoint, Vertx vertx, JsonObject config){
        return new EVDispatchServiceImpl(endpoint, vertx, config);
    }

    void sendSensorDetection(int id, RoutingContext routingContext);

}
