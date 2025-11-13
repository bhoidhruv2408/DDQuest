package com.example.ddquest.utils;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.util.List;

/**
 * Utility class for Firebase Storage operations
 * Handles PDF uploads, profile images, file checks, etc.
 */
public class FirebaseStorageUtil {

    private static FirebaseStorage mStorage;
    private static StorageReference mRootRef;

    // Private constructor - prevent instantiation
    private FirebaseStorageUtil() {}

    /** Get FirebaseStorage instance */
    private static FirebaseStorage getStorage() {
        if (mStorage == null) {
            mStorage = FirebaseStorage.getInstance();
            mRootRef = mStorage.getReference();
        }
        return mStorage;
    }

    /** Get root storage reference */
    public static StorageReference getRootRef() {
        if (mRootRef == null) {
            getStorage();
        }
        return mRootRef;
    }

    // ========================
    // UPLOAD PDF
    // ========================
    public static void uploadPdfFile(File file, String fileName, UploadCallback callback) {
        if (!file.exists()) {
            callback.onFailure("File does not exist");
            return;
        }

        StorageReference pdfRef = getRootRef().child(FilePaths.MATERIALS + "/" + fileName);
        Uri fileUri = Uri.fromFile(file);

        UploadTask uploadTask = pdfRef.putFile(fileUri);

        uploadTask
                .addOnSuccessListener(taskSnapshot ->
                        pdfRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()))
                )
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // DOWNLOAD PDF
    // ========================
    public static void downloadPdfFile(String filePath, File localFile, DownloadCallback callback) {
        StorageReference fileRef = getRootRef().child(filePath);

        fileRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> callback.onSuccess(localFile.getAbsolutePath()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // DELETE FILE
    // ========================
    public static void deleteFile(String filePath, DeleteCallback callback) {
        StorageReference fileRef = getRootRef().child(filePath);
        fileRef.delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // CHECK IF FILE EXISTS
    // ========================
    public static void checkFileExists(String filePath, FileExistsCallback callback) {
        StorageReference fileRef = getRootRef().child(filePath);
        fileRef.getDownloadUrl()
                .addOnSuccessListener(uri -> callback.onExists(true, uri.toString()))
                .addOnFailureListener(e -> callback.onExists(false, null));
    }

    // ========================
    // GET DOWNLOAD URL
    // ========================
    public static void getDownloadUrl(String filePath, DownloadUrlCallback callback) {
        StorageReference fileRef = getRootRef().child(filePath);
        fileRef.getDownloadUrl()
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // UPLOAD PROFILE IMAGE
    // ========================
    public static void uploadProfileImage(Uri imageUri, String userId, UploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure("Image URI is null");
            return;
        }

        StorageReference imageRef = getRootRef().child(FilePaths.PROFILE_IMAGES + "/" + userId + ".jpg");
        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()))
                )
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // GET FILE SIZE
    // ========================
    public static void getFileSize(String filePath, FileSizeCallback callback) {
        StorageReference fileRef = getRootRef().child(filePath);
        fileRef.getMetadata()
                .addOnSuccessListener(metadata -> callback.onSuccess(metadata.getSizeBytes()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // LIST FILES IN DIRECTORY
    // ========================
    public static void listFiles(String directoryPath, ListFilesCallback callback) {
        StorageReference dirRef = getRootRef().child(directoryPath);
        dirRef.listAll()
                .addOnSuccessListener(listResult -> callback.onSuccess(listResult.getItems()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================
    // PROGRESS LISTENER
    // ========================
    public static class ProgressListener implements com.google.android.gms.tasks.OnProgressListener<UploadTask.TaskSnapshot> {
        private final ProgressCallback callback;

        public ProgressListener(ProgressCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            callback.onProgress(progress);
        }
    }

    // ========================
    // CALLBACK INTERFACES
    // ========================
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    public interface DownloadCallback {
        void onSuccess(String localFilePath);
        void onFailure(String errorMessage);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface FileExistsCallback {
        void onExists(boolean exists, String downloadUrl);
    }

    public interface DownloadUrlCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    public interface FileSizeCallback {
        void onSuccess(long sizeBytes);
        void onFailure(String errorMessage);
    }

    public interface ListFilesCallback {
        void onSuccess(List<StorageReference> files);
        void onFailure(String errorMessage);
    }

    public interface ProgressCallback {
        void onProgress(double progress);
    }

    // ========================
    // FILE PATHS (ORGANIZED)
    // ========================
    public static class FilePaths {
        public static final String MATERIALS = "materials";
        public static final String PROFILE_IMAGES = "profile_images";
        public static final String DAILY_TESTS = "daily_tests";
        public static final String MOCK_TESTS = "mock_tests";
        public static final String WEEKLY_TESTS = "weekly_tests";

        // Subject-wise materials
        public static String getPhysicsMaterialsPath(String fileName) {
            return MATERIALS + "/physics/" + fileName;
        }

        public static String getMathMaterialsPath(String fileName) {
            return MATERIALS + "/mathematics/" + fileName;
        }

        public static String getChemistryMaterialsPath(String fileName) {
            return MATERIALS + "/chemistry/" + fileName;
        }

        public static String getEnglishMaterialsPath(String fileName) {
            return MATERIALS + "/english/" + fileName;
        }

        public static String getAptitudeMaterialsPath(String fileName) {
            return MATERIALS + "/aptitude/" + fileName;
        }
    }
}