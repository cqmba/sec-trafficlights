package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLIncident;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A Service that allows adding, handling and retrieving Emergency Vehicle Incidents.
 */
public interface IntersectionEmergencyService {

    /**
     * @return A new Instance
     */
    static IntersectionEmergencyService getInstance(){
        return new IntersectionEmergencyServiceImpl();
    }

    /**Returns a filtered List of EV Incidents.
     * @param filter the given Predicate
     * @return
     */
    List<TLIncident> getFilteredIncidents(Predicate<TLIncident> filter);

    /**Registers a new EV Incident.
     * @param incident The new EV Incident.
     */
    void addIncident(TLIncident incident);

    /**Returns the next unresolved Incident.
     * @return The next EV Incident with an unresolved State
     */
    Optional<TLIncident> getNextUnresolvedIncident();

    /**Updates an optional Incident.
     * @param incident The optional Incident.
     * @param mainGreen true if the MainRoad is currently Green.
     * @param sideGreen true if the SideRoad is currently Green.
     * @return the updated optional Incident
     */
    Optional<TLIncident> updateIncident(Optional<TLIncident> incident, boolean mainGreen, boolean sideGreen);

    /**
     * Sets all Incidents with Position Side Road to Resolved.
     */
    void resolveSideRoadIncidents();

    /**
     * Sets all Incidents with Position Main Road to Resolved.
     */
    void resolveMainRoadIncidents();
}
