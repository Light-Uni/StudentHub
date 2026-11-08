package vn.edu.studentmanager.utils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class Validation {
    public static final String[] STUDENT_FIELDS = {"studentId", "fullName", "birthDate", "gender", "email", "phone", "address", "className", "major", "enrollmentDate", "status"};
    public static final String[] CERTIFICATE_FIELDS = {"certificateId", "name", "issuer", "issueDate", "expirationDate", "description"};
    private Validation() {}
    public static String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
    public static String validate(Map<String, ?> row, boolean certificates) {
        try {
            if (certificates) {
                id(row, "certificateId"); required(row, "name", 160); required(row, "issuer", 160);
                LocalDate issue = date(row, "issueDate", false), expiry = date(row, "expirationDate", true);
                if (expiry != null && expiry.isBefore(issue)) return "Ngày hết hạn phải từ ngày cấp trở đi.";
                optional(row, "description", 1000);
            } else {
                id(row, "studentId"); required(row, "fullName", 120); required(row, "className", 60); required(row, "major", 120);
                String email = required(row, "email", 160);
                if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return "Email không hợp lệ.";
                String phone = optional(row, "phone", 20);
                if (!phone.isEmpty() && !phone.matches("\\+?[0-9 ()-]{7,20}")) return "Số điện thoại không hợp lệ.";
                optional(row, "address", 300);
                LocalDate birth = date(row, "birthDate", false), enrollment = date(row, "enrollmentDate", false);
                if (birth.isAfter(LocalDate.now())) return "Ngày sinh không được ở tương lai.";
                if (enrollment.isBefore(birth)) return "Ngày nhập học phải sau ngày sinh.";
                if (!List.of("male", "female", "other").contains(row.get("gender"))) return "Giới tính phải là male, female hoặc other.";
                if (!List.of("studying", "graduated", "paused").contains(row.get("status"))) return "Trạng thái phải là studying, graduated hoặc paused.";
            }
            return null;
        } catch (IllegalArgumentException ex) { return ex.getMessage(); }
    }
    private static void id(Map<String, ?> row, String key) {
        if (!required(row, key, 40).matches("[A-Za-z0-9_-]{1,40}")) throw new IllegalArgumentException("Mã chỉ gồm chữ, số, gạch ngang hoặc gạch dưới.");
    }
    private static String required(Map<String, ?> row, String key, int max) {
        String s = optional(row, key, max);
        if (s.isBlank()) throw new IllegalArgumentException("Thiếu trường " + key + ".");
        return s;
    }
    private static String optional(Map<String, ?> row, String key, int max) {
        Object raw = row.get(key);
        String s = raw == null ? "" : raw.toString().trim();
        if (s.length() > max) throw new IllegalArgumentException(key + " vượt quá " + max + " ký tự.");
        return s;
    }
    private static LocalDate date(Map<String, ?> row, String key, boolean optional) {
        String s = optional(row, key, 10);
        if (s.isEmpty() && optional) return null;
        try {
            if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("", s, 0);
            LocalDate d = LocalDate.parse(s);
            if (d.getYear() < 1900 || d.getYear() > 2200) throw new DateTimeParseException("", s, 0);
            return d;
        } catch (DateTimeParseException e) { throw new IllegalArgumentException(key + " phải là ngày hợp lệ dạng yyyy-MM-dd."); }
    }
}
