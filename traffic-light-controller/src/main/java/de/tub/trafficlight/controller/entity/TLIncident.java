package de.tub.trafficlight.controller.entity;

public class TLIncident {

    private final TLPosition position;
    private STATE state;

    public TLIncident(TLPosition position, STATE state) {
        this.position = position;
        this.state = state;
    }

    public enum STATE {
        RESOLVED,
        UNRESOLVED
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public TLPosition getPosition() {
        return position;
    }
}
