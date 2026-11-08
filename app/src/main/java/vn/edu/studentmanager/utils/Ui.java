package vn.edu.studentmanager.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.functions.FirebaseFunctionsException;
import vn.edu.studentmanager.R;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Ui {
    private Ui() {}
    public static void insets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
    }
    public static void message(Context c, String text) { Toast.makeText(c, text, Toast.LENGTH_LONG).show(); }
    public static String error(Exception error) {
        if (error instanceof FirebaseFunctionsException e) return e.getMessage() == null ? "Không thể thực hiện thao tác." : e.getMessage();
        if (error instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException || error instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) return "Email, mật khẩu không đúng hoặc tài khoản đã bị khóa.";
        if (error instanceof com.google.firebase.FirebaseNetworkException) return "Không có kết nối. Kiểm tra mạng rồi thử lại.";
        if (error instanceof IOException || error instanceof IllegalArgumentException) return error.getMessage();
        return "Không thể tải hoặc lưu dữ liệu. Kiểm tra kết nối và quyền tài khoản rồi thử lại.";
    }
    public static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    public static TextInputEditText field(Context c, LinearLayout container, String label, String initial, int inputType) {
        TextInputLayout box = new TextInputLayout(c);
        box.setHint(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(c, 16); box.setLayoutParams(lp);
        TextInputEditText edit = new TextInputEditText(c); edit.setInputType(inputType); edit.setText(initial);
        edit.setMinHeight(dp(c, 56)); box.addView(edit); container.addView(box); return edit;
    }
    public static String value(EditText field) { return field.getText().toString().trim(); }
    public static void confirm(Context c, String title, String body, Runnable action) {
        new MaterialAlertDialogBuilder(c).setTitle(title).setMessage(body).setNegativeButton("Hủy", null)
            .setPositiveButton("Xác nhận", (d, w) -> action.run()).show();
    }
    public static String timestamp(com.google.firebase.Timestamp value) {
        return value == null ? "—" : new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(value.toDate());
    }
    public static void avatar(ImageView target, String base64) {
        target.setImageResource(R.drawable.ic_person);
        if (base64 != null && !base64.isEmpty()) {
            try { byte[] bytes = Base64.decode(base64, Base64.DEFAULT); target.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length)); }
            catch (IllegalArgumentException ignored) { target.setImageResource(R.drawable.ic_person); }
        }
    }
    public static String readAvatar(Context c, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        try (InputStream stream = c.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(stream, null, bounds); }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Không đọc được ảnh. Hãy chọn JPG hoặc PNG.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        while (Math.max(bounds.outWidth, bounds.outHeight) / Math.max(1, options.inSampleSize) > 512) options.inSampleSize = Math.max(1, options.inSampleSize) * 2;
        Bitmap image;
        try (InputStream stream = c.getContentResolver().openInputStream(uri)) { image = BitmapFactory.decodeStream(stream, null, options); }
        if (image == null) throw new IOException("Không đọc được ảnh.");
        int edge = Math.min(image.getWidth(), image.getHeight());
        Bitmap crop = Bitmap.createBitmap(image, (image.getWidth() - edge) / 2, (image.getHeight() - edge) / 2, edge, edge);
        Bitmap small = Bitmap.createScaledBitmap(crop, 256, 256, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); small.compress(Bitmap.CompressFormat.JPEG, 82, out);
        if (out.size() > 135000) throw new IOException("Ảnh quá lớn. Vui lòng chọn ảnh khác.");
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }
}
