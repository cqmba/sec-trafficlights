package de.tub.trafficlight.controller.logic;

import de.tub.trafficlight.controller.entity.*;
import de.tub.trafficlight.controller.schedule.TLSchedule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TLIntersectionLogicServiceImpl implements TLIntersectionLogicService{

    private static final Logger logger = LogManager.getLogger(TLIntersectionLogicServiceImpl.class);

    private TrafficLight main_west, main_east, side_north, side_south, ped1_east, ped1_south, ped2_west, ped2_south, ped3_north, ped3_west, ped4_north, ped4_east;

    private final TLPosition pos_mw = TLPosition.MAIN_ROAD_WEST;
    private final TLPosition pos_me = TLPosition.MAIN_ROAD_EAST;
    private final TLPosition pos_sn = TLPosition.SIDE_ROAD_NORTH;
    private final TLPosition pos_ss = TLPosition.SIDE_ROAD_SOUTH;

    private TLMode mw_mode, me_mode, sn_mode, ss_mode;

    private TLHealth mw_health, me_health, sn_health, ss_health;

    private TLSchedule mw_schedule, me_schedule, sn_schedule, ss_schedule;

    private TLState state;

    private final TLType type_ped = TLType.PEDESTRIAN;
    private final TLType type_car = TLType.VEHICLE;

    private List<TrafficLight> roadLights, pedLights;

    public TLIntersectionLogicServiceImpl(){
        this.state = TLState.S0_MG_SR_PM;
        this.mw_mode = TLMode.SCHEDULED;
        this.me_mode = TLMode.SCHEDULED;
        this.sn_mode = TLMode.SCHEDULED;
        this.ss_mode = TLMode.SCHEDULED;
        this.mw_health = TLHealth.HEALTHY;
        this.me_health = TLHealth.HEALTHY;
        this.sn_health = TLHealth.HEALTHY;
        this.ss_health = TLHealth.HEALTHY;
        this.mw_schedule = new TLSchedule();
        this.me_schedule = new TLSchedule();
        this.sn_schedule = new TLSchedule();
        this.ss_schedule = new TLSchedule();

        main_west = new TrafficLight(state.getCurrentMainRoadColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_car);
        main_east = new TrafficLight(state.getCurrentMainRoadColor(), pos_me, me_health, me_mode, me_schedule, type_car);
        side_north = new TrafficLight(state.getCurrentSideRoadColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_car);
        side_south = new TrafficLight(state.getCurrentSideRoadColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_car);
        ped1_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule, type_ped);
        ped1_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_ped);
        ped2_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule, type_ped);
        ped2_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_ped);
        ped3_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_ped);
        ped3_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule, type_ped);
        ped4_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule, type_ped);
        ped4_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule, type_ped);

        pedLights = new ArrayList<>(Arrays.asList(ped1_east, ped1_south, ped2_west, ped2_south, ped3_north, ped3_west, ped4_east, ped4_north));
        roadLights= new ArrayList<>(Arrays.asList(main_west, main_east, side_north, side_south));
        printIntersection();
    }

    @Override
    public void doTransition(boolean isEmergencyMain, boolean isEmergencySide){
        this.state = state.nextState(isEmergencyMain, isEmergencySide);
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

    private void printIntersection(){
        logger.debug("New intersection State " + state.toString());
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
    public String getCurrentIntersectionState(){
        return state.toString();
    }
}
