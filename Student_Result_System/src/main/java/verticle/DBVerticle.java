package verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import model.Result;
import model.Student;
import util.Constants;

import java.util.logging.Logger;
public class DBVerticle extends AbstractVerticle {
    private JDBCPool jdbcPool;
    private final String textFilePath = "students_details.txt";
    private final Logger log = Logger.getLogger(DBVerticle.class.getName());

    @Override
    public void start(Promise<Void> startPromise) {
        // set up the MySQL database connection options
        final JsonObject config = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/student_result_system")
                .put("datasourceName", "pool-students")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("user", "Malinda")
                .put("password", "Malinda@1997")
                .put("max_pool_size", 16);

        jdbcPool = JDBCPool.pool(vertx, config);

        vertx.eventBus().consumer(Constants.STUDENT_SAVE_EVENT_BUS, message -> {
            Student student = (Student) message.body();
            saveStudent(student)
                    .onSuccess(success -> {
                        message.reply(success);
                    }).onFailure(throwable -> {
                        log.severe(throwable.getMessage());
                        message.fail(500, throwable.getMessage());
                    });
        });

        startPromise.complete();

        vertx.eventBus().consumer(Constants.STUDENT_UPDATE_EVENT_BUS, message -> {
            Student student = (Student) message.body();
            updateStudent(student)
                    .onSuccess(success -> {
                        // After updating in the database, update the text file as well
                        updateStudentInTextFile(student)
                                .onSuccess(textFileUpdateResult -> {
                                    message.reply(success);
                                })
                                .onFailure(textFileUpdateError -> {
                                    message.fail(500, textFileUpdateError.getMessage());
                                });
                    }).onFailure(throwable -> {
                        message.fail(500, throwable.getMessage());
                    });
        });

        vertx.eventBus().consumer(Constants.STUDENT_DELETE_EVENT_BUS, message -> {
            int studentID = (int) message.body();
            deleteStudent(studentID)
                    .onSuccess(success -> {
                        // After deleting from the database, update the text file as well
                        deleteStudentFromTextFile(studentID)
                                .onSuccess(textFileDeleteResult -> {
                                    message.reply(success);
                                })
                                .onFailure(textFileDeleteError -> {
                                    message.fail(500, textFileDeleteError.getMessage());
                                });
                    }).onFailure(throwable -> {
                        message.fail(500, throwable.getMessage());
                    });
        });

        // Event bus consumer for fetching results
        vertx.eventBus().consumer(Constants.STUDENT_RESULT_FETCH_EVENT_BUS, message -> {
            JsonObject requestJson = (JsonObject) message.body();
            int studentID = requestJson.getInteger("studentID");
            fetchResult(studentID)
                    .onSuccess(message::reply)
                    .onFailure(throwable -> {
                        message.fail(500, throwable.getMessage());
                    });
        });
    }

    public Future<String> saveStudent(Student student) {
        Promise<String> promise = Promise.promise();

        Tuple tuple = Tuple.of(student.getStudentID(), student.getName(), student.getAge());

        // Save to the database asynchronously
        vertx.executeBlocking(blockingPromise -> {
            jdbcPool.preparedQuery(Constants.INSERT_STUDENT_DATA)
                    .execute(tuple)
                    .onFailure(e -> {
                        blockingPromise.fail(e.getMessage());
                    })
                    .onSuccess(rows -> {
                        blockingPromise.complete("success");
                    });
        }, false).onComplete(result -> {
            if (result.succeeded()) {
                // Successfully saved to the database, now export to the text file
                exportStudentToTextFile(student).onSuccess(exportResult -> {
                    promise.complete("success");
                }).onFailure(exportError -> {
                    promise.fail("Failed to export student to text file: " + exportError.getMessage());
                });
            } else {
                promise.fail("Failed to save student to database: " + result.cause().getMessage());
            }
        });

        return promise.future();
    }

    private Future<Void> exportStudentToTextFile(Student student) {
        Promise<Void> promise = Promise.promise();
        String studentDetails = JsonObject.mapFrom(student).encodePrettily() + System.lineSeparator();
        Buffer buffer = Buffer.buffer(studentDetails);

        vertx.fileSystem().open(textFilePath, new OpenOptions().setCreate(true).setAppend(true), openResult -> {
            if (openResult.succeeded()) {
                AsyncFile asyncFile = openResult.result();

                // Get the file size first
                asyncFile.size(sizeResult -> {
                    if (sizeResult.succeeded()) {
                        long fileSize = sizeResult.result();

                        // Write the buffer at the end of the file
                        asyncFile.write(buffer, fileSize, writeResult -> {
                            if (writeResult.succeeded()) {
                                asyncFile.close(closeResult -> {
                                    if (closeResult.succeeded()) {
                                        promise.complete();
                                    } else {
                                        promise.fail("Error closing the text file: " + closeResult.cause().getMessage());
                                    }
                                });
                            } else {
                                promise.fail("Error writing to the text file: " + writeResult.cause().getMessage());
                            }
                        });
                    } else {
                        promise.fail("Error getting the file size: " + sizeResult.cause().getMessage());
                    }
                });
            } else {
                promise.fail("Error opening the text file: " + openResult.cause().getMessage());
            }
        });

        return promise.future();
    }
    public Future<String> updateStudent(Student student) {
        Promise<String> promise = Promise.promise();

        Tuple tuple = Tuple.of(student.getName(), student.getAge(), student.getStudentID());

        jdbcPool.preparedQuery(Constants.UPDATE_STUDENT_DATA)
                .execute(tuple)
                .onFailure(e -> {
                    promise.fail(e.getMessage());
                })
                .onSuccess(rows -> {
                    // Update the student in the text file
                    updateStudentInTextFile(student)
                            .onSuccess(textFileUpdateResult -> {
                                promise.complete("success");
                            })
                            .onFailure(textFileUpdateError -> {
                                promise.fail("Failed to update student in text file: " + textFileUpdateError.getMessage());
                            });
                });

        return promise.future();
    }

    private Future<Void> updateStudentInTextFile(Student student) {
        Promise<Void> promise = Promise.promise();
        String updatedStudentDetails = JsonObject.mapFrom(student).encodePrettily() + System.lineSeparator();
        Buffer buffer = Buffer.buffer(updatedStudentDetails);

        vertx.fileSystem().readFile(textFilePath, readFileResult -> {
            if (readFileResult.succeeded()) {
                String fileContent = readFileResult.result().toString();
                fileContent = fileContent.replaceFirst("\"studentID\":" + student.getStudentID() + "\\s*,\\s*\"name\":\"[^\"]*\"\\s*,\\s*\"age\":" + student.getAge(),
                        "\"studentID\":" + student.getStudentID() + ",\"name\":\"" + student.getName() + "\",\"age\":" + student.getAge());
                vertx.fileSystem().writeFile(textFilePath, Buffer.buffer(fileContent), writeFileResult -> {
                    if (writeFileResult.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail("Error updating student in text file: " + writeFileResult.cause().getMessage());
                    }
                });
            } else {
                promise.fail("Error reading text file: " + readFileResult.cause().getMessage());
            }
        });

        return promise.future();
    }

    public Future<String> deleteStudent(int studentID) {
        Promise<String> promise = Promise.promise();

        jdbcPool.preparedQuery(Constants.DELETE_STUDENT_DATA)
                .execute(Tuple.of(studentID))
                .onFailure(e -> {
                    promise.fail(e.getMessage());
                })
                .onSuccess(rows -> {
                    // Delete the student from the text file
                    deleteStudentFromTextFile(studentID)
                            .onSuccess(textFileDeleteResult -> {
                                promise.complete("success");
                            })
                            .onFailure(textFileDeleteError -> {
                                promise.fail("Failed to delete student from text file: " + textFileDeleteError.getMessage());
                            });
                });

        return promise.future();

    }

    private Future<Void> deleteStudentFromTextFile(int studentID) {
        Promise<Void> promise = Promise.promise();

        vertx.fileSystem().readFile(textFilePath, readFileResult -> {
            if (readFileResult.succeeded()) {
                String fileContent = readFileResult.result().toString();
                fileContent = fileContent.replaceAll("\"studentID\":" + studentID + "\\s*,\\s*\"name\":\"[^\"]*\"\\s*,\\s*\"age\":[0-9]+\\s*\\n", "");
                vertx.fileSystem().writeFile(textFilePath, Buffer.buffer(fileContent), writeFileResult -> {
                    if (writeFileResult.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail("Error deleting student from text file: " + writeFileResult.cause().getMessage());
                    }
                });
            } else {
                promise.fail("Error reading text file: " + readFileResult.cause().getMessage());
            }
        });

        return promise.future();
    }
    private Future<JsonObject> fetchResult(int studentID) {
        Promise<JsonObject> promise = Promise.promise();

        // Check if the result exists in the database
        jdbcPool.preparedQuery("SELECT * FROM subject_results WHERE Student_ID = ?")
                .execute(Tuple.of(studentID))
                .onSuccess(rows -> {
                    if (rows.size() > 0) {
                        // Fetch the result from the database
                        Row row = rows.iterator().next();
                        Result result = new Result();
                        result.setStudentID(row.getInteger("Student_ID"));
                        result.setSubjectID(row.getInteger("Subject_ID"));
                        result.setScore(row.getInteger("marks"));
                        promise.complete(result.toJson());
                    } else {
                        // Result not found in the database, fetch from the third-party API
                        fetchResultFromThirdPartyAPI(studentID, promise);
                    }
                })
                .onFailure(throwable -> {
                    promise.fail("Failed to fetch result from database: " + throwable.getMessage());
                });

        return promise.future();
    }

    private void fetchResultFromThirdPartyAPI(int studentID, Promise<JsonObject> promise) {
        JsonObject requestJson = new JsonObject().put("studentID", studentID);
        vertx.eventBus().request(Constants.THIRD_PARTY_API_EVENT_BUS, requestJson, ar -> {
            if (ar.succeeded()) {
                JsonObject resultJson = (JsonObject) ar.result().body();
                Result result = resultJson.mapTo(Result.class);
                // Save the fetched result to the database
                saveResultToDatabase(result)
                        .onSuccess(saveResult -> {
                            promise.complete(resultJson);
                        })
                        .onFailure(saveError -> {
                            promise.fail("Failed to save result to database: " + saveError.getMessage());
                        });
            } else {
                promise.fail("Failed to fetch result from third-party API: " + ar.cause().getMessage());
            }
        });
    }
    private Future<Void> saveResultToDatabase(Result result) {
        Promise<Void> promise = Promise.promise();

        Tuple tuple = Tuple.of(result.getStudentID(), result.getSubjectID(), result.getScore());

        jdbcPool.preparedQuery(Constants.INSERT_RESULT_DATA)
                .execute(tuple)
                .onSuccess(rows -> {
                    log.info("Result saved successfully to database.");
                    promise.complete();
                })
                .onFailure(throwable -> {
                    log.warning("Failed to save result to database: " + throwable.getMessage());
                    promise.fail(throwable.getMessage());
                });

        return promise.future();
    }
}