package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private MaterialButton btnUploadDaily, btnUploadMock, btnUploadPdf, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupClickListeners();
        setupBackPress(); // Modern way to handle back button
    }

    private void initViews() {
        btnUploadDaily = findViewById(R.id.btnUploadDaily);
        btnUploadMock = findViewById(R.id.btnUploadMock);
        btnUploadPdf = findViewById(R.id.btnUploadPdf);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnUploadDaily.setOnClickListener(v ->
                startActivity(new Intent(this, UploadDailyPracticeActivity.class)));

        btnUploadMock.setOnClickListener(v ->
                startActivity(new Intent(this, UploadMockTestActivity.class)));

        btnUploadPdf.setOnClickListener(v ->
                startActivity(new Intent(this, UploadPdfActivity.class)));

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // MODERN BACK PRESS HANDLING (Replaces deprecated onBackPressed())
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing or show a message
                Toast.makeText(AdminDashboardActivity.this, "Use Logout to exit", Toast.LENGTH_SHORT).show();
                // OR: moveTaskToBack(true); // minimize app
            }
        });
    }
}