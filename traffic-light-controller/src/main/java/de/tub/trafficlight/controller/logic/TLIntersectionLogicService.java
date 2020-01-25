package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLMode;
import de.tub.trafficlight.controller.entity.TLState;
import de.tub.trafficlight.controller.entity.TrafficLight;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;

import java.util.List;

public interface TLIntersectionLogicService {

    static TLIntersectionLogicService getInstance(int groupId, TLPersistenceService persistence){
        return new TLIntersectionLogicServiceImpl(groupId, persistence);
    }

    TLMode getMode();

    boolean setMode(TLMode mode);

    void doTransition();

    List<TrafficLight> getTLList();

    int getNextTransitionTimeMs();

    TLState getCurrentIntersectionState();
}
