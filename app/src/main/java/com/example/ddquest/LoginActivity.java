package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private ImageButton btnBack;
    private TextView tvForgotPassword, tvRegisterRedirect;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleSignInResult(task);
                        } else {
                            showProgress(false);
                            toast("Google Sign-In canceled");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        initViews();
        setupGoogleSignIn();
        setupClickListeners();
        checkCurrentUser();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnBack = findViewById(R.id.btnBack);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegisterRedirect = findViewById(R.id.tvRegisterRedirect);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> loginWithEmail());

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        tvRegisterRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = getText(etEmail).trim();
            if (TextUtils.isEmpty(email)) {
                emailLayout.setError("Enter your email");
                return;
            }
            resetPassword(email);
        });
    }

    private void checkCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isEmailVerified()) {
                // Add a small delay to ensure Firebase is initialized
                new android.os.Handler().postDelayed(() -> {
                    checkUserRoleAndNavigate(user.getUid());
                }, 500);
            } else {
                sendVerificationEmail(user);
            }
        }
    }

    private void loginWithEmail() {
        resetErrors();
        String email = getText(etEmail).trim();
        String password = getText(etPassword);

        if (!validateInputs(email, password)) return;

        showProgress(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                toast("Login successful!");
                                // Check user role and navigate
                                checkUserRoleAndNavigate(user.getUid());
                            } else {
                                sendVerificationEmail(user);
                            }
                        }
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        toast(msg);
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toast("Verification email sent! Check inbox (and spam).");
                    } else {
                        toast("Failed to send verification email.");
                    }
                    mAuth.signOut();
                });
    }

    private void signInWithGoogle() {
        showProgress(true);
        Intent intent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(intent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            showProgress(false);
            toast("Google Sign-In failed: " + e.getMessage());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            toast("Google Sign-In successful!");
                            // Create user profile if doesn't exist
                            createUserProfileForGoogle(user);
                            checkUserRoleAndNavigate(user.getUid());
                        }
                    } else {
                        toast("Authentication failed");
                    }
                });
    }

    private void createUserProfileForGoogle(FirebaseUser user) {
        String userId = user.getUid();
        String name = user.getDisplayName();
        String email = user.getEmail();

        if (name == null || name.isEmpty()) {
            name = email != null ? email.split("@")[0] : "User";
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("semester", "Not Set");
        userData.put("branch", "Not Set");
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("LoginActivity", "Google user profile created");
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Failed to create Google user profile", e);
                });
    }

    private void resetPassword(String email) {
        showProgress(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        toast("Password reset email sent!");
                    } else {
                        toast("Failed to send reset email");
                    }
                });
    }

    // === FIXED: Everyone goes to MainActivity ===
    private void checkUserRoleAndNavigate(String uid) {
        if (TextUtils.isEmpty(uid)) {
            navigateToMainActivity("student");
            return;
        }

        // Check if user is admin
        DocumentReference adminDoc = db.collection("admins").document(uid);

        adminDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                boolean isAdmin = snapshot != null && snapshot.exists();

                if (isAdmin) {
                    // For admin users, ensure they have admin profile
                    ensureAdminProfile(uid);
                    navigateToMainActivity("admin");
                } else {
                    // Regular student
                    navigateToMainActivity("student");
                }
            } else {
                Log.e("LOGIN", "Role check failed", task.getException());
                navigateToMainActivity("student");
            }
        });
    }

    private void ensureAdminProfile(String uid) {
        // Check if admin has a profile in users collection
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create admin profile
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String name = user.getDisplayName();
                            String email = user.getEmail();

                            if (name == null || name.isEmpty()) {
                                name = "Admin";
                            }

                            Map<String, Object> adminUserData = new HashMap<>();
                            adminUserData.put("name", name);
                            adminUserData.put("email", email);
                            adminUserData.put("semester", "Admin");
                            adminUserData.put("branch", "Admin");
                            adminUserData.put("role", "admin");
                            adminUserData.put("createdAt", System.currentTimeMillis());

                            db.collection("users").document(uid)
                                    .set(adminUserData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LoginActivity", "Admin profile created");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("LoginActivity", "Failed to create admin profile", e);
                                    });
                        }
                    } else {
                        // Update existing profile to mark as admin
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("semester", "Admin");
                        updates.put("branch", "Admin");
                        updates.put("role", "admin");

                        db.collection("users").document(uid)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("LoginActivity", "Existing profile updated to admin");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("LoginActivity", "Failed to update profile to admin", e);
                                });
                    }
                });
    }

    private void navigateToMainActivity(String role) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Pass role as extra for MainActivity
        intent.putExtra("USER_ROLE", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if ("admin".equals(role)) {
            toast("Welcome, Admin!");
        } else {
            toast("Welcome back!");
        }
        finish();
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Enter email");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Enter password");
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Min 6 characters");
            valid = false;
        }

        return valid;
    }

    private void resetErrors() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkCurrentUser();
    }
}