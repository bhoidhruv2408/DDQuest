package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initViews();

        // Setup dropdowns
        setupDropdowns();

        // Click listeners
        btnBack.setOnClickListener(v -> finish());

        tvLoginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
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
        // Semester
        String[] semesters = {"1st Semester", "2nd Semester", "3rd Semester", "4th Semester",
                "5th Semester", "6th Semester", "7th Semester", "8th Semester"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesters);
        actvSemester.setAdapter(semesterAdapter);

        // Branch
        String[] branches = {
                // Computer
                "Computer Engineering", "Information Technology", "Computer Science & Engineering",
                "Artificial Intelligence & Data Science", "Artificial Intelligence & Machine Learning",

                // Core
                "Civil Engineering", "Mechanical Engineering", "Electrical Engineering",
                "Electronics & Communication Engineering", "Electrical & Electronics Engineering",
                "Instrumentation & Control Engineering", "Chemical Engineering",
                "Automobile Engineering", "Production Engineering",

                // Other
                "Biomedical Engineering", "Environmental Engineering", "Mechatronics Engineering",
                "Robotics & Automation",

                // Diploma
                "Diploma in Computer Engineering", "Diploma in Civil Engineering",
                "Diploma in Mechanical Engineering", "Diploma in Electrical Engineering",
                "Diploma in Electronics & Communication", "Diploma in Information Technology",
                "Diploma in Chemical Engineering", "Diploma in Automobile Engineering",

                "Other"
        };

        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, branches);
        actvBranch.setAdapter(branchAdapter);
    }

    private void registerUser() {
        // Prevent double click
        if ("Registering...".equals(btnRegister.getText().toString())) {
            return;
        }

        // Reset errors
        clearErrors();

        String name = getTrimmedText(etName);
        String email = getTrimmedText(etEmail);
        String password = etPassword.getText().toString();
        String semester = getTrimmedText(actvSemester);
        String branch = getTrimmedText(actvBranch);

        if (!validateInputs(name, email, password, semester, branch)) {
            return;
        }

        // Show loading
        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserDataToDatabase(user.getUid(), name, email, semester, branch, user);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        semesterLayout.setError(null);
        branchLayout.setError(null);
    }

    private String getTrimmedText(TextInputEditText editText) {
        return editText.getText().toString().trim();
    }

    private String getTrimmedText(AutoCompleteTextView actv) {
        return actv.getText().toString().trim();
    }

    private boolean validateInputs(String name, String email, String password, String semester, String branch) {
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Please enter your full name");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Please enter your email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Please enter a password");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(semester)) {
            semesterLayout.setError("Please select a semester");
            isValid = false;
        }

        if (TextUtils.isEmpty(branch)) {
            branchLayout.setError("Please select a branch");
            isValid = false;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms & Conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Registering..." : "Register");
    }

    private void saveUserDataToDatabase(String userId, String name, String email, String semester, String branch, FirebaseUser firebaseUser) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("semester", semester);
        userData.put("branch", branch);
        userData.put("createdAt", System.currentTimeMillis());

        mDatabase.child("users").child(userId).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Send verification email
                    firebaseUser.sendEmailVerification()
                            .addOnSuccessListener(aVoid1 -> Toast.makeText(this,
                                    "Verification email sent to " + email, Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Failed to send verification email.", Toast.LENGTH_SHORT).show());

                    // Navigate to Login
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}