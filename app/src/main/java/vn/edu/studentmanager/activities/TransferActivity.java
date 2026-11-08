package vn.edu.studentmanager.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import vn.edu.studentmanager.StudentApp;
import vn.edu.studentmanager.databinding.ActivityTransferBinding;
import vn.edu.studentmanager.repositories.Repository;
import vn.edu.studentmanager.utils.*;
import java.io.*;
import java.util.*;

public class TransferActivity extends SecureActivity {
    private ActivityTransferBinding binding;
    private String studentId;
    private boolean template;
    private TabularFiles.Preview preview;
    private final ActivityResultLauncher<String[]> open = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::read);
    private final ActivityResultLauncher<String> createCsv = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> export(uri, false));
    private final ActivityResultLauncher<String> createExcel = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), uri -> export(uri, true));
    @Override protected boolean requiresManager() { return true; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); studentId = getIntent().getStringExtra("studentId"); template = state != null && state.getBoolean("template");
        binding = ActivityTransferBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot()); Ui.insets(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.scope.setText(studentId == null ? "Danh sách sinh viên" : "Chứng chỉ · " + studentId);
        binding.importFile.setOnClickListener(v -> open.launch(new String[]{"text/*", "application/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream"}));
        binding.exportCsv.setOnClickListener(v -> { template = false; createCsv.launch(filename("csv")); });
        binding.exportExcel.setOnClickListener(v -> { template = false; createExcel.launch(filename("xlsx")); });
        binding.template.setOnClickListener(v -> { template = true; createCsv.launch("mau_" + filename("csv")); });
        binding.confirmImport.setOnClickListener(v -> Ui.confirm(this, "Nhập dữ liệu?", "Lưu các dòng hợp lệ. Mã đã tồn tại sẽ được bỏ qua.", this::commit));
        if (state != null) binding.result.setText("Nếu đang xem trước dữ liệu, vui lòng chọn lại tệp để tiếp tục.");
    }
    private String filename(String extension) { return (studentId == null ? "sinh_vien" : "chung_chi_" + studentId) + "." + extension; }
    private String[] columns() { return studentId == null ? Validation.STUDENT_FIELDS : Validation.CERTIFICATE_FIELDS; }
    private Map<String, Object> scope() {
        Map<String, Object> data = new HashMap<>(); data.put("kind", studentId == null ? "students" : "certificates"); if (studentId != null) data.put("studentId", studentId); return data;
    }
    private void busy(boolean busy) {
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.importFile.setEnabled(!busy); binding.exportCsv.setEnabled(!busy); binding.exportExcel.setEnabled(!busy); binding.template.setEnabled(!busy); binding.confirmImport.setEnabled(!busy);
    }
    private void read(Uri uri) {
        if (uri == null) return;
        preview = null; binding.confirmImport.setVisibility(View.GONE); busy(true); binding.result.setText("Đang đọc và kiểm tra tệp…");
        StudentApp.IO.execute(() -> {
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                TabularFiles.Preview result = TabularFiles.preview(TabularFiles.readLimited(stream), studentId != null);
                runOnUiThread(() -> {
                    if (isDestroyed()) return; preview = result; busy(false);
                    StringBuilder summary = new StringBuilder("Đã kiểm tra ").append(result.total).append(" dòng.\nHợp lệ: ").append(result.records.size()).append("\nLỗi: ").append(result.errors.size());
                    for (String error : result.errors) summary.append("\n").append(error);
                    if (!result.records.isEmpty()) {
                        summary.append("\n\nXem trước (tối đa 5 dòng):");
                        for (Map<String, Object> record : result.records.subList(0, Math.min(5, result.records.size()))) summary.append("\n").append(record.get(columns()[0])).append(" · ").append(record.get(studentId == null ? "fullName" : "name"));
                    }
                    binding.result.setText(summary); binding.confirmImport.setVisibility(result.records.isEmpty() ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) { failure(e); }
        });
    }
    private void commit() {
        if (preview == null || preview.records.isEmpty()) return;
        busy(true); Map<String, Object> data = scope(); data.put("records", preview.records);
        new Repository().call("importRecords", data).addOnSuccessListener(this, result -> {
            busy(false); binding.confirmImport.setVisibility(View.GONE);
            List<?> duplicates = (List<?>) result.get("duplicates"), errors = (List<?>) result.get("errors");
            binding.result.setText("Đã nhập: " + result.get("imported") + "\nMã đã tồn tại, bỏ qua: " + duplicates.size() + "\n" + String.join(", ", duplicates.stream().map(Object::toString).toList())
                + "\nLỗi tại máy chủ: " + errors.size() + "\n" + errors + "\nDòng bị loại khi kiểm tra tệp: " + preview.errors.size()); preview = null;
        }).addOnFailureListener(this, this::failure);
    }
    private void export(Uri uri, boolean xlsx) {
        if (uri == null) return;
        busy(true); binding.result.setText("Đang chuẩn bị dữ liệu xuất…");
        if (template) { template = false; write(uri, xlsx, new ArrayList<>()); }
        else exportPage(uri, xlsx, "", new ArrayList<>());
    }
    private void exportPage(Uri uri, boolean xlsx, String after, List<Map<String, Object>> records) {
        Map<String, Object> data = scope(); data.put("after", after);
        new Repository().call("exportRecords", data).addOnSuccessListener(this, result -> {
            @SuppressWarnings("unchecked") List<Map<String, Object>> page = (List<Map<String, Object>>) result.get("records"); records.addAll(page);
            String next = Objects.toString(result.get("next"), "");
            binding.result.setText("Đã đọc " + records.size() + " dòng…");
            if (!next.isEmpty()) exportPage(uri, xlsx, next, records); else write(uri, xlsx, records);
        }).addOnFailureListener(this, this::failure);
    }
    private void write(Uri uri, boolean xlsx, List<Map<String, Object>> records) {
        StudentApp.IO.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new IOException("Không mở được tệp để ghi.");
                if (xlsx) TabularFiles.writeXlsx(output, columns(), records); else TabularFiles.writeCsv(output, columns(), records);
                runOnUiThread(() -> { if (!isDestroyed()) { busy(false); binding.result.setText("Đã lưu " + records.size() + " dòng vào tệp " + (xlsx ? "Excel (.xlsx)." : "CSV UTF-8.")); } });
            } catch (Exception e) { failure(e); }
        });
    }
    private void failure(Exception e) { runOnUiThread(() -> { if (!isDestroyed()) { busy(false); binding.result.setText(Ui.error(e)); } }); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("template", template); super.onSaveInstanceState(out); }
}
