package de.tub.trafficlight.controller.security;

import de.tub.trafficlight.controller.exception.AuthenticationException;
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

    public static void authenticateAndLogUser(RoutingContext routingContext) throws AuthenticationException {
        MultiMap params = routingContext.request().params();
        String tokenStr = params.get("token");
        try {
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            LogManager.getLogger().info("User: "+token.getPreferredUsername()+ " requested Resource " + routingContext.request().path());
        } catch (VerificationException | NullPointerException e) {
            LogManager.getLogger().info("Client has no username associated");
            throw new AuthenticationException("Client has no username associated");
        }
    }

    public static Set<String> getRolesFromToken(String tokenStr) {
        try {
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            return token.getRealmAccess().getRoles();
        } catch (VerificationException | NullPointerException e) {
            LogManager.getLogger().info("Client could not be verified");
            return new HashSet<>();
        }
    }
}
