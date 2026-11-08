package vn.edu.studentmanager.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import vn.edu.studentmanager.databinding.ActivityEditorBinding;
import vn.edu.studentmanager.models.UserProfile;
import vn.edu.studentmanager.repositories.Repository;
import vn.edu.studentmanager.utils.*;
import java.util.Map;

public class UserEditorActivity extends SecureActivity {
    private ActivityEditorBinding binding;
    private FormFields form;
    private String uid;
    private Bundle restored;
    @Override protected boolean requiresAdmin() { return true; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); restored = state; uid = getIntent().getStringExtra("uid");
        binding = ActivityEditorBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        binding.toolbar.setTitle(uid == null ? "Thêm tài khoản" : "Thông tin tài khoản"); binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.cancel.setOnClickListener(v -> finish()); binding.save.setOnClickListener(v -> save());
        binding.description.setText(uid == null ? "Cấp tài khoản và chia sẻ mật khẩu qua kênh riêng. Mật khẩu cần ít nhất 12 ký tự." : "Email đăng nhập giữ nguyên. Thay đổi vai trò hoặc khóa tài khoản được kiểm tra tại máy chủ.");
        if (uid == null) { UserProfile p = new UserProfile(); p.status = "normal"; render(p); }
        else { busy(true); new Repository().getUser(uid).addOnSuccessListener(this, p -> {
            busy(false); if (p == null) { Ui.message(this, "Tài khoản không còn tồn tại."); finish(); } else render(p);
        }).addOnFailureListener(this, e -> { busy(false); binding.save.setEnabled(false); error(Ui.error(e)); }); }
    }
    private void render(UserProfile p) {
        form = new FormFields(this, binding.fields);
        form.text("fullName", "Họ và tên *", p.fullName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.text("age", "Tuổi (16–100) *", p.age == 0 ? "" : String.valueOf(p.age), InputType.TYPE_CLASS_NUMBER);
        form.text("phone", "Số điện thoại", p.phone, InputType.TYPE_CLASS_PHONE);
        form.text("email", "Email đăng nhập *", p.email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).setEnabled(uid == null);
        if (uid == null) form.text("password", "Mật khẩu ban đầu *", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.select("role", "Vai trò", p.role, new String[]{"employee", "manager", "admin"}, new String[]{"Nhân viên", "Quản lý", "Quản trị viên"});
        form.select("status", "Trạng thái", p.status, new String[]{"normal", "locked"}, new String[]{"Bình thường", "Đã khóa"});
        form.restore(restored);
    }
    private void save() {
        if (form == null) return;
        Map<String, Object> data = form.values();
        if (data.get("fullName").toString().isBlank()) { error("Vui lòng nhập họ tên."); return; }
        try { int age = Integer.parseInt(data.get("age").toString()); if (age < 16 || age > 100) throw new NumberFormatException(); data.put("age", age); }
        catch (NumberFormatException e) { error("Tuổi phải từ 16 đến 100."); return; }
        if (uid == null && data.get("password").toString().length() < 12) { error("Mật khẩu cần ít nhất 12 ký tự."); return; }
        if (uid != null) data.put("uid", uid);
        busy(true); new Repository().call("saveUser", data).addOnSuccessListener(this, r -> { Ui.message(this, "Đã lưu tài khoản."); finish(); })
            .addOnFailureListener(this, e -> { busy(false); error(Ui.error(e)); });
    }
    private void error(String message) { binding.error.setText(message); binding.error.setVisibility(View.VISIBLE); }
    private void busy(boolean busy) { binding.save.setEnabled(!busy); binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE); binding.error.setVisibility(View.GONE); }
    @Override protected void onSaveInstanceState(Bundle out) { if (form != null) { form.save(out); out.remove("field_password"); } super.onSaveInstanceState(out); }
}
