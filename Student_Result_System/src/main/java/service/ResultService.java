package service;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import model.Result;
import util.Constants;

public class ResultService {
    private final EventBus eventBus;

    public ResultService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void saveResult(Result result) {
        eventBus.send(Constants.RESULT_SAVE_EVENT_BUS, result.toJson());
    }

    public void updateResult(Result result) {
        eventBus.send(Constants.RESULT_UPDATE_EVENT_BUS, result.toJson());
    }

    public void deleteResult(int studentID) {
        eventBus.send(Constants.RESULT_DELETE_EVENT_BUS, studentID);
    }

    public void fetchResult(int studentID) {
        JsonObject requestJson = new JsonObject().put("studentID", studentID);
        eventBus.send(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, requestJson);
    }
}
