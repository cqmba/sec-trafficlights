package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLIncident;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IntersectionEmergencyService {

    static IntersectionEmergencyService getInstance(){
        return new IntersectionEmergencyServiceImpl();
    }

    List<TLIncident> getFilteredIncidents(Predicate<TLIncident> filter);

    void addIncident(TLIncident incident);

    Optional<TLIncident> getNextUnresolvedIncident();

    Optional<TLIncident> updateIncident(Optional<TLIncident> incident, boolean mainGreen, boolean sideGreen);

    void resolveSideRoadIncidents();

    void resolveMainRoadIncidents();
}
