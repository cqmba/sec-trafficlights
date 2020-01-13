package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.entity.TLIntersectionMatrix;
import de.tub.trafficlight.controller.entity.TLPosition;
import de.tub.trafficlight.controller.entity.TrafficLight;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;
import de.tub.trafficlight.controller.persistence.TLPersistenceServiceImpl;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Optional;

public class TLControllerServiceImpl implements TLControllerService {

    private TLPersistenceService persistence;
    private TLIntersectionMatrix intersection;
    private Vertx vertx;

    public TLControllerServiceImpl(Vertx vertx){
        persistence = new TLPersistenceServiceImpl();
        intersection = new TLIntersectionMatrix();
        this.vertx = vertx;
        startSchedule();
    }

    private void startSchedule() {
        List<TrafficLight> trafficLights = intersection.getTLList();
        try {
            if (trafficLights.size() != persistence.addTrafficLightList(trafficLights).size()) {
                throw new Exception("Error: Failed to initialize Intersection and TL State");
            }
        }catch (Exception ex){
            //TODO handle
        }
        vertx.setTimer(25000, event -> {
            intersection.doTransition(false, false);
            persistence.updateTrafficLightList(intersection.getTLList());
            System.out.println("Waiting " + intersection.getNextTransitionTimeMs() +" ms");
            timeNextTransition(vertx, intersection.getNextTransitionTimeMs());
        });
        /*vertx.setPeriodic(5000, event -> {
            intersection.doTransition(false, false);
            persistence.updateTrafficLightList(intersection.getTLList());
        });*/
    }

    private void timeNextTransition(Vertx vertx, int time){
        vertx.setTimer(time, event -> {
            intersection.doTransition(false, false);
            persistence.updateTrafficLightList(intersection.getTLList());
            System.out.println("Waiting " + intersection.getNextTransitionTimeMs() +" ms");
            timeNextTransition(vertx, intersection.getNextTransitionTimeMs());
        });
    }
    
    @Override
    public Optional<TrafficLight> getSingleTLState(int tlId) {
        return persistence.getTrafficLight(tlId);
    }

    @Override
    public List<TrafficLight> getTLList() {
        try{
            return persistence.getAllTrafficLights();
        }catch (Exception ex){
            //TODO ErrorHandling
            return null;
        }
    }

    @Override
    public TrafficLight addTL(TLPosition position) {
        return persistence.addTrafficLight(new TrafficLight(position));
    }

    @Override
    public boolean removeTL(int tlId) {
        return persistence.removeTrafficLight(tlId);
    }

    @Override
    public boolean updateTL(int tlId, TrafficLight tl) {
        return persistence.updateTrafficLight(tlId, tl);
    }

    //TODO still need Helper Methods?
    private static TLPosition fromString(String name) {
        return getEnumFromString(TLPosition.class, name);
    }

    private static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if( c != null && string != null ) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            } catch(IllegalArgumentException ex) {
            }
        }
        return null;
    }
}
