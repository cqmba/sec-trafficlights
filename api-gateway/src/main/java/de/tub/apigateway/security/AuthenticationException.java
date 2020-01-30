package de.tub.apigateway.security;

public class AuthenticationException extends Exception{
    public AuthenticationException(String message) {
        super(message);
    }
}
