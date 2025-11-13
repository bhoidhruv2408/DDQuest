package com.example.ddquest.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Centralised Firebase utilities - singleton pattern
 * Safe, null-safe, and used across the app
 */
public class FirebaseUtil {

    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;

    // Private constructor to prevent instantiation
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
    }

    /** Check if user is logged in */
    public static boolean isLoggedIn() {
        return getAuth().getCurrentUser() != null;
    }
}