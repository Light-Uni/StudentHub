package vn.edu.studentmanager.models;

import com.google.firebase.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class Student {
    public String studentId = "", fullName = "", birthDate = "", gender = "other", email = "", phone = "", address = "", className = "", major = "", enrollmentDate = "", status = "studying";
    public boolean deleting;
    public Timestamp createdAt, updatedAt;
    public Student() {}
    public String statusLabel() { return switch (status) { case "graduated" -> "Tốt nghiệp"; case "paused" -> "Bảo lưu"; default -> "Đang học"; }; }
    public String genderLabel() { return switch (gender) { case "male" -> "Nam"; case "female" -> "Nữ"; default -> "Khác"; }; }
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studentId", studentId); m.put("fullName", fullName); m.put("birthDate", birthDate);
        m.put("gender", gender); m.put("email", email); m.put("phone", phone); m.put("address", address);
        m.put("className", className); m.put("major", major); m.put("enrollmentDate", enrollmentDate); m.put("status", status);
        return m;
    }
}
