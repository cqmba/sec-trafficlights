package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.TLControllerVerticle;
import de.tub.trafficlight.controller.entity.TLColor;
import de.tub.trafficlight.controller.entity.TLMode;
import de.tub.trafficlight.controller.entity.TLType;
import de.tub.trafficlight.controller.entity.TrafficLight;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(VertxUnitRunner.class)
public class TLControllerServiceImplTest {

    private TLControllerServiceImpl service;


    @Before
    public void setUp(TestContext context) {
        service = new TLControllerServiceImpl(Vertx.vertx());
    }

    @After
    public void tearDown(TestContext context){
        service.resetIntersectionState();
        TrafficLight.resetCounter();
    }

    @Test
    public void testStateMachineInitialization(TestContext context){
        for (TrafficLight tl : service.getTLList()){

            if (tl.getPosition().isMain()){
                context.assertTrue(tl.getColor().equals(TLColor.GREEN));
            } else context.assertTrue(tl.getColor().equals(TLColor.RED));
        }
    }

    @Test
    public void testIfNextStateIsActiveAfterTransition(TestContext context){
        service.doTransition();
        for (TrafficLight tl : service.getTLList()){
            if(tl.getType().equals(TLType.VEHICLE)){
                if (tl.getPosition().isMain()){
                    context.assertTrue(tl.getColor().equals(TLColor.YELLOW));
                } else context.assertTrue(tl.getColor().equals(TLColor.RED));
            }
        }
    }

    @Test
    public void testThatPedestrianLightsNeverHaveGreenWhenCrossingVehiclesHaveGreen(TestContext context){
        for (int i=0; i<=5; i++){
            //list all pedestrian TrafficLights
            for (TrafficLight pedLight : service.getTLList().stream().filter(tl -> tl.getType().equals(TLType.PEDESTRIAN)).collect(Collectors.toList())){
                //ped Light is green on main road, will fail if any side road Vehicle traffic lights are green
                if (pedLight.getColor().equals(TLColor.GREEN) && pedLight.getPosition().isMain()){
                    context.assertTrue(!service.getTLList().stream().anyMatch(
                            tl -> tl.getType().equals(TLType.VEHICLE)
                                    && tl.getPosition().isSide()
                                    && tl.getColor().equals(TLColor.GREEN)
                    ));
                }
                //ped Light is green on side road, will fail if any main road Vehicle traffic lights are green
                else if (pedLight.getColor().equals(TLColor.GREEN) && pedLight.getPosition().isSide()){
                    context.assertTrue(!service.getTLList().stream().anyMatch(
                            tl -> tl.getType().equals(TLType.VEHICLE)
                                    && tl.getPosition().isMain()
                                    && tl.getColor().equals(TLColor.GREEN)
                    ));
                }
            }
            //test all possible states
            service.doTransition();
        }
    }


    @Test
    public void testThatVehicleLightsNeverHaveGreenWhenCrossingVehiclesHaveGreen(TestContext context){
        for (int i=0; i<=5; i++){
            //list all vehicle TrafficLights
            List<TrafficLight> greenMainRoadVehicleTLs = service.getTLList().stream()
                    .filter(tl -> tl.getType().equals(TLType.VEHICLE)
                        && tl.getPosition().isMain()
                            && tl.getColor().equals(TLColor.GREEN)
                    ).collect(Collectors.toList());

            List<TrafficLight> greenSideRoadVehicleTLs = service.getTLList().stream()
                    .filter(tl -> tl.getType().equals(TLType.VEHICLE)
                            && tl.getPosition().isSide()
                            && tl.getColor().equals(TLColor.GREEN)
                    ).collect(Collectors.toList());
            if (!greenMainRoadVehicleTLs.isEmpty() && !greenSideRoadVehicleTLs.isEmpty()){
                context.fail();
            }
            //test all possible states
            service.doTransition();
        }
    }

    @Test
    public void testIfLightsAreYellowBlinkingWhenMaintenanceModeIsEnabled(TestContext context){
        TLMode expectedMode = TLMode.MAINTENANCE;
        TLMode confirmedMode = service.switchGroupMode(1, expectedMode);
        context.assertTrue(confirmedMode.equals(expectedMode));
        for (TrafficLight tl : service.getTLList()){
            context.assertTrue(tl.getColor().equals(TLColor.YELLOWBLINKING));
        }
    }

    @Test
    public void testIfLightsResumeOldStateWhenMaintenanceModeIsReset(TestContext context){
        TLMode expectedMode = TLMode.SCHEDULED;
        TLMode confirmedMode = service.switchGroupMode(1, expectedMode);
        context.assertTrue(confirmedMode.equals(expectedMode));
        for (TrafficLight tl : service.getTLList()){
            context.assertTrue(!tl.getColor().equals(TLColor.YELLOWBLINKING));
        }
    }


}