package de.tub.trafficlight.controller.entity;

import de.tub.trafficlight.controller.schedule.TLSchedule;

import java.util.concurrent.atomic.AtomicInteger;

public class TrafficLight {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private int id;
    private int group;
    private TLColor color;
    private TLPosition position;
    private TLHealth health;
    private TLMode mode;
    private TLSchedule schedule;
    private TLType type;

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public TLType getType() {
        return type;
    }

    public void setType(TLType type) {
        this.type = type;
    }

    public TLSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(TLSchedule schedule) {
        this.schedule = schedule;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TLPosition getPosition() {
        return position;
    }

    public void setPosition(TLPosition position) {
        this.position = position;
    }

    public TLHealth getHealth() {
        return health;
    }

    public void setHealth(TLHealth health) {
        this.health = health;
    }

    public TLMode getMode() {
        return mode;
    }

    public void setMode(TLMode mode) {
        this.mode = mode;
    }

    public  TLColor getColor() {
        return color;
    }

    public  void setColor(TLColor color) {
        this.color = color;
    }

    public TrafficLight() {
        this.id = COUNTER.getAndIncrement();
        this.group = 2;
        this.color = TLColor.GREEN;
        this.position = TLPosition.MAIN_ROAD_EAST;
        this.health = TLHealth.HEALTHY;
        this.mode = TLMode.SCHEDULED;
        this.schedule = new TLSchedule(5000, TLColor.YELLOWRED, TLColor.GREEN);
        this.type = TLType.VEHICLE;
    }

    public TrafficLight(int id, TLColor color, TLPosition position, TLType type, int group) {
        if (id <= COUNTER.get()){
            this.id = COUNTER.getAndIncrement();
        } else {
            this.id = id;
        }
        this.color = color;
        this.position = position;
        this.health = TLHealth.HEALTHY;
        this.mode = TLMode.SCHEDULED;
        this.schedule = new TLSchedule(5000, TLColor.YELLOWRED, TLColor.GREEN);
        this.type = type;
        this.group = group;
    }

    public TrafficLight(TLColor color, TLPosition position, TLHealth health, TLMode mode, TLSchedule schedule, TLType type, int group) {
        this.id = COUNTER.getAndIncrement();
        this.color = color;
        this.position = position;
        this.health = health;
        this.mode = mode;
        this.schedule = schedule;
        this.type = type;
        this.group = group;
    }
}
