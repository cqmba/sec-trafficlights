package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.persistence.TLPersistenceService;
import de.tub.trafficlight.controller.schedule.TLSchedule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TLIntersectionLogicServiceImpl implements TLIntersectionLogicService{

    private static final Logger logger = LogManager.getLogger(TLIntersectionLogicServiceImpl.class);

    private TrafficLight main_west, main_east, side_north, side_south, ped1_east, ped1_south, ped2_west, ped2_south, ped3_north, ped3_west, ped4_north, ped4_east;

    private TLState state;
    private int groupId;

    private List<TrafficLight> roadLights, pedLights;

    private Optional<TLIncident> optional;
    private TLPersistenceService persistence;

    public TLIntersectionLogicServiceImpl(int groupId, TLPersistenceService persistence){
        this.persistence = persistence;
        this.state = TLState.S0_MG_SR_PM;
        this.groupId = groupId;
        TLMode mw_mode = TLMode.SCHEDULED;
        TLMode me_mode = TLMode.SCHEDULED;
        TLMode sn_mode = TLMode.SCHEDULED;
        TLMode ss_mode = TLMode.SCHEDULED;
        TLHealth mw_health = TLHealth.HEALTHY;
        TLHealth me_health = TLHealth.HEALTHY;
        TLHealth sn_health = TLHealth.HEALTHY;
        TLHealth ss_health = TLHealth.HEALTHY;
        TLSchedule mw_schedule = new TLSchedule();
        TLSchedule me_schedule = new TLSchedule();
        TLSchedule sn_schedule = new TLSchedule();
        TLSchedule ss_schedule = new TLSchedule();

        TLPosition pos_mw = TLPosition.MAIN_ROAD_WEST;
        TLType type_car = TLType.VEHICLE;
        main_west = new TrafficLight(state.getCurrentMainRoadColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_car, groupId);
        TLPosition pos_me = TLPosition.MAIN_ROAD_EAST;
        main_east = new TrafficLight(state.getCurrentMainRoadColor(), pos_me, me_health, me_mode, me_schedule, type_car, groupId);
        TLPosition pos_sn = TLPosition.SIDE_ROAD_NORTH;
        side_north = new TrafficLight(state.getCurrentSideRoadColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_car, groupId);
        TLPosition pos_ss = TLPosition.SIDE_ROAD_SOUTH;
        side_south = new TrafficLight(state.getCurrentSideRoadColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_car, groupId);
        TLType type_ped = TLType.PEDESTRIAN;
        ped1_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule, type_ped, groupId);
        ped1_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_ped, groupId);
        ped2_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_ped, groupId);
        ped2_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_ped, groupId);
        ped3_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_ped, groupId);
        ped3_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_ped, groupId);
        ped4_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule, type_ped, groupId);
        ped4_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_ped, groupId);

        pedLights = new ArrayList<>(Arrays.asList(ped1_east, ped1_south, ped2_west, ped2_south, ped3_north, ped3_west, ped4_east, ped4_north));
        roadLights= new ArrayList<>(Arrays.asList(main_west, main_east, side_north, side_south));
        optional = Optional.empty();
        printIntersection();
    }

    @Override
    public void doTransition(){
        updateIncidents();
        this.state = calculateNextState();

        main_west.setColor(state.getCurrentMainRoadColor());
        main_east.setColor(state.getCurrentMainRoadColor());
        side_north.setColor(state.getCurrentSideRoadColor());
        side_south.setColor(state.getCurrentSideRoadColor());
        ped1_south.setColor(state.getCurrentSidePedestrianColor());
        ped1_east.setColor(state.getCurrentMainPedestrianColor());
        ped2_west.setColor(state.getCurrentMainPedestrianColor());
        ped2_south.setColor(state.getCurrentSidePedestrianColor());
        ped3_north.setColor(state.getCurrentSidePedestrianColor());
        ped3_west.setColor(state.getCurrentMainPedestrianColor());
        ped4_north.setColor(state.getCurrentSidePedestrianColor());
        ped4_east.setColor(state.getCurrentMainPedestrianColor());
        printIntersection();
    }

    private TLState calculateNextState() {
        //handle incident
        if (optional.isPresent() && optional.get().getState().equals(TLIncident.STATE.UNRESOLVED)){
            TLState emergency_state = state.nextState(optional.get().getPosition().isMain(), optional.get().getPosition().isSide());
            TLState scheduled_state = state.nextState(false, false);
            logger.debug("Emergency Transition to "+ emergency_state.toString() + " ; Scheduled was "+ scheduled_state.toString());
            return emergency_state;
        }else {
            return state.nextState(false, false);
        }
    }

    private void updateIncidents() {
        //if past incident got green now it is resolved
        if (state.equals(TLState.S3_MR_SG_PS)){
            persistence.resolveSideRoadIncidents();
            optional = persistence.updateIncident(optional, false, true);
            logger.debug("Side Road incidents have been handled.");
        } else if(state.equals(TLState.S0_MG_SR_PM)){
            persistence.resolveMainRoadIncidents();
            optional = persistence.updateIncident(optional, true, false);
            logger.debug("Main Road incidents have been handled.");
        }
        //load new incident if necessary
        if (optional.isEmpty()){
            optional = persistence.getNextUnresolvedIncident();
        }
    }

    private void printIntersection(){
        logger.debug("New intersection State " + state.toString() + " of Intersection Group "+ groupId);
        logger.debug("Road Traffic Lights: ");
        for (TrafficLight light : roadLights){
            logger.debug("("+ light.getId() + ") " + light.getPosition().toString() +": "+light.getColor().toString());
        }
        logger.debug("Pedestrian Lights: ");
        for (TrafficLight light : pedLights){
            logger.debug("("+ light.getId() + ") " + light.getPosition().toString() +": "+light.getColor().toString());
        }
    }

    @Override
    public List<TrafficLight> getTLList(){
        return Stream.concat(roadLights.stream(), pedLights.stream()).collect(Collectors.toList());
    }

    @Override
    public int getNextTransitionTimeMs(){
        return state.getCurrentStandardTransitionTimeMs();
    }

    @Override
    public TLState getCurrentIntersectionState(){
        return state;
    }
}
