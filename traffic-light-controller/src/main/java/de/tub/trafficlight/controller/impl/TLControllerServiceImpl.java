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
    private boolean interrupt;

    private static int counter;

    private static final Logger logger = LogManager.getLogger(TLControllerServiceImpl.class);
    private static final int MAIN_INTERSECTION_GROUP = 1;
    public TLControllerServiceImpl(Vertx vertx){
        persistence = TLPersistenceService.getInstance();
        intersection = TLIntersectionLogicService.getInstance(MAIN_INTERSECTION_GROUP, persistence);
        this.vertx = vertx;
        this.interrupt = false;
        counter = 0;
        startSchedule();
    }

    private void startSchedule() {
        List<TrafficLight> trafficLights = intersection.getTLList();
        try {
            if (trafficLights.size() != persistence.addTrafficLightList(trafficLights).size()) {
                throw new Exception("Error: Failed to initialize Intersection and TL State");
            }
            logger.debug("Intersection successfully initialized with state " + intersection.getCurrentIntersectionState());
        }catch (Exception ex){
            //TODO handle
        }
        vertx.setTimer(25000, event -> {
            intersection.doTransition();
            persistence.updateTrafficLightList(intersection.getTLList());
            logger.debug("Waiting for next Transition "+ intersection.getNextTransitionTimeMs() + "ms");
            timePeriodic(intersection.getNextTransitionTimeMs());
        });
    }

    private void timePeriodic(int time){
        counter = 0;
        final int interval = 1000;
        vertx.setPeriodic(interval, event ->{
            counter += interval;
            if (interrupt || counter >= time){
                this.interrupt = false;
                counter = 0;
                vertx.cancelTimer(event);
                executeTransitionAndSetNewTimer();
            }
        });
    }

    private void executeTransitionAndSetNewTimer(){
        intersection.doTransition();
        persistence.updateTrafficLightList(intersection.getTLList());
        int transitionMs = intersection.getNextTransitionTimeMs();
        logger.debug("Waiting for "+ transitionMs + "ms");
        timePeriodic(transitionMs);
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
    public TrafficLight addTL(int id, TLColor color, TLPosition position, TLType type,  int groupId) {
        return persistence.addTrafficLight(new TrafficLight(id, color, position, type, groupId));
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
        //TODO get responsible party
        String user = "someone";
        if(getSingleTLState(tlId).isPresent()){
            TrafficLight tl = getSingleTLState(tlId).get();
            if (TLType.VEHICLE.equals(tl.getType())){
                persistence.addIncident(new TLIncident(tl.getPosition(), user, tl.getId(), TLIncident.STATE.UNRESOLVED));
                this.interrupt = true;
                logger.debug("New Incident added: " + user + " requested GREEN on TL " + tl.getId() + " for Position " + tl.getPosition().toString());
                return true;
            } else {
                //EV can only request for Vehicle
                logger.debug("Green Requested for Pedestrian TL");
                return false;
            }
        }
        return false;
    }
/*
    private boolean haveToChangeSchedule(TrafficLight tl){
        if (tl.getPosition().equals(TLPosition.MAIN_ROAD_EAST) || tl.getPosition().equals(TLPosition.MAIN_ROAD_WEST) ){
            if (intersection.getCurrentIntersectionState().equals(TLState.S0_MG_SR_PM)){
                return false;
            }
        } else if (tl.getPosition().equals(TLPosition.SIDE_ROAD_NORTH)  || tl.getPosition().equals(TLPosition.SIDE_ROAD_SOUTH) ){
            if (intersection.getCurrentIntersectionState().equals(TLState.S3_MR_SG_PS){
                return false;
            }
        }
        return true;
    }

    private void handleIncidents(){
        while (evIncidents.size() >= 1){
            //resolve immediate requests for green on green
            else {
                //TODO safe increasing
                TLPosition tlPosition = evIncidents.get(0);
                this.nextIncident = Promise.promise();
                nextIncident.handle();
                //TODO wait out current timer and inject into following
                if(tlPosition.equals(TLPosition.MAIN_ROAD_EAST) || tlPosition.equals(TLPosition.MAIN_ROAD_WEST)){
                    intersection.doTransition(true, false);
                    logger.debug("Executed Emergency Green Light for Emergency Vehicle triggered by Sensor on " + tl.getPosition().toString());
                    logger.debug("New Intersection State " + intersection.getCurrentIntersectionState());
                    return true;
                } else if (tlPosition.equals(TLPosition.SIDE_ROAD_NORTH) || tlPosition.equals(TLPosition.SIDE_ROAD_SOUTH)){
                    intersection.doTransition(false, true);
                    return true;
                }
            }

        }
    }*/
}
