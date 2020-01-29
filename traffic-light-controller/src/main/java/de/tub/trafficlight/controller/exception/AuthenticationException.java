package de.tub.trafficlight.controller.exception;

/**
 * An Exception that gets thrown when a client cannot be authenticated
 */
public class AuthenticationException extends Exception{
    public AuthenticationException(String message) {
        super(message);
    }
}
