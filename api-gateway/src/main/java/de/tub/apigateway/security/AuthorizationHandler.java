package de.tub.apigateway.security;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import java.util.HashSet;
import java.util.Set;

/**
 * Handler Class that authenticates the Client based on Username and Looks up the Realm Roles by leveraging the access Token
 */
public class AuthorizationHandler {

    private static final Logger logger = LogManager.getLogger(AuthorizationHandler.class);

    /**Authenticates the Request based on the information within the token of the Request and logs identifying information.
     * @param routingContext RoutingContext of the current Request being handled
     * @throws AuthenticationException when a Request cant be authenticated
     */
    public static void authenticateAndLogRequest(RoutingContext routingContext) throws AuthenticationException{
        logger.debug("Source IP " + routingContext.request().remoteAddress() +" requests resource " +routingContext.request().absoluteURI());
        JsonObject userJson = routingContext.user().principal();
        if (userJson.containsKey("username")){
            logger.info("User " + userJson.getString("username") + " authenticated");
        }
        if (userJson.containsKey("access_token")){
            Set<String> roles = getRolesFromToken(userJson);
            if (!roles.isEmpty()){
                logger.info("Associated Roles of User " + String.join(",", roles));
                return;
            }
        } else {
            logger.debug("User Identity and/or Role mappings could not be validated");
            throw new AuthenticationException("User Identity and/or Role mappings could not be validated");
        }
    }

    /**Retrieves the realm roles from the Access Token.
     * @param principal The JsonObject of the User responsible for the Request
     * @return Set of associated Roles to the User
     * @throws AuthenticationException when the Client has no valid Realm Roles or cannot be verified.
     */
    public static Set<String> getRolesFromToken(JsonObject principal) throws AuthenticationException{
        try {
            String tokenStr = principal.getString("access_token");
            AccessToken token = TokenVerifier.create(tokenStr, AccessToken.class).getToken();
            Set<String> roles = token.getRealmAccess().getRoles();
            return roles;
        } catch (VerificationException | NullPointerException e) {
            logger.info("Client could not be verified");
            throw new AuthenticationException("Client could not be verified");
        }
    }

    /**Authorizes the current client by looking up his realm roles and comparing them to a set of accepted roles.
     * @param routingContext the current RoutingContext.
     * @param acceptedRoles The Set of accepted Roles.
     * @return true if the client is authorized to access the resource
     */
    public static boolean isAuthorized(RoutingContext routingContext, Set<String> acceptedRoles) {
        try {
            Set<String> actualRoles = getRolesFromToken(routingContext.user().principal());
            for (String role : acceptedRoles){
                if(actualRoles.contains(role)){
                    return true;
                }
            }
            logger.info("Client Token does not contain accepted Roles");
            return false;
        } catch (AuthenticationException e) {
            return false;
        }
    }
}
