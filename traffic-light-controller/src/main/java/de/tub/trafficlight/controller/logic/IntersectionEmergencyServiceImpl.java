package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.TLIncident;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the Emergency Service, where Emergency Vehicle Incidents can be added, handled and updated.
 */
public class IntersectionEmergencyServiceImpl implements IntersectionEmergencyService {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private Map<Integer, TLIncident> incidentRepo;

    private static final Logger logger = LogManager.getLogger(IntersectionEmergencyServiceImpl.class);

    public IntersectionEmergencyServiceImpl(){
        this.incidentRepo = new HashMap<>();
    }

    @Override
    public List<TLIncident> getFilteredIncidents(Predicate<TLIncident> filter) {
        return incidentRepo.values().stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public void addIncident(TLIncident incident) {
        logger.debug("New incident added to Persistence: " + incident.getPosition().toString());
        incidentRepo.put(COUNTER.getAndIncrement(), incident);
    }

    @Override
    public Optional<TLIncident> getNextUnresolvedIncident() {
        List<TLIncident> unresolved = getFilteredIncidents(filter -> filter.getState().equals(TLIncident.STATE.UNRESOLVED));
        if (unresolved.size() >= 1){
            logger.debug("New incident passed to Intersection logic");
            return Optional.ofNullable(unresolved.iterator().next());
        }
        return Optional.empty();
    }

    @Override
    public Optional<TLIncident> updateIncident(Optional<TLIncident> incident, boolean mainGreen, boolean sideGreen) {
        if (incident.isEmpty()){
            //nothing to do
            return incident;
        } else if (incident.get().getState().equals(TLIncident.STATE.RESOLVED)){
            logger.debug("Incident was already resolved");
            return Optional.empty();
        } else if (incident.get().getState().equals(TLIncident.STATE.UNRESOLVED) && mainGreen && incident.get().getPosition().isMain()){
            if (resolveSingle(incident.get()) == 1){
                logger.debug("Incident was successfully resolved "+ incident.get().getPosition().toString());
                return Optional.empty();
            }
        } else if (incident.get().getState().equals(TLIncident.STATE.UNRESOLVED) && sideGreen && incident.get().getPosition().isSide()){
            if (resolveSingle(incident.get()) == 1){
                logger.debug("Incident was successfully resolved " + incident.get().getPosition().toString());
                return Optional.empty();
            }
        } else {
            logger.debug("Incident not resolved yet");
            return incident;
        }
        return Optional.empty();
    }

    @Override
    public void resolveSideRoadIncidents() {
        List<TLIncident> toResolve = getFilteredIncidents(filter -> filter.getState().equals(TLIncident.STATE.UNRESOLVED) && filter.getPosition().isSide());
        resolveList(toResolve);
    }

    @Override
    public void resolveMainRoadIncidents(){
        List<TLIncident> toResolve = getFilteredIncidents(filter -> filter.getState().equals(TLIncident.STATE.UNRESOLVED) && filter.getPosition().isMain());
        resolveList(toResolve);
    }

    private int resolveSingle(TLIncident incident){
        int resolved = 0;
        for (Integer key : keys(incidentRepo, incident).collect(Collectors.toList())){
            incident.setState(TLIncident.STATE.RESOLVED);
            incidentRepo.replace(key, incident);
            logger.debug("Incident resolved " + incident.getPosition().toString());
            resolved ++;
        }
        if (resolved > 1){
            logger.debug("Multiple Incidents in the Repo matched the same Object, but have been resolved");
        }
        return resolved;
    }

    private void resolveList(List<TLIncident> toResolve) {
        for (TLIncident incident : toResolve){
            resolveSingle(incident);
        }
    }

    private  <K, V> Stream<K> keys(Map<K, V> map, V value) {
        return map
                .entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey);
    }
}
