package vn.edu.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;
import vn.edu.studentmanager.adapters.RecordAdapter;
import vn.edu.studentmanager.databinding.ActivityDetailBinding;
import vn.edu.studentmanager.models.*;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.*;
import java.util.Map;

public class StudentDetailActivity extends SecureActivity {
    private ActivityDetailBinding binding;
    private String studentId;
    private Student student;
    private ListenerRegistration studentListener, certificatesListener;
    private RecordAdapter<Certificate> adapter;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); studentId = getIntent().getStringExtra("studentId"); if (studentId == null) { finish(); return; }
        binding = ActivityDetailBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.edit.setOnClickListener(v -> startActivity(new Intent(this, StudentEditorActivity.class).putExtra("studentId", studentId)));
        binding.delete.setOnClickListener(v -> Ui.confirm(this, "Xóa sinh viên?", "Xóa " + (student == null ? studentId : student.fullName) + " và toàn bộ chứng chỉ. Không thể hoàn tác.", () -> {
            binding.delete.setEnabled(false);
            new Repository().call("deleteStudent", Map.of("studentId", studentId)).addOnSuccessListener(this, r -> finish())
                .addOnFailureListener(this, e -> { binding.delete.setEnabled(true); Ui.message(this, Ui.error(e)); });
        }));
        binding.addCertificate.setOnClickListener(v -> certificateForm(new Certificate(), true));
        binding.transfer.setOnClickListener(v -> startActivity(new Intent(this, TransferActivity.class).putExtra("studentId", studentId)));
        adapter = new RecordAdapter<>(new RecordAdapter.Presenter<>() {
            public String title(Certificate c) { return c.name; }
            public String subtitle(Certificate c) { return c.certificateId + " · " + c.issuer; }
            public String status(Certificate c) { return "Cấp: " + c.issueDate + (c.expirationDate.isEmpty() ? " · Không hết hạn" : " · Hết hạn: " + c.expirationDate); }
        }, this::showCertificate);
        binding.certificates.setLayoutManager(new LinearLayoutManager(this)); binding.certificates.setAdapter(adapter);
        permissions();
    }
    private void permissions() {
        if (binding == null) return;
        int visible = SessionRepository.canManage() ? View.VISIBLE : View.GONE;
        binding.edit.setVisibility(visible); binding.delete.setVisibility(visible); binding.addCertificate.setVisibility(visible); binding.transfer.setVisibility(visible);
    }
    @Override protected void onProfileChanged(UserProfile profile) { permissions(); }
    @Override protected void onStart() {
        super.onStart(); if (binding == null) return;
        Repository repo = new Repository();
        studentListener = repo.student(studentId, (s, e) -> {
            if (e != null) { binding.state.setText(Ui.error(e)); return; }
            if (s == null || s.deleting) { Ui.message(this, "Hồ sơ đã bị xóa hoặc đang được xóa."); finish(); return; }
            student = s; binding.name.setText(s.fullName); binding.initial.setText(RecordAdapter.initials(s.fullName));
            binding.subtitle.setText(s.studentId + " · " + s.statusLabel());
            binding.info.setText("Lớp: " + s.className + "\nNgành: " + s.major + "\nNgày sinh: " + s.birthDate + "\nGiới tính: " + s.genderLabel()
                + "\nEmail: " + s.email + "\nĐiện thoại: " + dash(s.phone) + "\nĐịa chỉ: " + dash(s.address) + "\nNhập học: " + s.enrollmentDate
                + "\nTạo: " + Ui.timestamp(s.createdAt) + "\nCập nhật: " + Ui.timestamp(s.updatedAt));
        });
        certificatesListener = repo.certificates(studentId, (rows, e) -> {
            binding.progress.setVisibility(View.GONE);
            if (e != null) { binding.state.setText(Ui.error(e)); return; }
            binding.state.setText(rows.isEmpty() ? "Chưa có chứng chỉ cho sinh viên này." : rows.size() + " chứng chỉ"); adapter.submit(rows);
        });
    }
    private String dash(String value) { return value.isEmpty() ? "—" : value; }
    private void showCertificate(Certificate c) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this).setTitle(c.name)
            .setMessage("Mã: " + c.certificateId + "\nĐơn vị cấp: " + c.issuer + "\nNgày cấp: " + c.issueDate + "\nHết hạn: " + dash(c.expirationDate) + "\n\n" + c.description)
            .setNegativeButton("Đóng", null);
        if (SessionRepository.canManage()) {
            dialog.setPositiveButton("Sửa", (d, w) -> certificateForm(c, false));
            dialog.setNeutralButton("Xóa", (d, w) -> Ui.confirm(this, "Xóa chứng chỉ?", c.name + " sẽ bị xóa khỏi hồ sơ.", () ->
                new Repository().call("deleteCertificate", Map.of("studentId", studentId, "certificateId", c.certificateId))
                    .addOnFailureListener(this, e -> Ui.message(this, Ui.error(e)))));
        }
        dialog.show();
    }
    private void certificateForm(Certificate c, boolean create) {
        if (!SessionRepository.canManage()) return;
        ScrollView scroll = new ScrollView(this); LinearLayout container = new LinearLayout(this); container.setOrientation(LinearLayout.VERTICAL);
        int padding = Ui.dp(this, 24); container.setPadding(padding, padding, padding, 0); scroll.addView(container);
        FormFields form = new FormFields(this, container);
        form.text("certificateId", "Mã chứng chỉ *", c.certificateId, InputType.TYPE_CLASS_TEXT).setEnabled(create);
        form.text("name", "Tên chứng chỉ *", c.name, InputType.TYPE_CLASS_TEXT);
        form.text("issuer", "Đơn vị cấp *", c.issuer, InputType.TYPE_CLASS_TEXT);
        form.date("issueDate", "Ngày cấp *", c.issueDate); form.date("expirationDate", "Ngày hết hạn (không bắt buộc)", c.expirationDate);
        form.text("description", "Mô tả", c.description, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        TextView error = new TextView(this); error.setTextColor(getColor(vn.edu.studentmanager.R.color.error)); error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); container.addView(error);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(create ? "Thêm chứng chỉ" : "Sửa chứng chỉ").setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Map<String, Object> data = form.values(); String message = Validation.validate(data, true);
            if (message != null) { error.setText(message); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); error.setText("Đang lưu…");
            new Repository().call("saveCertificate", Map.of("studentId", studentId, "record", data, "create", create))
                .addOnSuccessListener(this, r -> { dialog.dismiss(); Ui.message(this, "Đã lưu chứng chỉ."); })
                .addOnFailureListener(this, e -> { error.setText(Ui.error(e)); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); });
        })); dialog.show();
    }
    @Override protected void onStop() { if (studentListener != null) studentListener.remove(); if (certificatesListener != null) certificatesListener.remove(); super.onStop(); }
}
