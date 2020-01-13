package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.TLPosition;
import de.tub.trafficlight.controller.entity.TrafficLight;
import de.tub.trafficlight.controller.impl.TLControllerServiceImpl;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Optional;

public interface TLControllerService {
    static TLControllerService createService(Vertx vertx) {
        return new TLControllerServiceImpl(vertx);
    }

    Optional<TrafficLight> getSingleTLState(int tlId);

    List<TrafficLight> getTLList();

    TrafficLight addTL(TLPosition position);

    boolean removeTL(int tlId);

    boolean updateTL(int tlId, TrafficLight tl);
}
