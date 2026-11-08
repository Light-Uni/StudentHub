package vn.edu.studentmanager.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import vn.edu.studentmanager.models.UserProfile;

public final class SessionRepository {
    public static UserProfile profile;
    private SessionRepository() {}
    public static boolean canManage() { return profile != null && profile.canManage(); }
    public static boolean isAdmin() { return profile != null && profile.isAdmin(); }
    public static ListenerRegistration observe(Repository.Result<UserProfile> callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { callback.accept(null, null); return null; }
        return FirebaseFirestore.getInstance().collection("users").document(uid).addSnapshotListener((snap, error) -> {
            UserProfile p = snap == null ? null : snap.toObject(UserProfile.class);
            if (p != null) p.uid = uid;
            profile = p; callback.accept(p, error);
        });
    }
    public static void signOut() { profile = null; FirebaseAuth.getInstance().signOut(); }
}
