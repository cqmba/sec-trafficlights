package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLState;
import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.List;

public interface TLIntersectionLogicService {
    void doTransition();

    List<TrafficLight> getTLList();

    int getNextTransitionTimeMs();

    TLState getCurrentIntersectionState();
}
