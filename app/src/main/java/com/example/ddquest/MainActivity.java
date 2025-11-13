package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ddquest.model.User;
import com.example.ddquest.utils.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvStreak, tvCompletion, tvCurrentSubject,
            tvWeeklyProgress, tvWeeklyGoal;
    private LinearProgressIndicator progressDaily, progressWeekly;
    private MaterialCardView cardDailyPractice, cardMockTest, cardWeeklyTest,
            cardSubjectRotation, cardAnalytics, cardPdfMaterials,
            cardStreak, cardCompletion;

    private final String[] subjects = {"Mathematics", "Physics", "Chemistry", "English", "Aptitude"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        initViews();
        setupToolbar();
        setupClickListeners();
        setupBottomNavigation();
        setTimeBasedGreeting();
        updateWeeklyTestVisibility();
        loadUserData();
    }

    private void initViews() {
        try {
            // Header Views
            tvGreeting = findViewById(R.id.tvGreeting);
            tvUserName = findViewById(R.id.tvUserName);

            // Stats Cards
            cardStreak = findViewById(R.id.cardStreak);
            cardCompletion = findViewById(R.id.cardCompletion);
            tvStreak = findViewById(R.id.tvStreak);
            tvCompletion = findViewById(R.id.tvCompletion);

            // Main Feature Cards
            cardDailyPractice = findViewById(R.id.cardDailyPractice);
            cardMockTest = findViewById(R.id.cardMockTest);
            cardWeeklyTest = findViewById(R.id.cardWeeklyTest);
            cardSubjectRotation = findViewById(R.id.cardSubjectRotation);
            cardAnalytics = findViewById(R.id.cardAnalytics);
            cardPdfMaterials = findViewById(R.id.cardPdfMaterials);

            // Progress Indicators and Text Views
            progressDaily = findViewById(R.id.progressDaily);
            progressWeekly = findViewById(R.id.progressWeekly);
            tvCurrentSubject = findViewById(R.id.tvCurrentSubject);
            tvWeeklyProgress = findViewById(R.id.tvWeeklyProgress);
            tvWeeklyGoal = findViewById(R.id.tvWeeklyGoal);

            // NOTE: tvDailyProgress has been REMOVED from XML
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        try {
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Set up navigation icon click listener
            toolbar.setNavigationOnClickListener(v -> {
                // You can implement a navigation drawer here
                Toast.makeText(MainActivity.this, "Menu clicked", Toast.LENGTH_SHORT).show();
            });

            toolbar.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_logout) {
                    logoutUser();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            // Log toolbar setup error
        }
    }

    private void setTimeBasedGreeting() {
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            String greeting = hour < 12 ? "Good morning!" :
                    hour < 17 ? "Good afternoon!" :
                            "Good evening!";

            if (tvGreeting != null) {
                tvGreeting.setText(greeting);
            }
        } catch (Exception e) {
            // Log time greeting error
        }
    }

    private void loadUserData() {
        try {
            String uid = FirebaseUtil.currentUserId();
            if (uid == null) {
                redirectToLogin();
                return;
            }

            FirebaseUtil.getDb().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                updateUIWithUserData(user);
                            } else {
                                setDefaultData();
                            }
                        } else {
                            setDefaultData();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                        setDefaultData();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
            setDefaultData();
        }
    }

    private void updateUIWithUserData(User user) {
        try {
            // User information
            if (tvUserName != null)
                tvUserName.setText(user.getName() != null ? user.getName() : "Student");

            // Streak and completion
            if (tvStreak != null)
                tvStreak.setText(String.valueOf(user.getStreak()));
            if (tvCompletion != null)
                tvCompletion.setText(user.getCompletion() + "%");

            // Current subject rotation
            if (tvCurrentSubject != null) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                int dayIndex = cal.get(Calendar.DAY_OF_YEAR) % subjects.length;
                tvCurrentSubject.setText(subjects[dayIndex]);
            }

            // Weekly progress calculation
            int weeklyProgress = 0;
            if (user.getStreak() > 0) {
                weeklyProgress = Math.min((user.getStreak() * 100) / 7, 100);
            }

            if (progressWeekly != null)
                progressWeekly.setProgress(weeklyProgress);
            if (tvWeeklyProgress != null)
                tvWeeklyProgress.setText(weeklyProgress + "%");
            if (tvWeeklyGoal != null)
                tvWeeklyGoal.setText("This week's goal: " + user.getStreak() + "/7 days");

            // Daily progress - ONLY update progress bar (text view removed from XML)
            int dailyProgress = user.getDailyProgress() > 0 ? user.getDailyProgress() : 0;
            if (progressDaily != null)
                progressDaily.setProgress(dailyProgress);

        } catch (Exception e) {
            // Log UI update error
        }
    }

    private void setDefaultData() {
        try {
            if (tvUserName != null) tvUserName.setText("Student");
            if (tvStreak != null) tvStreak.setText("0");
            if (tvCompletion != null) tvCompletion.setText("0%");

            // Set current subject
            if (tvCurrentSubject != null) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                int dayIndex = cal.get(Calendar.DAY_OF_YEAR) % subjects.length;
                tvCurrentSubject.setText(subjects[dayIndex]);
            }

            // Reset progress indicators
            if (progressWeekly != null) progressWeekly.setProgress(0);
            if (tvWeeklyProgress != null) tvWeeklyProgress.setText("0%");
            if (tvWeeklyGoal != null) tvWeeklyGoal.setText("This week's goal: 0/7 days");

            if (progressDaily != null) progressDaily.setProgress(0);
        } catch (Exception e) {
            // Log default data error
        }
    }

    private void updateWeeklyTestVisibility() {
        try {
            if (cardWeeklyTest == null) return;

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            int day = cal.get(Calendar.DAY_OF_WEEK);
            boolean isWeekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY;

            cardWeeklyTest.setVisibility(isWeekend ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            // Log visibility update error
        }
    }

    private void setupClickListeners() {
        try {
            cardDailyPractice.setOnClickListener(v ->
                    startActivity(new Intent(this, DailyTestActivity.class)));

            cardMockTest.setOnClickListener(v ->
                    startActivity(new Intent(this, MockTestActivity.class)));

            // Weekly test listener
            cardWeeklyTest.setOnClickListener(v -> {
                if (cardWeeklyTest.getVisibility() == View.VISIBLE) {
                    startActivity(new Intent(this, WeeklyTestActivity.class));
                }
            });

            cardSubjectRotation.setOnClickListener(v ->
                    startActivity(new Intent(this, SubjectRotationActivity.class)));

            cardAnalytics.setOnClickListener(v ->
                    startActivity(new Intent(this, AnalyticsActivity.class)));

            cardPdfMaterials.setOnClickListener(v ->
                    startActivity(new Intent(this, PdfMaterialsActivity.class)));

            cardStreak.setOnClickListener(v -> showStreakDetails());
            cardCompletion.setOnClickListener(v -> showCompletionDetails());
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up click listeners", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStreakDetails() {
        String streak = tvStreak != null ? tvStreak.getText().toString() : "0";
        Toast.makeText(this, "Amazing! " + streak + " day streak! Keep going!", Toast.LENGTH_LONG).show();
    }

    private void showCompletionDetails() {
        String completion = tvCompletion != null ? tvCompletion.getText().toString() : "0%";
        Toast.makeText(this, "You've completed " + completion + " of your learning goals!", Toast.LENGTH_LONG).show();
    }

    private void setupBottomNavigation() {
        try {
            BottomNavigationView nav = findViewById(R.id.bottomNavigation);
            nav.setSelectedItemId(R.id.nav_home);

            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    // Already on home
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutUser() {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseUtil.logout();
                        redirectToLogin();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } catch (Exception e) {
            // Log logout error
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the activity
        loadUserData();
        updateWeeklyTestVisibility();
        setTimeBasedGreeting();

        // Update bottom navigation selection
        try {
            BottomNavigationView nav = findViewById(R.id.bottomNavigation);
            nav.setSelectedItemId(R.id.nav_home);
        } catch (Exception e) {
            // Ignore navigation errors
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is authenticated
        if (FirebaseUtil.currentUserId() == null) {
            redirectToLogin();
        }
    }
}