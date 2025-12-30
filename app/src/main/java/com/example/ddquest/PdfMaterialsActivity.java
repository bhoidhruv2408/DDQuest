package com.example.ddquest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.ddquest.utils.FirebaseUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfMaterialsActivity extends AppCompatActivity {

    // Views
    private TextView tvTotalPdfs, tvDownloaded;
    private MaterialButton btnDownloadPhysics, btnDownloadModernPhysics, btnDownloadMath;
    private TextView tvViewAllPhysics, tvViewAllMath;
    private TabLayout tabLayout;
    private ExtendedFloatingActionButton fabRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_materials);

        initViews();
        setupListeners();
        setupTabs();
        loadStats();
    }

    private void initViews() {
        tvTotalPdfs = findViewById(R.id.tvTotalPdfs);
        tvDownloaded = findViewById(R.id.tvDownloaded);

        btnDownloadPhysics = findViewById(R.id.btnDownloadPhysics);
        btnDownloadModernPhysics = findViewById(R.id.btnDownloadModernPhysics);
        btnDownloadMath = findViewById(R.id.btnDownloadMath);

        // Fixed: Use proper IDs for View All text views
        tvViewAllPhysics = findViewById(R.id.tvViewAllPhysics);
        tvViewAllMath = findViewById(R.id.tvViewAllMath);

        tabLayout = findViewById(R.id.tabLayout);
        fabRefresh = findViewById(R.id.fabRefresh);

        // Toolbar back
        findViewById(R.id.toolbar).setOnClickListener(v -> finishWithAnimation());
    }

    private void setupListeners() {
        // Download buttons
        btnDownloadPhysics.setOnClickListener(v ->
                downloadPdf("physics_formulas.pdf", "Physics Formulas & Laws", btnDownloadPhysics));

        btnDownloadModernPhysics.setOnClickListener(v ->
                downloadPdf("modern_physics.pdf", "Modern Physics", btnDownloadModernPhysics));

        btnDownloadMath.setOnClickListener(v ->
                downloadPdf("algebra_calculus.pdf", "Algebra & Calculus", btnDownloadMath));

        // View All
        tvViewAllPhysics.setOnClickListener(v -> showAllMaterials("Physics"));
        tvViewAllMath.setOnClickListener(v -> showAllMaterials("Mathematics"));

        // FAB
        fabRefresh.setOnClickListener(v -> refreshMaterials());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String subject = tab.getText() != null ? tab.getText().toString() : "All";
                filterBySubject(subject);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void downloadPdf(String fileName, String title, MaterialButton button) {
        button.setEnabled(false);
        button.setText("Downloading...");

        StorageReference ref = FirebaseUtil.getStorage().getReference()
                .child("materials")
                .child(fileName);

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "DDQuest");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String localFileName = title.replace(" ", "") + "" + timestamp + ".pdf";
        File file = new File(dir, localFileName);

        FileDownloadTask downloadTask = ref.getFile(file);

        downloadTask.addOnSuccessListener(taskSnapshot -> {
            button.setEnabled(true);
            button.setText("Download");
            Toast.makeText(this, title + " downloaded successfully!", Toast.LENGTH_SHORT).show();
            updateDownloadedCount();
            showDownloadDialog(file, title);
        }).addOnFailureListener(e -> {
            button.setEnabled(true);
            button.setText("Download");
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }).addOnProgressListener(snapshot -> {
            // Show download progress if needed
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            // You can update progress here
        });
    }

    private void showDownloadDialog(File file, String title) {
        new AlertDialog.Builder(this)
                .setTitle("Download Complete")
                .setMessage(title + " has been downloaded. Do you want to open it now?")
                .setPositiveButton("Open", (dialog, which) -> openPdf(file))
                .setNegativeButton("Later", null)
                .setNeutralButton("Show Folder", (dialog, which) -> showInFolder(file))
                .show();
    }

    private void openPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showInFolder(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath());
            intent.setDataAndType(uri, "resource/folder");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "File saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "File saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadStats() {
        tvTotalPdfs.setText("24");
        tvDownloaded.setText("12");
        // TODO: Load from Firestore
    }

    private void updateDownloadedCount() {
        try {
            int count = Integer.parseInt(tvDownloaded.getText().toString()) + 1;
            tvDownloaded.setText(String.valueOf(count));
        } catch (Exception e) {
            tvDownloaded.setText("1");
        }
    }

    private void filterBySubject(String subject) {
        View physics = findViewById(R.id.physicsSection);
        View math = findViewById(R.id.mathSection);

        switch (subject) {
            case "All":
                physics.setVisibility(View.VISIBLE);
                math.setVisibility(View.VISIBLE);
                break;
            case "Physics":
                physics.setVisibility(View.VISIBLE);
                math.setVisibility(View.GONE);
                break;
            case "Math":
                physics.setVisibility(View.GONE);
                math.setVisibility(View.VISIBLE);
                break;
            case "Chemistry":
            case "English":
                physics.setVisibility(View.GONE);
                math.setVisibility(View.GONE);
                Toast.makeText(this, subject + " materials coming soon!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showAllMaterials(String subject) {
        Toast.makeText(this, "Showing all " + subject + " materials", Toast.LENGTH_SHORT).show();
        // TODO: Start SubjectMaterialsActivity here
        // Intent intent = new Intent(this, SubjectMaterialsActivity.class);
        // intent.putExtra("SUBJECT", subject);
        // startActivity(intent);
    }

    private void refreshMaterials() {
        fabRefresh.setText("Refreshing...");
        // Simulate refresh
        new Handler().postDelayed(() -> {
            fabRefresh.setText("Refresh");
            loadStats();
            Toast.makeText(this, "Materials refreshed!", Toast.LENGTH_SHORT).show();
        }, 800);
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }




}