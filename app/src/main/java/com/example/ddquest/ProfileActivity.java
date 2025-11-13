package com.example.ddquest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfile;
    private ImageView ivCameraIcon;
    private TextView tvUserName, tvUserEmail;
    private TextInputEditText etName, etEmail, etSemester, etBranch;
    private TextInputLayout nameLayout, emailLayout, semesterLayout, branchLayout;
    private MaterialButton btnEdit, btnSave, btnLogout;
    private View cardEditMode;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private String userId;

    private Uri photoUri;
    private boolean isEditMode = false;
    private String currentPhotoBase64 = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            photoUri = result.getData().getData();
                            loadProfileImage(photoUri);
                            ivCameraIcon.setImageResource(R.drawable.ic_check);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        userId = currentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId);

        initViews();
        setupToolbar();
        loadUserProfile();
        setupEditMode();
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        ivCameraIcon = findViewById(R.id.ivCameraIcon);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etSemester = findViewById(R.id.etSemester);
        etBranch = findViewById(R.id.etBranch);
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        semesterLayout = findViewById(R.id.semesterLayout);
        branchLayout = findViewById(R.id.branchLayout);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);
        cardEditMode = findViewById(R.id.cardEditMode);

        setEditMode(false);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    setInitialsAvatar("??");
                    return;
                }

                String name = getString(snapshot, "name");
                String email = getString(snapshot, "email");
                String semester = getString(snapshot, "semester");
                String branch = getString(snapshot, "branch");
                currentPhotoBase64 = getString(snapshot, "photoBase64");

                etName.setText(name);
                etEmail.setText(email);
                etSemester.setText(semester);
                etBranch.setText(branch);

                tvUserName.setText(name);
                tvUserEmail.setText(email);

                if (!TextUtils.isEmpty(currentPhotoBase64)) {
                    loadBase64Image(currentPhotoBase64);
                } else if (!TextUtils.isEmpty(name)) {
                    setInitialsAvatar(name);
                } else {
                    setInitialsAvatar("??");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                toast("Failed to load profile");
                setInitialsAvatar("??");
            }
        });
    }

    private String getString(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value != null ? value.toString() : "";
    }

    private void loadBase64Image(String base64) {
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            ivProfile.setImageBitmap(bitmap);
        } catch (Exception e) {
            setInitialsAvatar(getStringFromEditText(etName));
        }
    }

    private void setInitialsAvatar(String fullName) {
        if (TextUtils.isEmpty(fullName)) fullName = "??";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        if (parts.length >= 2) {
            initials.append(Character.toUpperCase(parts[0].charAt(0)));
            initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        } else if (parts.length == 1 && parts[0].length() >= 1) {
            initials.append(Character.toUpperCase(parts[0].charAt(0)));
            if (parts[0].length() > 1) initials.append(Character.toUpperCase(parts[0].charAt(1)));
        } else {
            initials.append("??");
        }
        ivProfile.setImageDrawable(new InitialsAvatarDrawable(
                initials.toString(),
                ContextCompat.getColor(this, R.color.primary_color)
        ));
    }

    private void loadProfileImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_person_large)
                .error(R.drawable.ic_person_large)
                .circleCrop()
                .into(ivProfile);
    }

    private void setupEditMode() {
        findViewById(R.id.layoutEditProfile).setOnClickListener(v -> toggleEditMode());
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        ivCameraIcon.setOnClickListener(v -> openImagePicker());

        // Clear errors on text change
        etName.addTextChangedListener(new ErrorClearWatcher(nameLayout));
        etEmail.addTextChangedListener(new ErrorClearWatcher(emailLayout));
        etSemester.addTextChangedListener(new ErrorClearWatcher(semesterLayout));
        etBranch.addTextChangedListener(new ErrorClearWatcher(branchLayout));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setEditMode(isEditMode);
    }

    private void setEditMode(boolean enable) {
        etName.setEnabled(enable);
        etEmail.setEnabled(enable);
        etSemester.setEnabled(enable);
        etBranch.setEnabled(enable);

        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        cardEditMode.setVisibility(enable ? View.VISIBLE : View.GONE);
        ivCameraIcon.setVisibility(enable ? View.VISIBLE : View.GONE);

        if (enable) {
            ivCameraIcon.setImageResource(photoUri != null ? R.drawable.ic_check : R.drawable.ic_camera);
        }
    }

    private void saveProfile() {
        String name = getStringFromEditText(etName).trim();
        String email = getStringFromEditText(etEmail).trim();
        String semester = getStringFromEditText(etSemester).trim();
        String branch = getStringFromEditText(etBranch).trim();

        if (!validateInputs(name, email, semester, branch)) return;

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Update Firebase Auth Email
        currentUser.updateEmail(email)
                .addOnSuccessListener(aVoid -> {
                    // Save to Realtime DB
                    saveToRealtimeDB(name, email, semester, branch);
                })
                .addOnFailureListener(e -> {
                    emailLayout.setError("Failed to update email. Re-login required.");
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }

    private void saveToRealtimeDB(String name, String email, String semester, String branch) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("semester", semester);
        updates.put("branch", branch);

        // Handle photo
        if (photoUri != null) {
            compressAndSavePhoto(updates, name);
        } else {
            // Keep current photo or remove
            if (currentPhotoBase64 != null) {
                updates.put("photoBase64", currentPhotoBase64);
            }
            saveUpdates(updates, name);
        }
    }

    private void compressAndSavePhoto(Map<String, Object> updates, String name) {
        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                updates.put("photoBase64", base64);

                runOnUiThread(() -> saveUpdates(updates, name));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    toast("Image processing failed");
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
            }
        }).start();
    }

    private void saveUpdates(Map<String, Object> updates, String name) {
        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    toast("Profile updated!");
                    tvUserName.setText(name);
                    tvUserEmail.setText(updates.get("email").toString());
                    setEditMode(false);
                    btnSave.setText("Save Changes");
                    btnSave.setEnabled(true);
                    photoUri = null;
                    currentPhotoBase64 = (String) updates.get("photoBase64");
                })
                .addOnFailureListener(e -> {
                    toast("Save failed");
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }

    private boolean validateInputs(String name, String email, String semester, String branch) {
        boolean valid = true;
        if (name.isEmpty()) { nameLayout.setError("Required"); valid = false; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailLayout.setError("Invalid email"); valid = false; }
        if (semester.isEmpty()) { semesterLayout.setError("Required"); valid = false; }
        if (branch.isEmpty()) { branchLayout.setError("Required"); valid = false; }
        return valid;
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private String getStringFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Clear error on typing
    private static class ErrorClearWatcher implements TextWatcher {
        private final TextInputLayout layout;
        ErrorClearWatcher(TextInputLayout layout) { this.layout = layout; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { layout.setError(null); }
    }
}