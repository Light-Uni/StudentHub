package vn.edu.studentmanager.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.ListenerRegistration;
import vn.edu.studentmanager.activities.*;
import vn.edu.studentmanager.adapters.RecordAdapter;
import vn.edu.studentmanager.databinding.*;
import vn.edu.studentmanager.models.*;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.Ui;
import java.util.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ListenerRegistration students, users;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentHomeBinding.inflate(inflater, parent, false);
        UserProfile p = SessionRepository.profile;
        binding.greeting.setText(p == null ? "Xin chào" : "Xin chào, " + p.fullName);
        binding.role.setText(p == null ? "" : p.roleLabel());
        Ui.avatar(binding.avatar, p == null ? "" : p.avatarBase64);
        binding.openStudents.setOnClickListener(v -> ((MainActivity) requireActivity()).showStudents());
        binding.addStudent.setVisibility(SessionRepository.canManage() ? View.VISIBLE : View.GONE);
        binding.transfer.setVisibility(SessionRepository.canManage() ? View.VISIBLE : View.GONE);
        binding.addStudent.setOnClickListener(v -> startActivity(new Intent(requireContext(), StudentEditorActivity.class)));
        binding.transfer.setOnClickListener(v -> startActivity(new Intent(requireContext(), TransferActivity.class)));
        binding.refresh.setOnClickListener(v -> load()); return binding.getRoot();
    }
    @Override public void onStart() { super.onStart(); load(); }
    private void load() {
        if (students != null) students.remove(); if (users != null) users.remove();
        Repository repo = new Repository(); binding.state.setText("Đang tải số liệu…");
        students = repo.students((rows, e) -> {
            if (binding == null) return;
            if (e != null) { binding.state.setText(Ui.error(e)); return; }
            binding.studentCount.setText(String.valueOf(rows.size()));
            binding.studyingCount.setText(rows.stream().filter(s -> "studying".equals(s.status)).count() + " sinh viên đang học");
            binding.recent.removeAllViews();
            List<Student> recent = new ArrayList<>(rows);
            recent.sort(Comparator.comparingLong((Student s) -> s.createdAt == null ? 0 : s.createdAt.getSeconds()).reversed());
            for (Student s : recent.subList(0, Math.min(3, recent.size()))) {
                ItemRecordBinding row = ItemRecordBinding.inflate(getLayoutInflater(), binding.recent, false);
                row.initial.setText(RecordAdapter.initials(s.fullName)); row.title.setText(s.fullName);
                row.subtitle.setText(s.studentId + " · " + s.className); row.status.setText(s.statusLabel());
                row.status.setTextColor(requireContext().getColor("paused".equals(s.status) ? vn.edu.studentmanager.R.color.warning_ink : "studying".equals(s.status) ? vn.edu.studentmanager.R.color.success : vn.edu.studentmanager.R.color.text_secondary));
                row.getRoot().setOnClickListener(v -> startActivity(new Intent(requireContext(), StudentDetailActivity.class).putExtra("studentId", s.studentId)));
                binding.recent.addView(row.getRoot());
            }
            binding.state.setText(rows.isEmpty() ? "Chưa có hồ sơ. Thêm sinh viên đầu tiên hoặc nhập danh sách từ tệp." : "Danh sách cập nhật khi dữ liệu thay đổi.");
        });
        repo.certificateCount().addOnSuccessListener(count -> { if (binding != null) binding.certificateCount.setText("Chứng chỉ đã lưu · " + count); })
            .addOnFailureListener(e -> { if (binding != null) binding.certificateCount.setText("Chứng chỉ · Chưa tải được. Nhấn làm mới để thử lại."); });
        if (SessionRepository.isAdmin()) {
            binding.userCount.setVisibility(View.VISIBLE);
            users = repo.users((rows, e) -> { if (binding != null) binding.userCount.setText(e == null ? "Tài khoản hệ thống · " + rows.size() : "Tài khoản · Chưa tải được"); });
        }
    }
    @Override public void onStop() { if (students != null) students.remove(); if (users != null) users.remove(); students = users = null; super.onStop(); }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
