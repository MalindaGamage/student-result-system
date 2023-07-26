package verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import model.Result;
import model.Student;
import util.Constants;

public class HTTPServerVerticle extends AbstractVerticle {
    private Router router;
    private EventBus eventBus;
    @Override
    public void start(Promise<Void> startPromise) {
        // Create a router to handle HTTP requests
        router = Router.router(vertx);
        eventBus = vertx.eventBus();
        router.route().handler(BodyHandler.create()); // Enable parsing of request bodies

        // Define the route to handle saving student details (POST request)
        router.post("/students").handler(this::saveStudent);

        // Define the route to handle updating student details (PUT request)
        router.put("/students/:studentID").handler(this::updateStudent);

        // Define the route to handle deleting student details (DELETE request)
        router.delete("/students/:studentID").handler(this::deleteStudent);

        // Define the route to check pass or fail status (GET request)
        router.get("/students/:studentID/subjects/:subjectID").handler(this::checkPassOrFail);

        // Define the route to fetch and save the results (GET request)
        router.get("/students/:studentID").handler(this::fetchAndSaveResult);

        // Create an HTTP server and pass the router to handle incoming requests
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(9000)
                .onSuccess(server -> {
                    System.out.println("Server started on port 9000 successfully");
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);

    }

    private void saveStudent(RoutingContext context) {
        JsonObject studentJson = context.getBodyAsJson();
        Student student = studentJson.mapTo(Student.class);

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "save");
        eventBus.request(Constants.STUDENT_SAVE_EVENT_BUS, student, options, ar -> {
            if (ar.succeeded()) {
                context.response().setStatusCode(200).end(ar.result().body().toString());
            } else {
                context.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void updateStudent(RoutingContext context) {
        JsonObject studentJson = context.getBodyAsJson();
        int studentID = Integer.parseInt(context.pathParam("studentID"));
        Student updatedStudent = studentJson.mapTo(Student.class);
        updatedStudent.setStudentID(studentID);

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "update");
        eventBus.request(Constants.STUDENT_UPDATE_EVENT_BUS, updatedStudent, options, ar -> {
            if (ar.succeeded()) {
                context.response().setStatusCode(200).end(ar.result().body().toString());
            } else {
                context.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void deleteStudent(RoutingContext context) {
        int studentID = Integer.parseInt(context.pathParam("studentID"));

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete");
        eventBus.request(Constants.STUDENT_DELETE_EVENT_BUS, studentID, options, ar -> {
            if (ar.succeeded()) {
                context.response().setStatusCode(200).end(ar.result().body().toString());
            } else {
                context.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void checkPassOrFail(RoutingContext context) {
        int studentID = Integer.parseInt(context.pathParam("studentID"));
        int subjectID = Integer.parseInt(context.pathParam("subjectID"));

        // Fetch the result for the given student ID and subject ID
        JsonObject requestJson = new JsonObject()
                .put("studentID", studentID);

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "fetchResult");
        eventBus.request(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, requestJson, options, ar -> {
            if (ar.succeeded()) {
                JsonObject resultJson = (JsonObject) ar.result().body();
                Result result = resultJson.mapTo(Result.class);

                // Determine pass or fail status based on the score
                String passOrFailMessage;
                if (result.getScore() >= 50) {
                    passOrFailMessage = "Pass";
                } else {
                    passOrFailMessage = "Fail";
                }

                context.response().setStatusCode(200).end(passOrFailMessage);
            } else {
                context.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }

    private void fetchAndSaveResult(RoutingContext context) {
        int studentID = Integer.parseInt(context.pathParam("studentID"));

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "fetchAndSaveResult");
        eventBus.request(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, studentID, options, ar -> {
            if (ar.succeeded()) {
                context.response().setStatusCode(200).end(ar.result().body().toString());
            } else {
                context.response().setStatusCode(500).end(ar.cause().getMessage());
            }
        });
    }
}