package de.tub.trafficlight.controller.entity;

import de.tub.trafficlight.controller.schedule.TLSchedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TLIntersectionMatrix {

    private TrafficLight main_west;
    private TrafficLight main_east;
    private TrafficLight side_north;
    private TrafficLight side_south;
    private TrafficLight ped1_east;
    private TrafficLight ped1_south;
    private TrafficLight ped2_west;
    private TrafficLight ped2_south;
    private TrafficLight ped3_north;
    private TrafficLight ped3_west;
    private TrafficLight ped4_north;
    private TrafficLight ped4_east;

    private final TLPosition pos_mw = TLPosition.MAIN_ROAD_WEST;
    private final TLPosition pos_me = TLPosition.MAIN_ROAD_EAST;
    private final TLPosition pos_sn = TLPosition.SIDE_ROAD_NORTH;
    private final TLPosition pos_ss = TLPosition.SIDE_ROAD_SOUTH;

    private TLOperationMode mw_mode;
    private TLOperationMode me_mode;
    private TLOperationMode sn_mode;
    private TLOperationMode ss_mode;

    private TLHealth mw_health;
    private TLHealth me_health;
    private TLHealth sn_health;
    private TLHealth ss_health;

    private TLSchedule mw_schedule;
    private TLSchedule me_schedule;
    private TLSchedule sn_schedule;
    private TLSchedule ss_schedule;

    private TLState state;

    private List<TrafficLight> roadLights;
    private List<TrafficLight> pedLights;


    public TLIntersectionMatrix(){
        this.state = TLState.S0_MG_SR_PM;
        this.mw_mode = TLOperationMode.ON_NORMAL;
        this.me_mode = TLOperationMode.ON_NORMAL;
        this.sn_mode = TLOperationMode.ON_NORMAL;
        this.ss_mode = TLOperationMode.ON_NORMAL;
        this.mw_health = TLHealth.HEALTHY;
        this.me_health = TLHealth.HEALTHY;
        this.sn_health = TLHealth.HEALTHY;
        this.ss_health = TLHealth.HEALTHY;
        this.mw_schedule = new TLSchedule();
        this.me_schedule = new TLSchedule();
        this.sn_schedule = new TLSchedule();
        this.ss_schedule = new TLSchedule();

        main_west = new TrafficLight(state.getCurrentMainRoadColor(), pos_mw, mw_health, mw_mode, mw_schedule);
        main_east = new TrafficLight(state.getCurrentMainRoadColor(), pos_me, me_health, me_mode, me_schedule);
        side_north = new TrafficLight(state.getCurrentSideRoadColor(), pos_sn, sn_health, sn_mode, sn_schedule);
        side_south = new TrafficLight(state.getCurrentSideRoadColor(), pos_ss, ss_health, ss_mode, ss_schedule);
        //TODO could specify pedestrian specific stuff here
        ped1_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule);
        ped1_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule);
        ped2_south = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_ss, ss_health, ss_mode, ss_schedule);
        ped2_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule);
        ped3_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule);
        ped3_west = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_mw, mw_health, mw_mode, mw_schedule);
        ped4_east = new TrafficLight(state.getCurrentMainPedestrianColor(), pos_me, me_health, me_mode, me_schedule);
        ped4_north = new TrafficLight(state.getCurrentSidePedestrianColor(), pos_sn, sn_health, sn_mode, sn_schedule);

        pedLights = new ArrayList<>(Arrays.asList(ped1_east, ped1_south, ped2_west, ped2_south, ped3_north, ped3_west, ped4_east, ped4_north));
        roadLights= new ArrayList<>(Arrays.asList(main_west, main_east, side_north, side_south));
        printIntersection();
    }

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
        //TODO remove this
        printIntersection();
    }

    private void printIntersection(){
        System.out.println("New intersection State " + state.toString());
        System.out.println("Road Traffic Lights: ");
        for (TrafficLight light : roadLights){
            System.out.println("("+ light.getId() + ") " + light.getPosition().toString() +": "+light.getColor().toString());
        }
        System.out.println("Pedestrian Lights: ");
        for (TrafficLight light : pedLights){
            System.out.println("("+ light.getId() + ") " + light.getPosition().toString() +": "+light.getColor().toString());
        }
    }

    public List<TrafficLight> getTLList(){
        return Stream.concat(roadLights.stream(), pedLights.stream()).collect(Collectors.toList());
    }

    public int getNextTransitionTimeMs(){
        return state.nextState(false, false).getCurrentStandardTransitionTimeMs();
    }
}
