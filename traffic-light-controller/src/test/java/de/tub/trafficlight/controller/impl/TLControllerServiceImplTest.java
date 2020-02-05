package de.tub.trafficlight.controller.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.TLControllerVerticle;
import de.tub.trafficlight.controller.entity.TLColor;
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

import static org.junit.jupiter.api.Assertions.*;

@RunWith(VertxUnitRunner.class)
public class TLControllerServiceImplTest {

    private TLControllerService service;


    @Before
    public void setUp(TestContext context) {
        service = new TLControllerServiceImpl(Vertx.vertx());
    }

    @Test
    public void testStateMachineInitialization(TestContext context){
        for (TrafficLight tl : service.getTLList()){

            if (tl.getPosition().isMain()){
                context.assertTrue(tl.getColor().equals(TLColor.GREEN));
            } else context.assertTrue(tl.getColor().equals(TLColor.RED));
        }
    }



}