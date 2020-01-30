package de.tub.trafficlight.controller.exception;

/**
 * A Exception to that gets thrown when incoming Requests cannot be mapped to expected Objects or Values
 */
public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super("JSON or Path parameters of Request could not be mapped to a valid Request " + message);
    }
}
