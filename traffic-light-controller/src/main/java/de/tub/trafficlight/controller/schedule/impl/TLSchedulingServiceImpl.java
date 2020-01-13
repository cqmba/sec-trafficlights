package de.tub.trafficlight.controller.schedule.impl;

import de.tub.trafficlight.controller.TLControllerService;
import de.tub.trafficlight.controller.impl.TLControllerServiceImpl;
import de.tub.trafficlight.controller.schedule.TLSchedule;
import de.tub.trafficlight.controller.schedule.TLSchedulingService;

public class TLSchedulingServiceImpl implements TLSchedulingService {

    private TLControllerService controller;

    public TLSchedulingServiceImpl(TLControllerService controller){
        this.controller = controller;
    }

    @Override
    public TLSchedule getSchedule(int id) {
        if (controller.getSingleTLState(id).isPresent()){
            return controller.getSingleTLState(id).get().getSchedule();
        } else {
            //TODO exception handling
            return null;
        }
    }

    @Override
    public boolean setSchedule(int id, TLSchedule schedule) {
        if (controller.getSingleTLState(id).isPresent()){
            controller.getSingleTLState(id).get().setSchedule(schedule);
            return true;
        }
        return false;
    }
}
