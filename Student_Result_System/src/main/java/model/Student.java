package model;

import io.vertx.core.json.JsonObject;

public class Student {
    private int studentID;
    private String name;
    private int age;

    // Add a new field to storing result details

    //Define Constructors, getters and setters for student entity
    public int getStudentID() {
        return studentID;
    }
    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("studentID", studentID);
        json.put("name", name);
        json.put("age", age);
        return json;
    }

}
