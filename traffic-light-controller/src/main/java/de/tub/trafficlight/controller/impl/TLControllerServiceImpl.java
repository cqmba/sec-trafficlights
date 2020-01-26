package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.logic.TLIntersectionLogicService;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;
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

    private int counter;

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
            logger.error(ex.getMessage());
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
                switch (intersection.getMode()){
                    case SCHEDULED:{
                        logger.debug("MODE SCHEDULED: Normal Operation");
                        executeTransitionAndSetNewTimer();
                        break;
                    }
                    case ASSIGNED:{
                        int waitToReset = 60000;
                        logger.debug("MODE ASSIGNED: Waiting for " + waitToReset + "ms to reset to normal Operation");
                        vertx.setTimer(waitToReset, timer -> {
                            //reset mode
                            intersection.setMode(TLMode.SCHEDULED);
                            executeTransitionAndSetNewTimer();
                        });
                        break;
                    }
                    //maintenance
                    default: timePeriodic(60000);
                }
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
        return persistence.getAllTrafficLights();
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
    public TrafficLight changeColor(int tlId, TLColor color) {
        if(persistence.getTrafficLight(tlId).isPresent()){
            TrafficLight toUpdate = persistence.getTrafficLight(tlId).get();
            toUpdate.setColor(color);
            persistence.updateTrafficLight(tlId,toUpdate);
            intersection.setMode(TLMode.ASSIGNED);
            interrupt = true;
            TrafficLight updated = persistence.getTrafficLight(tlId).get();
            logger.debug("Traffic light updated " + tlId);
            return updated;
        } else {
            logger.error("Traffic Light " +tlId + " is not present");
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
}
