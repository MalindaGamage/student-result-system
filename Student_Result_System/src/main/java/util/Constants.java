package util;

public class Constants {

    // Event bus addresses
    public static final String STUDENT_SAVE_EVENT_BUS = "student.save";
    public static final String STUDENT_UPDATE_EVENT_BUS = "student.update";
    public static final String STUDENT_DELETE_EVENT_BUS = "student.delete";
    public static final String STUDENT_RESULT_FETCH_EVENT_BUS = "student.result";

    public static final String RESULT_SAVE_EVENT_BUS = "result.save";
    public static final String RESULT_UPDATE_EVENT_BUS = "result.update";
    public static final String RESULT_DELETE_EVENT_BUS = "result.delete";
    public static final String THIRD_PARTY_API_EVENT_BUS = "your.third.party.api.event.bus.address";

    // SQL queries
    public static final String INSERT_STUDENT_DATA = "INSERT INTO Student_details (Student_ID, Student_Name, Student_Age) VALUES (?, ?, ?)";
    public static final String UPDATE_STUDENT_DATA = "UPDATE Student_details SET Student_Name = ?, Student_Age = ? WHERE Student_ID = ?";
    public static final String DELETE_STUDENT_DATA = "DELETE FROM Student_details WHERE Student_ID = ?";
    public static final String INSERT_RESULT_DATA = "INSERT INTO Result_details (Student_ID, Subject_ID, Marks) VALUES (?, ?, ?)";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Constants class should not be instantiated.");
    }
}
