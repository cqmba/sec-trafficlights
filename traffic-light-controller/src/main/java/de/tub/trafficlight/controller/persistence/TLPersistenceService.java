package de.tub.trafficlight.controller.persistence;

import de.tub.trafficlight.controller.entity.TrafficLight;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface TLPersistenceService {

    public List<TrafficLight> getFilteredTrafficLights(Predicate<TrafficLight> p);

    public List<TrafficLight> getAllTrafficLights();

    public Optional<TrafficLight> getTrafficLight(int id);

    public TrafficLight addTrafficLight(TrafficLight tl);

    public boolean removeTrafficLight(int id);

    public boolean updateTrafficLight(int id, TrafficLight tl);

    public boolean updateTrafficLightList(List<TrafficLight> tlList);

    public List<TrafficLight> addTrafficLightList(List<TrafficLight> tlList);

}
