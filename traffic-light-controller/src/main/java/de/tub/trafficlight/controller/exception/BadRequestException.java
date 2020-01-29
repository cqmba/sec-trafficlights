package de.tub.trafficlight.controller.exception;

public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super("JSON or Path parameters of Request could not be mapped to a valid Request" + message);
    }
}
