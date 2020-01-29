package de.tub.trafficlight.controller.security;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import java.util.HashSet;
import java.util.Set;

public class AuthorizationHandler {

    public static boolean isAuthorized(RoutingContext routingContext, Set<String> acceptedRoles) {
        MultiMap params = routingContext.request().params();
        String token = params.get("token");
        Set<String> actualRoles = getRolesFromToken(token);
        for (String role : acceptedRoles){
            if(actualRoles.contains(role)){
                return true;
            }
        }
        LogManager.getLogger().info("Client Token does not contain accepted Roles");
        return false;
    }



    public static String getUsername(RoutingContext routingContext){
        MultiMap params = routingContext.request().params();
        String tokenStr = params.get("token");
        try {
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            return token.getPreferredUsername();
        } catch (VerificationException | NullPointerException e) {
            LogManager.getLogger().info("Client has no username associated");
            return "";
        }
    }

    public static Set<String> getRolesFromToken(String tokenStr) {
        try {
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            Set<String> roles = token.getRealmAccess().getRoles();
            return roles;
        } catch (VerificationException | NullPointerException e) {
            LogManager.getLogger().info("Client could not be verified");
            return new HashSet<>();
        }
    }
}
