package vn.edu.studentmanager;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentApp extends Application {
    public static boolean configured;
    public static final ExecutorService IO = Executors.newFixedThreadPool(2);

    @Override public void onCreate() {
        super.onCreate();
        if (BuildConfig.USE_EMULATORS) {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, new FirebaseOptions.Builder()
                    .setProjectId("demo-student-manager").setApplicationId("1:1234567890:android:emulator")
                    // Giá trị giả chỉ cho Emulator, đủ định dạng 39 ký tự mà SDK kiểm tra.
                    .setApiKey("A00000000000000000000000000000000000000").build());
            }
        } else if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        configured = !FirebaseApp.getApps(this).isEmpty();
        if (!configured) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build());
        if (BuildConfig.USE_EMULATORS) {
            FirebaseAuth.getInstance().useEmulator(BuildConfig.EMULATOR_HOST, 9099);
            db.useEmulator(BuildConfig.EMULATOR_HOST, 8080);
            FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION).useEmulator(BuildConfig.EMULATOR_HOST, 5001);
        }
    }
}
