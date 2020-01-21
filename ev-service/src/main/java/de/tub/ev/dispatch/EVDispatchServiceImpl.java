package de.tub.ev.dispatch;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EVDispatchServiceImpl implements EVDispatchService {

    private static final String BASE_TLC_API = "/api/lights/";

    private static final Logger logger = LogManager.getLogger(EVDispatchServiceImpl.class);

    private HttpClient client;
    private Vertx vertx;
    private static final boolean MOCKED_SENSOR_RESULT = false;
    private static final int MOCKED_SENSOR_ID = 1;
    private static final int SENSOR_INTERVAL = 1000;

    public EVDispatchServiceImpl(HttpClient client, Vertx vertx) {
        this.client = client;
        this.vertx = vertx;

        pullSensorPeriodically();
    }

    private void pullSensorPeriodically(){
        vertx.setPeriodic(SENSOR_INTERVAL, event -> {
            if (MOCKED_SENSOR_RESULT){
                doDispatchEV(MOCKED_SENSOR_ID);
            }
        });
    }


    @Override
    public void doDispatchEV(int id) {
        final String path = BASE_TLC_API + id +"/colors";
        JsonObject payload = new JsonObject().put("color", "GREEN").put("group", 1);
        WebClient webClient = WebClient.wrap(client);
        webClient
                .put(path)
                .sendJsonObject(payload, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        logger.debug("Successfully reported EV on sensor "+ id+", status code " + response.statusCode() + "\n " +response.bodyAsString());
                    } else {
                        logger.error("Something went wrong " + ar.cause().getMessage());
                    }
                });
    }
}
