package com.example.ddquest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UploadPdfActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etSubject;
    private TextInputLayout titleLayout, subjectLayout;
    private MaterialButton btnSelectPdf, btnUpload;
    private Uri pdfUri;

    private final ActivityResultLauncher<Intent> pdfPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pdfUri = result.getData().getData();
                    btnSelectPdf.setText("PDF Selected");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_pdf);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etSubject = findViewById(R.id.etSubject);
        titleLayout = findViewById(R.id.titleLayout);
        subjectLayout = findViewById(R.id.subjectLayout);
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnUpload = findViewById(R.id.btnUpload);
    }

    private void setupClickListeners() {
        btnSelectPdf.setOnClickListener(v -> openPdfPicker());
        btnUpload.setOnClickListener(v -> uploadPdf());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        pdfPicker.launch(intent);
    }

    private void uploadPdf() {
        String title = etTitle.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();

        if (TextUtils.isEmpty(title)) { titleLayout.setError("Required"); return; }
        if (TextUtils.isEmpty(subject)) { subjectLayout.setError("Required"); return; }
        if (pdfUri == null) { toast("Select PDF"); return; }

        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");

        String fileName = System.currentTimeMillis() + ".pdf";
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("materials").child(fileName);

        ref.putFile(pdfUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveToFirestore(title, subject, uri.toString(), fileName))
                        .addOnFailureListener(e -> fail("URL failed")))
                .addOnFailureListener(e -> fail("Upload failed"));
    }

    private void saveToFirestore(String title, String subject, String url, String fileName) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("subject", subject);
        data.put("url", url);
        data.put("fileName", fileName);
        data.put("uploadedBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("uploadedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("materials")
                .add(data)
                .addOnSuccessListener(doc -> success())
                .addOnFailureListener(e -> fail("Save failed"));
    }

    private void success() {
        toast("PDF uploaded!");
        finish();
    }

    private void fail(String msg) {
        toast(msg);
        btnUpload.setEnabled(true);
        btnUpload.setText("Upload");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}