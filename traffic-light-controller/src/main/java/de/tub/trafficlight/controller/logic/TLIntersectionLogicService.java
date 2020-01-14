package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.List;

public interface TLIntersectionLogicService {
    void doTransition(boolean isEmergencyMain, boolean isEmergencySide);

    List<TrafficLight> getTLList();

    int getNextTransitionTimeMs();

    String getCurrentIntersectionState();
}
