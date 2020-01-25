package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TLIncident;
import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface TLPersistenceService {

    static TLPersistenceService getInstance(){
        return new TLPersistenceServiceImpl();
    }

    List<TrafficLight> getFilteredTrafficLights(Predicate<TrafficLight> p);

    List<TrafficLight> getAllTrafficLights();

    Optional<TrafficLight> getTrafficLight(int id);

    TrafficLight addTrafficLight(TrafficLight tl);

    boolean removeTrafficLight(int id);

    boolean updateTrafficLight(int id, TrafficLight tl);

    void updateTrafficLightList(List<TrafficLight> tlList);

    List<TrafficLight> addTrafficLightList(List<TrafficLight> tlList);

    List<TLIncident> getFilteredIncidents(Predicate<TLIncident> filter);

    void addIncident(TLIncident incident);

    Optional<TLIncident> getNextUnresolvedIncident();

    Optional<TLIncident> updateIncident(Optional<TLIncident> incident, boolean mainGreen, boolean sideGreen);

    void resolveSideRoadIncidents();

    void resolveMainRoadIncidents();

}
