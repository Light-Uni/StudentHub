package vn.edu.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.ListenerRegistration;
import vn.edu.studentmanager.StudentApp;
import vn.edu.studentmanager.models.UserProfile;
import vn.edu.studentmanager.repositories.SessionRepository;
import vn.edu.studentmanager.utils.Ui;

public abstract class SecureActivity extends AppCompatActivity {
    private ListenerRegistration session;
    protected boolean requiresManager() { return false; }
    protected boolean requiresAdmin() { return false; }
    @Override protected void onCreate(Bundle state) { super.onCreate(state); }
    @Override protected void onStart() {
        super.onStart();
        if (!StudentApp.configured) { goToLogin(); return; }
        session = SessionRepository.observe((profile, error) -> {
            if (isFinishing()) return;
            if (error != null || profile == null || !profile.isActive()) {
                SessionRepository.signOut(); goToLogin(); return;
            }
            if ((requiresManager() && !profile.canManage()) || (requiresAdmin() && !profile.isAdmin())) {
                Ui.message(this, "Bạn không có quyền mở chức năng này."); finish(); return;
            }
            onProfileChanged(profile);
        });
    }
    protected void onProfileChanged(UserProfile profile) {}
    protected void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
    }
    @Override protected void onStop() { if (session != null) session.remove(); session = null; super.onStop(); }
}
