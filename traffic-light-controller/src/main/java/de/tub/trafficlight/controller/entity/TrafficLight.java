package de.tub.trafficlight.controller.entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An Object that models the State and Metadata of a single Traffic Light
 */
public class TrafficLight {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private int id;
    private int group;
    private TLColor color;
    private TLPosition position;
    private TLHealth health;
    private TLType type;

    public int getGroup() {
        return group;
    }

    public TLType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public TLPosition getPosition() {
        return position;
    }

    public TLHealth getHealth() {
        return health;
    }

    public  TLColor getColor() {
        return color;
    }

    public  void setColor(TLColor color) {
        this.color = color;
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
        this.type = type;
        this.group = group;
    }

    public TrafficLight(TLColor color, TLPosition position, TLHealth health, TLType type, int group) {
        this.id = COUNTER.getAndIncrement();
        this.color = color;
        this.position = position;
        this.health = health;
        this.type = type;
        this.group = group;
    }

    public static void resetCounter(){
        COUNTER.set(0);
    }
}
