package vn.edu.studentmanager.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;
import vn.edu.studentmanager.R;
import vn.edu.studentmanager.activities.*;
import vn.edu.studentmanager.adapters.RecordAdapter;
import vn.edu.studentmanager.databinding.FragmentStudentsBinding;
import vn.edu.studentmanager.models.Student;
import vn.edu.studentmanager.repositories.*;
import vn.edu.studentmanager.utils.*;
import java.text.Collator;
import java.util.*;

public class StudentsFragment extends Fragment {
    private FragmentStudentsBinding binding;
    private ListenerRegistration listener;
    private final List<Student> students = new ArrayList<>();
    private RecordAdapter<Student> adapter;
    private int sort = 0;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentStudentsBinding.inflate(inflater, parent, false);
        adapter = new RecordAdapter<>(new RecordAdapter.Presenter<>() {
            public String title(Student s) { return s.fullName; }
            public String subtitle(Student s) { return s.studentId + " · " + s.className + "\n" + s.major; }
            public String status(Student s) { return s.statusLabel(); }
            public int statusColor(Student s) { return "paused".equals(s.status) ? R.color.warning_ink : "studying".equals(s.status) ? R.color.success : R.color.text_secondary; }
        }, s -> startActivity(new Intent(requireContext(), StudentDetailActivity.class).putExtra("studentId", s.studentId)));
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext())); binding.list.setAdapter(adapter);
        binding.add.setVisibility(SessionRepository.canManage() ? View.VISIBLE : View.GONE);
        binding.add.setOnClickListener(v -> startActivity(new Intent(requireContext(), StudentEditorActivity.class)));
        binding.filters.setOnCheckedStateChangeListener((group, ids) -> filter());
        binding.search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            public void afterTextChanged(Editable e) {}
        });
        binding.sort.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle("Sắp xếp sinh viên")
            .setSingleChoiceItems(new String[]{"Tên A–Z", "Tên Z–A", "Mới nhất", "Cũ nhất", "Mã sinh viên", "Lớp"}, sort,
                (d, which) -> { sort = which; filter(); d.dismiss(); }).setNegativeButton("Đóng", null).show());
        binding.retry.setOnClickListener(v -> subscribe()); return binding.getRoot();
    }
    @Override public void onStart() { super.onStart(); subscribe(); }
    private void subscribe() {
        if (listener != null) listener.remove();
        binding.progress.setVisibility(View.VISIBLE); binding.retry.setVisibility(View.GONE);
        listener = new Repository().students((rows, error) -> {
            if (binding == null) return;
            binding.progress.setVisibility(View.GONE);
            if (error != null) { binding.state.setText(Ui.error(error)); binding.state.setVisibility(View.VISIBLE); binding.retry.setVisibility(View.VISIBLE); return; }
            students.clear(); students.addAll(rows); filter();
        });
    }
    private void filter() {
        if (binding == null) return;
        String query = Validation.normalized(Ui.value(binding.search));
        int id = binding.filters.getCheckedChipId();
        String status = id == R.id.filterStudying ? "studying" : id == R.id.filterGraduated ? "graduated" : id == R.id.filterPaused ? "paused" : "";
        List<Student> visible = new ArrayList<>();
        for (Student s : students) {
            String haystack = Validation.normalized(s.studentId + " " + s.fullName + " " + s.className + " " + s.major + " " + s.statusLabel());
            boolean matches = true; for (String token : query.split("\\s+")) if (!haystack.contains(token)) matches = false;
            if (matches && (status.isEmpty() || status.equals(s.status))) visible.add(s);
        }
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        Comparator<Student> comparator = (a, b) -> collator.compare(a.fullName, b.fullName);
        if (sort == 1) comparator = comparator.reversed();
        else if (sort == 2 || sort == 3) {
            comparator = Comparator.comparingLong(s -> s.createdAt == null ? 0 : s.createdAt.getSeconds()); if (sort == 2) comparator = comparator.reversed();
        } else if (sort == 4) comparator = Comparator.comparing(s -> s.studentId);
        else if (sort == 5) comparator = Comparator.comparing(s -> s.className, collator);
        visible.sort(comparator.thenComparing(s -> s.studentId)); adapter.submit(visible);
        binding.count.setText(visible.size() + " / " + students.size() + " sinh viên");
        binding.state.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        binding.state.setText(students.isEmpty() ? R.string.empty_students : R.string.no_results);
    }
    @Override public void onStop() { if (listener != null) listener.remove(); listener = null; super.onStop(); }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
