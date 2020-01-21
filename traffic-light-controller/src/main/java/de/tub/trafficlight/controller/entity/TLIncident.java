package de.tub.trafficlight.controller.entity;

public class TLIncident {

    private final TLPosition position;
    private final String responsibleEntity;
    private final int tlId;
    private STATE state;

    public TLIncident(TLPosition position, String responsibleEntity, int tlId, STATE state) {
        this.position = position;
        this.responsibleEntity = responsibleEntity;
        this.tlId = tlId;
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
