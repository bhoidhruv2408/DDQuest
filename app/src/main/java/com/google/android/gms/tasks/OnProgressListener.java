package com.google.android.gms.tasks;

import androidx.annotation.NonNull;

import com.google.firebase.storage.UploadTask;

public interface OnProgressListener<T> {
    void onProgress(@NonNull UploadTask.TaskSnapshot snapshot);
}
