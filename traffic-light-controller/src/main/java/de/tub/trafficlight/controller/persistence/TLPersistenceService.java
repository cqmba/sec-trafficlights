package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TLMode;
import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The Service provides methods which mock the physical Interface
 * to the Traffic Lights and allows storing and altering of their states.
 */
public interface TLPersistenceService {

    /**Creates a new Instance of the Persistence Service
     * @return A new Instance
     */
    static TLPersistenceService getInstance(){
        return new TLPersistenceServiceImpl();
    }

    /**Returns a filtered List of the Registered Traffic Lights
     * @param p A Predicate
     * @return
     */
    List<TrafficLight> getFilteredTrafficLights(Predicate<TrafficLight> p);

    /**Returns a list of all registered TrafficLights.
     * @return
     */
    List<TrafficLight> getAllTrafficLights();

    /**Returns an optional TrafficLight, depending on if it exits for the given Id.
     * @param id The targeted TrafficLight Id.
     * @return
     */
    Optional<TrafficLight> getTrafficLight(int id);

    /**Registers a new TrafficLight.
     * @param tl The new TrafficLight
     * @return
     */
    TrafficLight addTrafficLight(TrafficLight tl);

    /**Unregisters a TrafficLights.
     * @param id The targeted TrafficLight Id.
     * @return true if successfully deleted.
     */
    boolean removeTrafficLight(int id);

    /**Changes a given TrafficLight.
     * @param id The targeted TrafficLight Id.
     * @param tl The targeted TrafficLight.
     * @return true if successfull
     */
    boolean updateTrafficLight(int id, TrafficLight tl);

    /**Updates a list of TrafficLights
     * @param tlList the list which shall be updated.
     */
    void updateTrafficLightList(List<TrafficLight> tlList);

    /**Registers a list of TrafficLights.
     * @param tlList The list of TrafficLights that shall be registered.
     * @return
     */
    List<TrafficLight> addTrafficLightList(List<TrafficLight> tlList);

    /**Changes the Mode of all freely added TrafficLights.
     * @param newMode The new Mode.
     * @return The updated Mode.
     */
    TLMode switchModeOfFreeTLs(TLMode newMode);

    /**Gets the current Mode of the free TrafficLights.
     * @return The current Mode of the free TrafficLights.
     */
    TLMode getFreeTLMode();

    void deletePreviousEntries();
}
