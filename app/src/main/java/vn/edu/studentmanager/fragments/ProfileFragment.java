package vn.edu.studentmanager.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import vn.edu.studentmanager.StudentApp;
import vn.edu.studentmanager.activities.LoginActivity;
import vn.edu.studentmanager.databinding.FragmentProfileBinding;
import vn.edu.studentmanager.models.UserProfile;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.Ui;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private final ActivityResultLauncher<String[]> pick = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
        if (uri == null || binding == null) return;
        binding.progress.setVisibility(View.VISIBLE); binding.changeAvatar.setEnabled(false);
        android.content.Context context = requireContext().getApplicationContext();
        StudentApp.IO.execute(() -> {
            try {
                String image = Ui.readAvatar(context, uri);
                new Repository().call("updateAvatar", Map.of("avatarBase64", image))
                    .addOnCompleteListener(task -> {
                        if (binding == null) return; binding.progress.setVisibility(View.GONE); binding.changeAvatar.setEnabled(true);
                        Ui.message(requireContext(), task.isSuccessful() ? "Đã cập nhật ảnh đại diện." : Ui.error(task.getException()));
                    });
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (binding == null) return; binding.progress.setVisibility(View.GONE); binding.changeAvatar.setEnabled(true); Ui.message(requireContext(), Ui.error(e));
                });
            }
        });
    });
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentProfileBinding.inflate(inflater, parent, false);
        binding.changeAvatar.setOnClickListener(v -> pick.launch(new String[]{"image/*"}));
        binding.logout.setOnClickListener(v -> Ui.confirm(requireContext(), "Đăng xuất?", "Bạn sẽ cần đăng nhập để xem dữ liệu.", () -> {
            SessionRepository.signOut(); startActivity(new Intent(requireContext(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        })); render(); return binding.getRoot();
    }
    public void render() {
        UserProfile p = SessionRepository.profile; if (binding == null || p == null) return;
        binding.name.setText(p.fullName); binding.details.setText(p.roleLabel() + "\n" + p.email + "\nTuổi: " + p.age + "\nĐiện thoại: " + (p.phone.isEmpty() ? "—" : p.phone) + "\nNgày tạo: " + Ui.timestamp(p.createdAt));
        Ui.avatar(binding.avatar, p.avatarBase64);
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
