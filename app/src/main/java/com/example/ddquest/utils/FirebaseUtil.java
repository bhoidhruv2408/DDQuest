package com.example.ddquest.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Centralised Firebase utilities - singleton pattern
 * Safe, null-safe, and used across the app
 */
public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";

    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;

    // Optional: Cache admin status to reduce Firestore reads
    private static Boolean cachedIsAdmin = null;

    // Private constructor
    private FirebaseUtil() {}

    /** Get Firebase Auth instance */
    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    /** Get Firestore instance */
    public static FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }

    /** Get Firebase Storage instance */
    public static FirebaseStorage getStorage() {
        if (storage == null) {
            storage = FirebaseStorage.getInstance();
        }
        return storage;
    }

    /** Get current user ID (or null if not logged in) */
    public static String currentUserId() {
        return getAuth().getCurrentUser() != null
                ? getAuth().getCurrentUser().getUid()
                : null;
    }

    /** Logout current user */
    public static void logout() {
        getAuth().signOut();
        cachedIsAdmin = null; // Clear cache on logout
    }

    /** Check if user is logged in */
    public static boolean isLoggedIn() {
        return getAuth().getCurrentUser() != null;
    }

    /**
     * Check if current user is an admin (ASYNC - RECOMMENDED)
     * Uses Firestore: /admins/{uid} → { role: "admin" }
     * Caches result to reduce reads.
     */
    public static void isAdmin(AdminCheckCallback callback) {
        String uid = currentUserId();
        if (uid == null) {
            callback.onResult(false);
            return;
        }

        // Return cached result if available
        if (cachedIsAdmin != null) {
            callback.onResult(cachedIsAdmin);
            return;
        }

        getDb().collection("admins")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = documentSnapshot.exists()
                            && "admin".equals(documentSnapshot.getString("role"));
                    cachedIsAdmin = isAdmin; // Cache result
                    callback.onResult(isAdmin);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check admin status for UID: " + uid, e);
                    callback.onResult(false);
                });
    }

    /**
     * DANGEROUS: Synchronous admin check (BLOCKS THREAD)
     * ONLY use in background threads (e.g. WorkManager, startup splash)
     * NEVER call from UI thread → will freeze app!
     */
    @SuppressLint("NewApi")
    public static boolean isAdminSync() {
        if (!isLoggedIn()) return false;

        try {
            DocumentSnapshot snapshot = getDb().collection("admins")
                    .document(currentUserId())
                    .get()
                    .getResult(); // Blocks thread

            boolean isAdmin = snapshot.exists() && "admin".equals(snapshot.getString("role"));
            cachedIsAdmin = isAdmin;
            return isAdmin;
        } catch (Exception e) {
            Log.e(TAG, "Sync admin check failed", e);
            return false;
        }
    }

    /** Clear admin cache (call after role changes) */
    public static void clearAdminCache() {
        cachedIsAdmin = null;
    }

    /** Interface for async admin check */
    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }
}