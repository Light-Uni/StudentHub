package vn.edu.studentmanager.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import vn.edu.studentmanager.BuildConfig;
import vn.edu.studentmanager.models.*;
import java.util.*;

public class Repository {
    public interface Result<T> { void accept(T value, Exception error); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION);
    public Task<Map<String, Object>> call(String name, Map<String, Object> data) {
        return functions.getHttpsCallable(name).call(data).continueWith(task -> {
            if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
            @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
            return result;
        });
    }
    public ListenerRegistration students(Result<List<Student>> callback) {
        return db.collection("students").orderBy("fullName").addSnapshotListener((snap, error) -> {
            List<Student> rows = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot doc : snap) { Student s = doc.toObject(Student.class); if (s != null && !s.deleting) rows.add(s); }
            callback.accept(rows, error);
        });
    }
    public ListenerRegistration student(String id, Result<Student> callback) {
        return db.collection("students").document(id).addSnapshotListener((snap, error) -> callback.accept(snap == null ? null : snap.toObject(Student.class), error));
    }
    public Task<Student> getStudent(String id) {
        return db.collection("students").document(id).get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
            return t.getResult().toObject(Student.class);
        });
    }
    public ListenerRegistration certificates(String id, Result<List<Certificate>> callback) {
        return db.collection("students").document(id).collection("certificates").orderBy("name").addSnapshotListener((snap, error) -> {
            List<Certificate> rows = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot doc : snap) { Certificate c = doc.toObject(Certificate.class); if (c != null) rows.add(c); }
            callback.accept(rows, error);
        });
    }
    public ListenerRegistration users(Result<List<UserProfile>> callback) {
        return db.collection("users").orderBy("fullName").addSnapshotListener((snap, error) -> {
            List<UserProfile> rows = new ArrayList<>();
            if (snap != null) for (DocumentSnapshot doc : snap) { UserProfile p = doc.toObject(UserProfile.class); if (p != null) { p.uid = doc.getId(); rows.add(p); } }
            callback.accept(rows, error);
        });
    }
    public Task<UserProfile> getUser(String uid) {
        return db.collection("users").document(uid).get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
            UserProfile p = t.getResult().toObject(UserProfile.class); if (p != null) p.uid = uid; return p;
        });
    }
    public Task<QuerySnapshot> loginHistory(String uid, DocumentSnapshot after) {
        Query q = db.collection("login_history").whereEqualTo("userId", uid).orderBy("loginAt", Query.Direction.DESCENDING).limit(50);
        if (after != null) q = q.startAfter(after);
        return q.get(Source.SERVER);
    }
    public Task<Long> certificateCount() {
        return db.collectionGroup("certificates").count().get(AggregateSource.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException()); return t.getResult().getCount();
        });
    }
}
