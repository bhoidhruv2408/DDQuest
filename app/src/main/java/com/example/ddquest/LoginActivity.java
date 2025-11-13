package com.example.ddquest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private ImageButton btnBack;
    private TextView tvForgotPassword, tvRegisterRedirect;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
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
        initViews();
        setupGoogleSignIn();          // <-- now uses correct web-client-id
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

    /***  IMPORTANT – uses the web client id from strings.xml  ***/
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))   // <-- FIXED
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
        if (user != null && user.isEmailVerified()) {
            navigateToHome();
        }
    }

    private void loginWithEmail() {
        resetErrors();
        String email = getText(etEmail).trim();
        String password = getText(etPassword).trim();

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
                                navigateToHome();
                            } else {
                                // *** SEND VERIFICATION EMAIL HERE ***
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

    /***  NEW METHOD – sends verification mail + shows nice toast  ***/
    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toast("Verification email sent! Check your inbox (and spam).");
                    } else {
                        toast("Failed to send verification email.");
                    }
                    mAuth.signOut();   // force user to verify before login
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
                        toast("Google Sign-In successful!");
                        navigateToHome();
                    } else {
                        toast("Authentication failed");
                    }
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

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkCurrentUser();
    }
}