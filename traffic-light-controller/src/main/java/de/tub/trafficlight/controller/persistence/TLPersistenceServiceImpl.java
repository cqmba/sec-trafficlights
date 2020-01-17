package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TLPersistenceServiceImpl implements TLPersistenceService {

    private Map<Integer, TrafficLight> tlList;

    public TLPersistenceServiceImpl(){
        tlList = new HashMap<>();
    }

    @Override
    public List<TrafficLight> getFilteredTrafficLights(Predicate<TrafficLight> p) {
        return tlList.values().stream().filter(p).collect(Collectors.toList());
    }

    public List<TrafficLight> getAllTrafficLights() {
        return tlList.values().stream().collect(Collectors.toList());
    }

    @Override
    public Optional<TrafficLight> getTrafficLight(int id) {
        return Optional.ofNullable(tlList.get(id));
    }

    @Override
    public TrafficLight addTrafficLight(TrafficLight tl) {
        tlList.put(tl.getId(), tl);
        return tl;
    }

    @Override
    public boolean removeTrafficLight(int id) {
        TrafficLight tl = tlList.remove(id);
        if (tl != null) return true;
        else return false;
    }

    @Override
    public boolean updateTrafficLight(int id, TrafficLight tl) {
        if (getTrafficLight(id).isPresent()){
            TrafficLight tlUpdated = tlList.replace(id, tl);
            if (tlUpdated != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateTrafficLightList(List<TrafficLight> tlList) {
        //TODO handle update as a single transaction and revert possibly
        try {
            for (TrafficLight tl : tlList) {
                if (!updateTrafficLight(tl.getId(), tl)) {
                    throw new Exception("Error: Failed to update TL");
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
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
}
