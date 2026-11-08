package vn.edu.studentmanager.activities;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import vn.edu.studentmanager.R;
import vn.edu.studentmanager.databinding.ActivityMainBinding;
import vn.edu.studentmanager.fragments.*;
import vn.edu.studentmanager.models.UserProfile;
import vn.edu.studentmanager.repositories.SessionRepository;
import vn.edu.studentmanager.utils.Ui;

public class MainActivity extends SecureActivity {
    private ActivityMainBinding binding;
    private String lastRole;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); binding = ActivityMainBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        lastRole = SessionRepository.profile == null ? null : SessionRepository.profile.role;
        binding.navigation.getMenu().findItem(R.id.nav_users).setVisible(SessionRepository.isAdmin());
        binding.navigation.setOnItemSelectedListener(item -> { open(item.getItemId()); return true; });
        if (state == null) open(R.id.nav_home);
    }
    public void showStudents() { binding.navigation.setSelectedItemId(R.id.nav_students); }
    private void open(int id) {
        Fragment fragment;
        if (id == R.id.nav_students) fragment = new StudentsFragment();
        else if (id == R.id.nav_users && SessionRepository.isAdmin()) fragment = new UsersFragment();
        else if (id == R.id.nav_profile) fragment = new ProfileFragment();
        else fragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
    }
    @Override protected void onProfileChanged(UserProfile profile) {
        binding.navigation.getMenu().findItem(R.id.nav_users).setVisible(profile.isAdmin());
        if (lastRole == null || !lastRole.equals(profile.role)) { binding.navigation.setSelectedItemId(R.id.nav_home); open(R.id.nav_home); }
        lastRole = profile.role;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content);
        if (current instanceof ProfileFragment p) p.render();
    }
}
