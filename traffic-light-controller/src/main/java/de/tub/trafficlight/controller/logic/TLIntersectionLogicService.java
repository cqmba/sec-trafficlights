package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLMode;
import de.tub.trafficlight.controller.entity.TLState;
import de.tub.trafficlight.controller.entity.TrafficLight;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;

import java.util.List;

/**
 * A Service that supplies methods to model the layout and logic of a traffic intersection with TrafficLights.
 * Transitions can be done on the Service to alter its state. The Mode can be altered to change its Behaviour.
 */
public interface TLIntersectionLogicService {

    /**Gets a new Instance
     * @param groupId Group Id of the Traffic Light Intersection
     * @param persistence The current persistence Service Instance
     * @param emergency The current Emergency Service Instance
     * @return
     */
    static TLIntersectionLogicService getInstance(int groupId, TLPersistenceService persistence, IntersectionEmergencyService emergency){
        return new TLIntersectionLogicServiceImpl(groupId, persistence, emergency);
    }

    /**Returns the current Mode of the Intersection
     * @return the current Mode of the Intersection
     */
    TLMode getMode();

    /**Sets the Mode of the Intersection
     * @param mode The new Mode
     * @return the updated Mode
     */
    TLMode setMode(TLMode mode);

    /**
     * Alters the State of the Intersection to the following state.
     */
    void doTransition();

    /**Returns all TrafficLights of the Intersection Instance
     * @return a list of all TrafficLights
     */
    List<TrafficLight> getTLList();

    /**Returns the Int Value in ms of the Transition Time to the next State.
     * @return The time as int
     */
    int getNextTransitionTimeMs();

    /**Gets the current Intersection State
     * @return current Intersection State
     */
    TLState getCurrentIntersectionState();

    void resetIntersectionState();
}
