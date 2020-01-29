package de.tub.trafficlight.controller;

import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.impl.TLControllerServiceImpl;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Optional;

/**
 * The Interface to the Controller Service, which handles how the Intersection States are timed and managed.
 */
public interface TLControllerService {

    /**Creates a new Instance of the Controller Service.
     * @param vertx The current Vertx Instance
     * @return A new Instance
     */
    static TLControllerService createService(Vertx vertx) {
        return new TLControllerServiceImpl(vertx);
    }

    /**Looks up if a TrafficLight can be retrieved for the specified Id.
     * @param tlId The targeted TrafficLight Id.
     * @return An Optional TrafficLight
     */
    Optional<TrafficLight> getSingleTLState(int tlId);

    /**Returns all registered Traffic Lights as a List.
     * @return All registered Traffic Lights
     */
    List<TrafficLight> getTLList();

    /**Adds a TrafficLight to Registration.
     * @param id The new Id.
     * @param color The new Color.
     * @param position THe new Position.
     * @param type The new Type.
     * @return The created Instance of TrafficLight
     */
    TrafficLight addTL(int id, TLColor color, TLPosition position, TLType type);

    /**Removes a TrafficLight from Registration
     * @param tlId The targeted TrafficLight Id.
     * @return true if the TrafficLight was deleted.
     */
    boolean removeTL(int tlId);

    /**Executes a User-caused Request to change a TrafficLight color to an arbitrary color.
     * @param tlId The targeted TrafficLight Id.
     * @param color The target Color
     * @return The changed TrafficLight.
     */
    TrafficLight changeToGenericColorOnManagerRequest(int tlId, TLColor color);


    /**Executes a EV-Sensor-caused Green-Light-Request.
     * @param tlId The targeted TrafficLight Id.
     * @return true if the Request was successful.
     */
    boolean changeToGreenOnEVRequest(int tlId);

    /**Returns the mode of a TrafficLight Group.
     * @param groupId The targeted Group Id.
     * @return The Mode of the group.
     */
    TLMode getGroupMode(int groupId);

    /**Switches the Group Mode to a new Mode.
     * @param group The targeted Group Id.
     * @param newMode The new Mode.
     * @return The mode that was switched to.
     */
    TLMode switchGroupMode(int group, TLMode newMode);
}
