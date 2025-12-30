package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private AutoCompleteTextView actvSemester, actvBranch;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, semesterLayout, branchLayout;
    private CheckBox cbTerms;
    private MaterialButton btnRegister;
    private ImageButton btnBack;
    private TextView tvLoginRedirect;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference rtdb;

    private static final String ADMIN_EMAIL_1 = "bhoidhruv24@gmail.com";
    private static final String ADMIN_EMAIL_2 = "dhobived252@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        rtdb = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupDropdowns();

        btnBack.setOnClickListener(v -> finish());
        tvLoginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        actvSemester = findViewById(R.id.actvSemester);
        actvBranch = findViewById(R.id.actvBranch);
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        semesterLayout = findViewById(R.id.semesterLayout);
        branchLayout = findViewById(R.id.branchLayout);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupDropdowns() {
        String[] semesters = {"1st Semester", "2nd Semester", "3rd Semester", "4th Semester",
                "5th Semester", "6th Semester", "7th Semester", "8th Semester"};
        actvSemester.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesters));

        String[] branches = {
                "Computer Engineering", "Information Technology", "Computer Science & Engineering",
                "Artificial Intelligence & Data Science", "Artificial Intelligence & Machine Learning",
                "Civil Engineering", "Mechanical Engineering", "Electrical Engineering",
                "Electronics & Communication Engineering", "Electrical & Electronics Engineering",
                "Instrumentation & Control Engineering", "Chemical Engineering",
                "Automobile Engineering", "Production Engineering",
                "Biomedical Engineering", "Environmental Engineering", "Mechatronics Engineering",
                "Robotics & Automation",
                "Diploma in Computer Engineering", "Diploma in Civil Engineering",
                "Diploma in Mechanical Engineering", "Diploma in Electrical Engineering",
                "Diploma in Electronics & Communication", "Diploma in Information Technology",
                "Diploma in Chemical Engineering", "Diploma in Automobile Engineering",
                "Other"
        };
        actvBranch.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, branches));
    }

    private void registerUser() {
        if ("Registering...".equals(btnRegister.getText().toString())) return;

        clearErrors();
        String name = getTrimmedText(etName);
        String email = getTrimmedText(etEmail);
        String password = etPassword.getText().toString();
        String semester = getTrimmedText(actvSemester);
        String branch = getTrimmedText(actvBranch);

        // FINAL variables for lambdas
        final String finalSemester = isAdminEmail(email) ? "Admin" : semester;
        final String finalBranch = isAdminEmail(email) ? "Admin" : branch;

        // Hide fields for admin
        if (isAdminEmail(email)) {
            semesterLayout.setVisibility(View.GONE);
            branchLayout.setVisibility(View.GONE);
        } else {
            semesterLayout.setVisibility(View.VISIBLE);
            branchLayout.setVisibility(View.VISIBLE);
        }

        if (!validateInputs(name, email, password, finalSemester, finalBranch, isAdminEmail(email))) return;

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String uid = user.getUid();

                        // Save to Firestore
                        saveToFirestore(uid, name, email, finalSemester, finalBranch);

                        // Save streak to Realtime DB
                        saveStreakToRealtimeDB(uid);

                        // Auto-make admin
                        maybeMakeAdmin(email, uid);

                        // Send verification & go to login
                        sendVerificationAndGoToLogin(user, email);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Registration failed: " + e.getMessage());
                });
    }

    // ADMIN CHECK
    private boolean isAdminEmail(String email) {
        return email.equalsIgnoreCase(ADMIN_EMAIL_1) || email.equalsIgnoreCase(ADMIN_EMAIL_2);
    }

    // VALIDATION
    private boolean validateInputs(String name, String email, String password,
                                   String semester, String branch, boolean isAdmin) {
        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Enter name");
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Valid email required");
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Min 6 characters");
            valid = false;
        }

        if (!isAdmin) {
            if (TextUtils.isEmpty(semester)) {
                semesterLayout.setError("Select semester");
                valid = false;
            }
            if (TextUtils.isEmpty(branch)) {
                branchLayout.setError("Select branch");
                valid = false;
            }
        }

        if (!cbTerms.isChecked()) {
            toast("Please accept Terms & Conditions");
            valid = false;
        }

        return valid;
    }

    private void saveToFirestore(String uid, String name, String email, String semester, String branch) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("semester", semester);
        data.put("branch", branch);
        data.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid).set(data)
                .addOnFailureListener(e -> Log.e("REG", "Firestore save failed", e));
    }

    private void saveStreakToRealtimeDB(String uid) {
        Map<String, Object> streak = new HashMap<>();
        streak.put("current", 0);
        streak.put("lastActive", System.currentTimeMillis());

        rtdb.child("streaks").child(uid).setValue(streak)
                .addOnFailureListener(e -> Log.e("REG", "RTDB save failed", e));
    }

    private void maybeMakeAdmin(String email, String uid) {
        if (isAdminEmail(email)) {
            db.collection("admins").document(uid)
                    .set(java.util.Collections.singletonMap("active", true))
                    .addOnSuccessListener(aVoid -> Log.d("ADMIN", "Admin created: " + email));
        }
    }

    private void sendVerificationAndGoToLogin(FirebaseUser user, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    toast(task.isSuccessful()
                            ? "Verification email sent to " + email
                            : "Failed to send verification email");
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    // UI HELPERS
    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        semesterLayout.setError(null);
        branchLayout.setError(null);
    }

    private String getTrimmedText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getTrimmedText(AutoCompleteTextView actv) {
        return actv.getText() != null ? actv.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "Registering..." : "Register");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}