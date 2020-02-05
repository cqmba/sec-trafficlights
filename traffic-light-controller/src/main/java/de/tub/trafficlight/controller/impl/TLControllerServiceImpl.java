package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.logic.IntersectionEmergencyService;
import de.tub.trafficlight.controller.logic.TLIntersectionLogicService;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implements the Controller Service, which handles how the Intersection States are timed and managed.
 */
public class TLControllerServiceImpl implements TLControllerService {

    private TLPersistenceService persistence;
    private TLIntersectionLogicService intersection;
    private IntersectionEmergencyService emergency;
    private Vertx vertx;
    private boolean interrupt;

    private int counter;

    private static final Logger logger = LogManager.getLogger(TLControllerServiceImpl.class);
    private static final int MAIN_INTERSECTION_GROUP = 1;

    /**Constructs a new instance and instanciates persistence, emergency and intersection Services
     * @param vertx The current vertx Instance.
     */
    public TLControllerServiceImpl(Vertx vertx){
        persistence = TLPersistenceService.getInstance();
        emergency = IntersectionEmergencyService.getInstance();
        intersection = TLIntersectionLogicService.getInstance(MAIN_INTERSECTION_GROUP, persistence, emergency);
        this.vertx = vertx;
        this.interrupt = false;
        counter = 0;
        startSchedule();
    }

    /**
     * Sets the first Timer and initializes the Intersection, then calls the periodic scheduling
     */
    private void startSchedule() {
        persistence.deletePreviousEntries();
        List<TrafficLight> trafficLights = intersection.getTLList();
        try {
            if (trafficLights.size() != persistence.addTrafficLightList(trafficLights).size()) {
                throw new RuntimeException("Error: Failed to initialize Intersection and TL State");
            }
            logger.debug("Intersection successfully initialized with state " + intersection.getCurrentIntersectionState());
        }catch (RuntimeException ex){
            logger.error(ex.getMessage());
        }
        timePeriodic(25000);
    }


    /**Checks periodically, if interrupts need to be handled or the timer is met, then handles it
     * according to the Intersection Mode.
     * @param time Time after wich the timed event executes in ms
     */
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
                    case MAINTENANCE:{
                        logger.debug("MODE MAINTENANCE: Checking every 2 seconds if still in maintenance");
                        timePeriodic(5000);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Makes an Intersection Transition, updates the states in the persistence and resets the timer
     */
    private void executeTransitionAndSetNewTimer(){
        doTransition();
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
    public TrafficLight addTL(int id, TLColor color, TLPosition position, TLType type) {
        return persistence.addTrafficLight(new TrafficLight(id, color, position, type, 2));
    }

    @Override
    public boolean removeTL(int tlId) {
        return persistence.removeTrafficLight(tlId);
    }

    @Override
    public TrafficLight changeToGenericColorOnManagerRequest(int tlId, TLColor color) {
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
    public boolean changeToGreenOnEVRequest(int tlId) {
        if(getSingleTLState(tlId).isPresent()){
            TrafficLight tl = getSingleTLState(tlId).get();
            if (TLType.VEHICLE.equals(tl.getType())){
                emergency.addIncident(new TLIncident(tl.getPosition(), TLIncident.STATE.UNRESOLVED));
                this.interrupt = true;
                logger.debug("New Incident added: EV requested GREEN on TL " + tl.getId() + " for Position " + tl.getPosition().toString());
                return true;
            } else {
                //EV can only request for Vehicle
                logger.debug("Green Requested for Pedestrian TL");
                return false;
            }
        }
        return false;
    }

    @Override
    public TLMode getGroupMode(int groupId) {
        if (groupId == 1){
            return intersection.getMode();
        } else {
            return persistence.getFreeTLMode();
        }
    }

    @Override
    public TLMode switchGroupMode(int groupId, TLMode newMode) {
        if(groupId == 1){
            TLMode confirmedMode = intersection.setMode(newMode);
            if (newMode.equals(TLMode.MAINTENANCE)){
                List<TrafficLight> toUpdate = new ArrayList<>();
                for (TrafficLight tl: intersection.getTLList()){
                    tl.setColor(TLColor.YELLOWBLINKING);
                }
                persistence.updateTrafficLightList(toUpdate);
            }
            interrupt = true;
            return confirmedMode;
        }else {
            return persistence.switchModeOfFreeTLs(newMode);
        }
    }

    protected void resetIntersectionState() {
        intersection.resetIntersectionState();
        interrupt = true;
    }

    protected void doTransition(){
        intersection.doTransition();
        persistence.updateTrafficLightList(intersection.getTLList());
    }
}
