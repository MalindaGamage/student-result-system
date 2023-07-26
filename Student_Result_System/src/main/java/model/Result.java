package model;

import io.vertx.core.json.JsonObject;

public class Result {
    private int studentID;
    private int subjectID;
    private int score;

    // Define Constructors, getters, and setters for the result entity
    public int getStudentID() {
        return studentID;
    }
    public int getSubjectID() {
        return subjectID;
    }
    public void setSubjectID(int subjectID) {
        this.subjectID = subjectID;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public void setStudentID(int studentID) {this.studentID = studentID;}

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("studentID", studentID);
        json.put("subjectID", subjectID);
        json.put("score", score);
        return json;
    }
}
