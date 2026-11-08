package vn.edu.studentmanager.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import vn.edu.studentmanager.databinding.ActivityEditorBinding;
import vn.edu.studentmanager.models.Student;
import vn.edu.studentmanager.repositories.Repository;
import vn.edu.studentmanager.utils.*;
import java.util.Map;

public class StudentEditorActivity extends SecureActivity {
    private ActivityEditorBinding binding;
    private FormFields form;
    private String studentId;
    private Bundle restored;
    @Override protected boolean requiresManager() { return true; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); restored = state; studentId = getIntent().getStringExtra("studentId");
        binding = ActivityEditorBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        binding.toolbar.setTitle(studentId == null ? "Thêm sinh viên" : "Sửa hồ sơ sinh viên");
        binding.toolbar.setNavigationOnClickListener(v -> finish()); binding.cancel.setOnClickListener(v -> finish());
        binding.description.setText("Mã sinh viên là duy nhất và không đổi sau khi tạo. Điện thoại và địa chỉ có thể để trống.");
        binding.save.setOnClickListener(v -> save());
        if (studentId == null) render(new Student());
        else { busy(true); new Repository().getStudent(studentId).addOnSuccessListener(this, s -> {
            busy(false); if (s == null || s.deleting) { Ui.message(this, "Không tìm thấy sinh viên."); finish(); } else render(s);
        }).addOnFailureListener(this, e -> { busy(false); binding.save.setEnabled(false); showError(Ui.error(e)); }); }
    }
    private void render(Student s) {
        form = new FormFields(this, binding.fields);
        form.text("studentId", "Mã sinh viên *", s.studentId, InputType.TYPE_CLASS_TEXT).setEnabled(studentId == null);
        form.text("fullName", "Họ và tên *", s.fullName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.date("birthDate", "Ngày sinh *", s.birthDate);
        form.select("gender", "Giới tính", s.gender, new String[]{"male", "female", "other"}, new String[]{"Nam", "Nữ", "Khác"});
        form.text("email", "Email *", s.email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.text("phone", "Số điện thoại", s.phone, InputType.TYPE_CLASS_PHONE);
        form.text("address", "Địa chỉ", s.address, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.text("className", "Lớp *", s.className, InputType.TYPE_CLASS_TEXT);
        form.text("major", "Ngành học *", s.major, InputType.TYPE_CLASS_TEXT);
        form.date("enrollmentDate", "Ngày nhập học *", s.enrollmentDate);
        form.select("status", "Trạng thái", s.status, new String[]{"studying", "graduated", "paused"}, new String[]{"Đang học", "Tốt nghiệp", "Bảo lưu"});
        form.restore(restored);
    }
    private void save() {
        if (form == null) return;
        Map<String, Object> data = form.values(); if (studentId != null) data.put("studentId", studentId);
        String error = Validation.validate(data, false); if (error != null) { showError(error); return; }
        busy(true); new Repository().call("saveStudent", Map.of("record", data, "create", studentId == null))
            .addOnSuccessListener(this, r -> { Ui.message(this, "Đã lưu hồ sơ sinh viên."); finish(); })
            .addOnFailureListener(this, e -> { busy(false); showError(Ui.error(e)); });
    }
    private void showError(String message) { binding.error.setText(message); binding.error.setVisibility(View.VISIBLE); }
    private void busy(boolean busy) { binding.save.setEnabled(!busy); binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE); binding.error.setVisibility(View.GONE); }
    @Override protected void onSaveInstanceState(Bundle out) { if (form != null) form.save(out); super.onSaveInstanceState(out); }
}
