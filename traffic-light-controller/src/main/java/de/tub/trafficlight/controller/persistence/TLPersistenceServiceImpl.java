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

    private static final Logger logger = LogManager.getLogger(TLPersistenceServiceImpl.class);

    private Map<Integer, TrafficLight> tlRepo;
    private TLMode freeTLMode;

    public TLPersistenceServiceImpl(){
        tlRepo = new HashMap<>();
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
}
