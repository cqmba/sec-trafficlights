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

/**
 * A Handler class which has static methods to authenticate and authorize users based on their username and Realm Role
 */
public class AuthorizationHandler {

    /**Authorizes if the Realm Roles of an Access Token contain atleast one entry of a set of accepted Roles.
     * @param routingContext The current RoutingContext
     * @param acceptedRoles If one of these Roles is included in the Token, the Request is authorized.
     * @return True if authorized
     */
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

    /**Authenticates the User by Username from the Token.
     * @param routingContext The current RoutingContext.
     * @throws AuthenticationException if the Client could not be authenticated
     */
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

    /**Retrieves the Realm Roles included in the passed Token.
     * @param tokenStr The token String.
     * @return A set of Realm Roles.
     */
    public static Set<String> getRolesFromToken(String tokenStr) {
        try {
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            return token.getRealmAccess().getRoles();
        } catch (VerificationException | NullPointerException e) {
            LogManager.getLogger().info("Client could not be verified");
            return new HashSet<>();
        }
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
}
