package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLPosition;
import de.tub.trafficlight.controller.entity.TLType;
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

    TrafficLight addTL(int id, TLColor color, TLPosition position, TLType type, int groupId);

    boolean removeTL(int tlId);

    boolean updateTL(int tlId, TrafficLight tl);

    TrafficLight changeColor(int tlId, TLColor color);

    boolean changeToGreen(int tlId);
}
