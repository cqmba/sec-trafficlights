package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TLIncident;
import de.tub.trafficlight.controller.entity.TrafficLight;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TLPersistenceServiceImpl implements TLPersistenceService {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Logger logger = LogManager.getLogger(TLPersistenceServiceImpl.class);

    private Map<Integer, TrafficLight> tlRepo;
    private Map<Integer, TLIncident> incidentRepo;

    public TLPersistenceServiceImpl(){
        tlRepo = new HashMap<>();
        incidentRepo = new HashMap<>();
    }

    @Override
    public List<TrafficLight> getFilteredTrafficLights(Predicate<TrafficLight> p) {
        return tlRepo.values().stream().filter(p).collect(Collectors.toList());
    }

    public List<TrafficLight> getAllTrafficLights() {
        return new ArrayList<>(tlRepo.values());
    }

    @Override
    public Optional<TrafficLight> getTrafficLight(int id) {
        return Optional.ofNullable(tlRepo.get(id));
    }

    @Override
    public TrafficLight addTrafficLight(TrafficLight tl) {
        tlRepo.put(tl.getId(), tl);
        return tl;
    }

    @Override
    public boolean removeTrafficLight(int id) {
        TrafficLight tl = tlRepo.remove(id);
        return tl != null;
    }

    @Override
    public boolean updateTrafficLight(int id, TrafficLight tl) {
        if (getTrafficLight(id).isPresent()){
            TrafficLight tlUpdated = tlRepo.replace(id, tl);
            return tlUpdated != null;
        }
        return false;
    }

    @Override
    public boolean updateTrafficLightList(List<TrafficLight> tlList) {
        for (TrafficLight tl : tlList) {
            if (!updateTrafficLight(tl.getId(), tl)) {
                logger.error("Error: Failed to update TL");
            }
        }
        return true;
    }

    @Override
    public List<TrafficLight> addTrafficLightList(List<TrafficLight> tlList) {
        List<TrafficLight> addedList = new ArrayList<>();
        try {
            for (TrafficLight tl : tlList) {
                if (!tl.equals(addTrafficLight(tl))) {
                    throw new Exception("Error: Failed to add TL");
                }else {
                    addedList.add(tl);
                }
            }
            return addedList;
        } catch (Exception e) {
            return addedList;
        }
    }

    @Override
    public List<TLIncident> getFilteredIncidents(Predicate<TLIncident> filter) {
        return incidentRepo.values().stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public TLIncident addIncident(TLIncident incident) {
        logger.debug("New incident added to Persistence: " + incident.getPosition().toString());
        incidentRepo.put(COUNTER.getAndIncrement(), incident);
        return incident;
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
    public boolean resolveSideRoadIncidents() {
        List<TLIncident> toResolve = getFilteredIncidents(filter -> filter.getState().equals(TLIncident.STATE.UNRESOLVED) && filter.getPosition().isSide());
        return resolveList(toResolve);
    }

    @Override
    public boolean resolveMainRoadIncidents(){
        List<TLIncident> toResolve = getFilteredIncidents(filter -> filter.getState().equals(TLIncident.STATE.UNRESOLVED) && filter.getPosition().isMain());
        return resolveList(toResolve);
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

    private boolean resolveList(List<TLIncident> toResolve) {
        if (toResolve.size() >= 1){
            int resolved = 0;
            for (TLIncident incident : toResolve){
                resolved += resolveSingle(incident);
            }
            return toResolve.size() == resolved;
        } else {
            return true;
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
