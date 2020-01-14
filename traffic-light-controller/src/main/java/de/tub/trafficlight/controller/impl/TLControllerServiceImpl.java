package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.logic.TLIntersectionLogicService;
import de.tub.trafficlight.controller.logic.TLIntersectionLogicServiceImpl;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;
import de.tub.trafficlight.controller.persistence.TLPersistenceServiceImpl;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class TLControllerServiceImpl implements TLControllerService {

    private TLPersistenceService persistence;
    private TLIntersectionLogicService intersection;
    private Vertx vertx;

    private static final Logger logger = LogManager.getLogger(TLControllerServiceImpl.class);

    public TLControllerServiceImpl(Vertx vertx){
        persistence = new TLPersistenceServiceImpl();
        intersection = new TLIntersectionLogicServiceImpl();
        this.vertx = vertx;
        startSchedule();
    }

    private void startSchedule() {
        List<TrafficLight> trafficLights = intersection.getTLList();
        try {
            if (trafficLights.size() != persistence.addTrafficLightList(trafficLights).size()) {
                throw new Exception("Error: Failed to initialize Intersection and TL State");
            }
            logger.info("Intersection successfully initialized with state " + intersection.getCurrentIntersectionState());
        }catch (Exception ex){
            //TODO handle
        }
        vertx.setTimer(25000, event -> {
            intersection.doTransition(false, false);
            persistence.updateTrafficLightList(intersection.getTLList());
            logger.info("Transition: New intersection state " + intersection.getCurrentIntersectionState());
            logger.info("Waiting for next Transition "+ intersection.getNextTransitionTimeMs() + "ms");
            timeNextTransition(vertx, intersection.getNextTransitionTimeMs());
        });
    }

    private void timeNextTransition(Vertx vertx, int time){
        vertx.setTimer(time, event -> {
            intersection.doTransition(false, false);
            persistence.updateTrafficLightList(intersection.getTLList());
            logger.info("Transition: New intersection state " + intersection.getCurrentIntersectionState());
            logger.info("Waiting for next Transition "+ intersection.getNextTransitionTimeMs() + "ms");
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

    @Override
    public TrafficLight changeColor(int tlId, TLColor color) {
        //TODO what should this do in the backend? Calculate new fitting state or just hold the state?
        if(persistence.getTrafficLight(tlId).isPresent()){
            TrafficLight tl = persistence.getTrafficLight(tlId).get();
            tl.setColor(color);
            return persistence.getTrafficLight(tlId).get();
        } else {
            //TODO handle ex
            return null;
        }
    }

    @Override
    public boolean changeToGreen(int tlId) {
        if(getSingleTLState(tlId).isPresent()){
            TrafficLight tl = getSingleTLState(tlId).get();
            if (TLType.VEHICLE.equals(tl.getType())){
                //TODO wait out current timer and inject into following
                if(tl.getPosition().equals(TLPosition.MAIN_ROAD_EAST) || tl.getPosition().equals(TLPosition.MAIN_ROAD_WEST)){
                    intersection.doTransition(true, false);
                    logger.info("Executed Emergency Green Light for Emergency Vehicle triggered by Sensor on " + tl.getPosition().toString());
                    logger.info("New Intersection State " + intersection.getCurrentIntersectionState());
                    return true;
                } else {
                    intersection.doTransition(false, true);
                    return true;
                }
            }
        }
        return false;
    }
}
