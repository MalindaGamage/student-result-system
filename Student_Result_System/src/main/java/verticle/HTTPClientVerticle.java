package verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import util.Constants;

public class HTTPClientVerticle extends AbstractVerticle {
    private EventBus eventBus;
    private WebClient webClient;

    @Override
    public void start(Promise<Void> startPromise) {
        eventBus = vertx.eventBus();
        webClient = WebClient.create(vertx);

        eventBus.consumer(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, this::fetchResultFromThirdParty);
        startPromise.complete();
    }

    private void fetchResultFromThirdParty(io.vertx.core.eventbus.Message<Integer> message) {
        int studentID = message.body();
        String url = "http://third-party-api.com/getStudentResult?studentID=" + studentID;

        // Perform the HTTP GET request using WebClient
        webClient.getAbs(url)
                .send()
                .onSuccess(this::handleResponse)
                .onFailure(this::handleError);
    }

    private void handleResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() != 200) {
            handleError(new Throwable("Failed to fetch student result from third party API"));
            return;
        }

        Buffer responseBody = response.body();
        JsonObject resultJson = responseBody.toJsonObject();
        eventBus.send(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, resultJson);
    }

    private void handleError(Throwable throwable) {
        eventBus.send(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, new JsonObject().put("error", throwable.getMessage()));
    }
}
