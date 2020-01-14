package de.tub.trafficlight.controller.schedule;

import de.tub.trafficlight.controller.entity.TLColor;

public class TLSchedule {

    private TLColor next_color;
    private int duration;
    private TLColor entry_color;

    public TLSchedule(){
        this.duration = 5000;
        this.entry_color = TLColor.GREEN;
        this.next_color = TLColor.YELLOW;
    }

    public TLSchedule(int duration, TLColor next_color, TLColor entry_color){
        this.duration = duration;
        this.next_color = next_color;
        this.entry_color = entry_color;
    }

    public TLColor getNext_color() {
        return next_color;
    }

    public void setNext_color(TLColor next_color) {
        this.next_color = next_color;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TLColor getEntry_color() {
        return entry_color;
    }

    public void setEntry_color(TLColor entry_color) {
        this.entry_color = entry_color;
    }

}
