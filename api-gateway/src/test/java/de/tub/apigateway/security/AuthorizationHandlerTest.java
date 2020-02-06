package de.tub.apigateway.security;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;

@RunWith(VertxUnitRunner.class)
public class AuthorizationHandlerTest {

    private JsonObject principal_manager;
    private final String TESTUSER = "testuser";

    @Before
    public void setUp(TestContext context) {
        principal_manager = new JsonObject().put("access_token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlRWZCejdLOTVBWUpJQXk1dnBIVU9BNDIxMjdneUtMUlE1RXJJR0duclBFIn0.eyJqdGkiOiI3ODcyOGZlZi0xZTg2LTQ0ZTItYjQ4Ni1kNjRkMGE0NzcyODUiLCJleHAiOjE1ODA3MjY3NTYsIm5iZiI6MCwiaWF0IjoxNTgwNzI2NDU2LCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4NDQzL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6Ijg3ZGU5NDcxLWVlZDYtNDQ3ZS1hOTFmLTk2YmQ1ZjRhYWNkNSIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXRsYzIiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiJhY2Q0YTUxZC1jY2M5LTRjZmItODNjMC1hYjc4ZTg4ZmI4NzMiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImV2IiwibWFuYWdlciIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJtYW5hZ2VyMiJ9.XOAwuqUkIrX67f6G6dVYPmZqjTmY8VepZl3gYPacyE0bUIbaovLkkfKLsBXFS8JyKNoOVaix3TsrFAUqjsz0V9N9nCirmtMfohgrrDZ9O3Y7HJNXxRM_syucZXilV2lknZLpl4lV3JEIiBSJs80j8oyk9pi4HJsX003DDIqW85SCKygwiw34gXur28fNmISrWKJdYCWvc9hPPbMq43ok8JKAWzvopybEhLuc5kZzdKkJdrdq99dd1r1BHc23iyHB8Aw9-qIO6tGMJV3kEYuJ26-62Z4HOZT_bqBaQv7HZ7SJPqqClxr2MowfXOk9_pE9xtK7GrCincK2IJm43l0OOQ")
                                            .put("username", TESTUSER);
    }

    @After
    public void tearDown(TestContext context) {
    }

    @Test
    public void testExtractRolesFromTokenIsNotEmpty(TestContext context) throws AuthenticationException {
        context.assertTrue(!AuthorizationHandler.getRolesFromToken(principal_manager).isEmpty());
    }

    @Test
    public void testRolesContainManager(TestContext context) throws AuthenticationException {
        context.assertTrue(AuthorizationHandler.getRolesFromToken(principal_manager).contains("manager"));
    }

    @Test
    public void testIfIsAuthorizedForGivenRoleWhenPassedAsSet(TestContext context){
        context.assertTrue(AuthorizationHandler.isAuthorized(principal_manager, new HashSet<>(Collections.singletonList("manager"))));
    }

    @Test
    public void testIfTestuserCanBeAuthenticatedThroughToken(TestContext context){
        try{
            AuthorizationHandler.authenticateAndLogRequest(principal_manager);
        }catch (AuthenticationException e){
            context.fail();
        }
    }
}

