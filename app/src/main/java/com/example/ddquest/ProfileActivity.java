package com.example.ddquest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private String currentPhotoBase64;
    private Uri photoUri;
    private boolean isEditMode = false;
    private boolean isAdmin = false;

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
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        userId = currentUser.getUid();

        initViews();
        setupToolbar();
        checkAdminAndLoadProfile();
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

    private void checkAdminAndLoadProfile() {
        db.collection("admins").document(userId).get()
                .addOnSuccessListener(adminSnap -> {
                    isAdmin = adminSnap.exists();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e("PROFILE", "Admin check failed", e);
                    loadUserProfile();
                });
    }

    private void loadUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        setInitialsAvatar("??");
                        return;
                    }

                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String semester = documentSnapshot.getString("semester");
                    String branch = documentSnapshot.getString("branch");
                    currentPhotoBase64 = documentSnapshot.getString("photoBase64");

                    etName.setText(name);
                    etEmail.setText(email);
                    etSemester.setText(semester);
                    etBranch.setText(branch);

                    tvUserName.setText(name);
                    tvUserEmail.setText(email);

                    // Admin: show "Admin" and disable edit
                    if (isAdmin) {
                        etSemester.setText("Admin");
                        etBranch.setText("Admin");
                        etSemester.setEnabled(false);
                        etBranch.setEnabled(false);
                    }

                    if (!TextUtils.isEmpty(currentPhotoBase64)) {
                        loadBase64Image(currentPhotoBase64);
                    } else if (!TextUtils.isEmpty(name)) {
                        setInitialsAvatar(name);
                    } else {
                        setInitialsAvatar("??");
                    }
                })
                .addOnFailureListener(e -> {
                    toast("Failed to load profile");
                    setInitialsAvatar("??");
                });
    }

    private void loadBase64Image(String base64) {
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            ivProfile.setImageBitmap(bitmap);
        } catch (Exception e) {
            setInitialsAvatar(etName.getText().toString());
        }
    }

    private void setInitialsAvatar(String fullName) {
        ivProfile.setImageDrawable(new InitialsAvatarDrawable(fullName, getColor(R.color.primary_color)));
    }

    private void loadProfileImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
            ivProfile.setImageBitmap(bitmap);
        } catch (IOException e) {
            toast("Failed to load image");
        }
    }

    private void setupEditMode() {
        findViewById(R.id.layoutEditProfile).setOnClickListener(v -> toggleEditMode());
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        ivCameraIcon.setOnClickListener(v -> openImagePicker());
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
        etName.setEnabled(enable && !isAdmin);
        etEmail.setEnabled(enable && !isAdmin);
        etSemester.setEnabled(enable && !isAdmin);
        etBranch.setEnabled(enable && !isAdmin);

        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        cardEditMode.setVisibility(enable ? View.VISIBLE : View.GONE);
        ivCameraIcon.setVisibility(enable ? View.VISIBLE : View.GONE);

        if (enable) {
            ivCameraIcon.setImageResource(photoUri != null ? R.drawable.ic_check : R.drawable.ic_camera);
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Required");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (photoUri != null) {
            compressAndSavePhoto(name);
        } else {
            saveNameOnly(name);
        }
    }

    private void compressAndSavePhoto(String name) {
        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                runOnUiThread(() -> saveToFirestore(name, base64));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    toast("Image failed");
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                });
            }
        }).start();
    }

    private void saveNameOnly(String name) {
        saveToFirestore(name, null);
    }

    private void saveToFirestore(String name, String newPhotoBase64) {
        db.collection("users").document(userId)
                .update("name", name, "photoBase64", newPhotoBase64 != null ? newPhotoBase64 : currentPhotoBase64)
                .addOnSuccessListener(aVoid -> {
                    toast("Profile updated!");
                    tvUserName.setText(name);
                    setEditMode(false);
                    btnSave.setText("Save");
                    btnSave.setEnabled(true);
                    photoUri = null;
                    if (newPhotoBase64 != null) {
                        currentPhotoBase64 = newPhotoBase64;
                        loadBase64Image(newPhotoBase64);
                    }
                })
                .addOnFailureListener(e -> {
                    toast("Save failed");
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                });
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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}