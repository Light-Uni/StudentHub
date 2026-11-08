package vn.edu.studentmanager.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.*;
import vn.edu.studentmanager.activities.UserEditorActivity;
import vn.edu.studentmanager.adapters.RecordAdapter;
import vn.edu.studentmanager.databinding.FragmentUsersBinding;
import vn.edu.studentmanager.models.UserProfile;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.Ui;
import java.util.Map;

public class UsersFragment extends Fragment {
    private FragmentUsersBinding binding;
    private ListenerRegistration listener;
    private RecordAdapter<UserProfile> adapter;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentUsersBinding.inflate(inflater, parent, false);
        adapter = new RecordAdapter<>(new RecordAdapter.Presenter<>() {
            public String title(UserProfile p) { return p.fullName; }
            public String subtitle(UserProfile p) { return p.email + "\n" + p.roleLabel() + " · " + p.age + " tuổi"; }
            public String status(UserProfile p) { return p.isActive() ? "Bình thường" : "Đã khóa"; }
            public int statusColor(UserProfile p) { return p.isActive() ? vn.edu.studentmanager.R.color.success : vn.edu.studentmanager.R.color.error; }
        }, this::actions);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext())); binding.list.setAdapter(adapter);
        binding.add.setOnClickListener(v -> startActivity(new Intent(requireContext(), UserEditorActivity.class)));
        binding.retry.setOnClickListener(v -> load()); return binding.getRoot();
    }
    @Override public void onStart() { super.onStart(); load(); }
    private void load() {
        if (!SessionRepository.isAdmin()) return;
        if (listener != null) listener.remove(); binding.progress.setVisibility(View.VISIBLE); binding.retry.setVisibility(View.GONE);
        listener = new Repository().users((rows, e) -> {
            if (binding == null) return; binding.progress.setVisibility(View.GONE);
            if (e != null) { binding.state.setText(Ui.error(e)); binding.retry.setVisibility(View.VISIBLE); return; }
            binding.state.setText(rows.size() + " tài khoản · Chạm để xem và quản lý"); adapter.submit(rows);
        });
    }
    private void actions(UserProfile p) {
        new MaterialAlertDialogBuilder(requireContext()).setTitle(p.fullName)
            .setItems(new String[]{"Xem / sửa thông tin", "Lịch sử đăng nhập", "Xóa tài khoản"}, (d, which) -> {
                if (which == 0) startActivity(new Intent(requireContext(), UserEditorActivity.class).putExtra("uid", p.uid));
                else if (which == 1) history(p);
                else Ui.confirm(requireContext(), "Xóa tài khoản?", p.fullName + " sẽ không thể đăng nhập. Nhật ký được giữ để đối chiếu.", () ->
                    new Repository().call("deleteUser", Map.of("uid", p.uid)).addOnCompleteListener(t -> {
                        if (isAdded()) Ui.message(requireContext(), t.isSuccessful() ? "Đã xóa tài khoản." : Ui.error(t.getException()));
                    }));
            }).setNegativeButton("Đóng", null).show();
    }
    private void history(UserProfile p) {
        ScrollView scroll = new ScrollView(requireContext()); TextView text = new TextView(requireContext());
        text.setPadding(48, 24, 48, 24); text.setTextSize(15); scroll.addView(text);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle("Đăng nhập · " + p.fullName).setView(scroll)
            .setNegativeButton("Đóng", null).setPositiveButton("Tải thêm", null).create();
        DocumentSnapshot[] cursor = {null}; StringBuilder lines = new StringBuilder();
        Runnable loadPage = () -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Repository().loginHistory(p.uid, cursor[0]).addOnSuccessListener(snapshot -> {
                if (!dialog.isShowing()) return;
                for (DocumentSnapshot doc : snapshot) lines.append(Ui.timestamp(doc.getTimestamp("loginAt"))).append("\n").append(doc.getString("device")).append("\n\n");
                if (!snapshot.isEmpty()) cursor[0] = snapshot.getDocuments().get(snapshot.size() - 1);
                text.setText(lines.length() == 0 ? "Chưa có lịch sử đăng nhập." : lines.toString());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(snapshot.size() == 50);
            }).addOnFailureListener(e -> { if (dialog.isShowing()) { text.setText(lines + "\n" + Ui.error(e)); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); } });
        };
        dialog.setOnShowListener(d -> { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> loadPage.run()); loadPage.run(); }); dialog.show();
    }
    @Override public void onStop() { if (listener != null) listener.remove(); listener = null; super.onStop(); }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
