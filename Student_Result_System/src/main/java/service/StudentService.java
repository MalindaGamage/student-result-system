package service;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import model.Student;
import util.Constants;
public class StudentService {
    private final EventBus eventBus;
    public StudentService(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    public void saveStudent(Student student) {
        eventBus.send(Constants.STUDENT_SAVE_EVENT_BUS, student.toJson());
    }
    public void updateStudent(Student student) {
        eventBus.send(Constants.STUDENT_UPDATE_EVENT_BUS, student.toJson());
    }

    public void deleteStudent(int studentID) {
        eventBus.send(Constants.STUDENT_DELETE_EVENT_BUS, studentID);
    }

    public void fetchStudentResult(int studentID) {
        JsonObject requestJson = new JsonObject().put("studentID", studentID);
        eventBus.send(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, requestJson);
    }

}