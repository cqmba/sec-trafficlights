package de.tub.trafficlight.controller.schedule;

public interface TLSchedulingService {

    TLSchedule getSchedule(int id);

    boolean setSchedule(int id, TLSchedule schedule);
}
