package de.tub.apigateway.security;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import java.util.Set;

public class AuthenticationHandler {

    private static final Logger logger = LogManager.getLogger(AuthenticationHandler.class);


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

    private static Set<String> getRolesFromToken(JsonObject principal) throws AuthenticationException{
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
}
