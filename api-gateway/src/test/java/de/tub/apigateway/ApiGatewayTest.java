package de.tub.apigateway;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.report.ReportOptions;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ApiGatewayTestSuite {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(APIGatewayVerticle.class.getName(),
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testSomething(TestContext context) {
        context.assertFalse(false);
    }

    @Test
    public void testIfApiGatewayIsRespondingWithStaticHandlerAfterDeployment(TestContext context) {
        final Async async = context.async();

        WebClient.create(vertx).get(8787, "localhost", "/")
                .send( ar -> {
                    context.assertTrue(ar.result().body().toString().contains("gateway"));
                    async.complete();
                });
    }
}
