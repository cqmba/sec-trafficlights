package de.tub.ev.dispatch;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Service, which can send Green Light Requests of Emergency Vehicles to the Traffic Light Controller at the Intersection
 */
public interface EVDispatchService {

    /**Gets a new Instance of the Service responsible for sending Green-Light Requests to the TLC.
     * @param endpoint The absolute base URI of the targeted Service endpoint.
     * @param vertx The current Vertx Instance
     * @param config The current Vertx Config
     * @return A new Instance of the Service
     */
    static EVDispatchService getInstance(String endpoint, Vertx vertx, JsonObject config){
        return new EVDispatchServiceImpl(endpoint, vertx, config);
    }

    /**Mocks a detected Emergency Vehicle and initiates a Green-Light request at the corresponding Traffic Light.
     * @param id Id of the targeted Traffic Light.
     * @param routingContext Current Request RoutingContext
     */
    void sendSensorDetection(int id, RoutingContext routingContext);

}
