package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.*;
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

    TrafficLight addTL(int id, TLColor color, TLPosition position, TLType type);

    boolean removeTL(int tlId);

    TrafficLight changeToGenericColorOnManagerRequest(int tlId, TLColor color);

    boolean changeToGreenOnEVRequest(int tlId);

    TLMode getGroupMode(int groupId);

    TLMode switchGroupMode(int group, TLMode newMode);
}
