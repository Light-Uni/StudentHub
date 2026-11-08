package vn.edu.studentmanager.models;

import com.google.firebase.Timestamp;
public class UserProfile {
    public String uid = "", fullName = "", email = "", phone = "", role = "employee", status = "locked", avatarBase64 = "";
    public long age;
    public boolean bootstrap, deleting;
    public Timestamp createdAt, updatedAt;
    public UserProfile() {}
    public boolean isAdmin() { return "admin".equals(role) && isActive(); }
    public boolean canManage() { return isActive() && ("admin".equals(role) || "manager".equals(role)); }
    public boolean isActive() { return "normal".equals(status) && !deleting; }
    public String roleLabel() { return switch (role) { case "admin" -> "Quản trị viên"; case "manager" -> "Quản lý"; default -> "Nhân viên"; }; }
}
