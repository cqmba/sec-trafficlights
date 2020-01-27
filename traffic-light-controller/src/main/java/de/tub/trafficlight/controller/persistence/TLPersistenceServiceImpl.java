package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLIncident;
import de.tub.trafficlight.controller.entity.TLMode;
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
    private TLMode freeTLMode;

    public TLPersistenceServiceImpl(){
        tlRepo = new HashMap<>();
        incidentRepo = new HashMap<>();
        freeTLMode = TLMode.SCHEDULED;
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
    public void updateTrafficLightList(List<TrafficLight> tlList) {
        for (TrafficLight tl : tlList) {
            if (!updateTrafficLight(tl.getId(), tl)) {
                logger.error("Error: Failed to update TL");
            }
        }
    }

    @Override
    public List<TrafficLight> addTrafficLightList(List<TrafficLight> tlList) {
        List<TrafficLight> addedList = new ArrayList<>();
            for (TrafficLight tl : tlList) {
                addedList.add(addIntersectionTrafficLight(tl));
            }
            return addedList;
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

    @Override
    public TLMode switchModeOfFreeTLs(TLMode newMode) {
        this.freeTLMode = newMode;
        List<TrafficLight> oldList = getFilteredTrafficLights(tl -> tl.getGroup() == 2);
        List<TrafficLight> toUpdate = new ArrayList<>();
        if (newMode.equals(TLMode.MAINTENANCE)){
            for (TrafficLight tl : oldList){
                tl.setColor(TLColor.YELLOWBLINKING);
                toUpdate.add(tl);
            }
        } else if(newMode.equals(TLMode.SCHEDULED)){
            for (TrafficLight tl : oldList){
                tl.setColor(TLColor.GREEN);
                toUpdate.add(tl);
            }
        }
        updateTrafficLightList(toUpdate);
        return freeTLMode;
    }

    @Override
    public TLMode getFreeTLMode() {
        return freeTLMode;
    }

    private TrafficLight addIntersectionTrafficLight(TrafficLight tl){
        tlRepo.put(tl.getId(), tl);
        return tl;
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
