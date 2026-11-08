package vn.edu.studentmanager.models;

import com.google.firebase.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
public class Certificate {
    public String certificateId = "", name = "", issuer = "", issueDate = "", expirationDate = "", description = "", studentId = "";
    public Timestamp createdAt, updatedAt;
    public Certificate() {}
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificateId", certificateId); m.put("name", name); m.put("issuer", issuer);
        m.put("issueDate", issueDate); m.put("expirationDate", expirationDate); m.put("description", description);
        return m;
    }
}
