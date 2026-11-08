package vn.edu.studentmanager.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import vn.edu.studentmanager.BuildConfig;
import vn.edu.studentmanager.StudentApp;
import vn.edu.studentmanager.databinding.ActivityLoginBinding;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.Ui;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); binding = ActivityLoginBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        binding.environment.setText(BuildConfig.USE_EMULATORS ? "Môi trường thử nghiệm · Firebase Emulator" : "Không gian quản lý đào tạo");
        binding.login.setOnClickListener(v -> login());
        binding.password.setOnEditorActionListener((v, id, e) -> { if (id == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { login(); return true; } return false; });
        if (!StudentApp.configured) {
            error("Chưa cấu hình Firebase. Thêm app/google-services.json và build lại, hoặc chọn biến thể emulatorDebug để chạy thử."); binding.login.setEnabled(false); return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() != null) { busy(true); loadProfile(); }
    }
    private void login() {
        if (!binding.login.isEnabled()) return;
        binding.emailBox.setError(null); binding.passwordBox.setError(null);
        String email = Ui.value(binding.email), password = binding.password.getText().toString();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.emailBox.setError("Nhập email hợp lệ."); return; }
        if (password.isEmpty()) { binding.passwordBox.setError("Vui lòng nhập mật khẩu."); return; }
        busy(true); binding.error.setVisibility(View.GONE);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(this, result -> loadProfile()).addOnFailureListener(this, e -> { busy(false); error(Ui.error(e)); });
    }
    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { busy(false); return; }
        Repository repo = new Repository();
        repo.getUser(uid).addOnSuccessListener(this, profile -> {
            if (profile == null || !profile.isActive()) { SessionRepository.signOut(); busy(false); error("Tài khoản bị khóa hoặc chưa được quản trị viên cấp quyền."); return; }
            SessionRepository.profile = profile;
            repo.call("recordLogin", Map.of("device", Build.MANUFACTURER + " " + Build.MODEL))
                .addOnSuccessListener(this, result -> { startActivity(new Intent(this, MainActivity.class)); finish(); })
                .addOnFailureListener(this, e -> { SessionRepository.signOut(); busy(false); error(Ui.error(e)); });
        }).addOnFailureListener(this, e -> { SessionRepository.signOut(); busy(false); error(Ui.error(e)); });
    }
    private void busy(boolean busy) { binding.login.setEnabled(!busy); binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE); }
    private void error(String text) { binding.error.setText(text); binding.error.setVisibility(View.VISIBLE); }
}
