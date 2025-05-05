package com.example.snapgpmail;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Handles sending security alert emails with attached images
 * Features:
 * - Offline queuing with WorkManager
 * - Firebase Cloud Functions integration
 * - Automatic retries for failed sends
 * - Image upload to Firebase Storage
 */
public class EmailSender {
    private static final String TAG = "EmailSender";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final Context context;
    private final FirebaseFunctions firebaseFunctions;
    private final StorageReference storageReference;
    private final Executor executor;
    private final WorkManager workManager;

    public EmailSender(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseFunctions = FirebaseFunctions.getInstance();
        this.storageReference = FirebaseStorage.getInstance().getReference();
        this.executor = Executors.newSingleThreadExecutor();
        this.workManager = WorkManager.getInstance(this.context);
    }

    /**
     * Main method to send security alert
     * @param recipientEmail Target email address
     * @param location Device location string
     * @param imageUri URI of captured image
     */
    public void sendSecurityAlert(String recipientEmail, String location, Uri imageUri) {
        executor.execute(() -> {
            String subject = context.getString(R.string.email_subject);
            String body = createEmailBody(location);

            if (NetworkUtils.isNetworkAvailable(context)) {
                sendEmailWithImage(recipientEmail, subject, body, imageUri);
            } else {
                queueEmail(new EmailData(recipientEmail, subject, body, imageUri.toString()));
                Log.i(TAG, "Network unavailable - email queued");
            }
        });
    }

    private String createEmailBody(String location) {
        return context.getString(R.string.email_body_header) + "\n\n" +
                context.getString(R.string.email_body_location, location) + "\n\n" +
                context.getString(R.string.email_body_footer);
    }

    /**
     * Sends email with image attachment via Firebase
     */
    private void sendEmailWithImage(String email, String subject, String body, Uri imageUri) {
        String imagePath = "security_images/" + UUID.randomUUID() + ".jpg";
        StorageReference imageRef = storageReference.child(imagePath);

        imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .continueWithTask(downloadUrlTask -> {
                    Uri downloadUrl = downloadUrlTask.getResult();
                    Map<String, Object> data = new HashMap<>();
                    data.put("email", email);
                    data.put("subject", subject);
                    data.put("body", body);
                    data.put("imageUrl", downloadUrl.toString());

                    return firebaseFunctions
                            .getHttpsCallable("sendSecurityAlert")
                            .call(data);
                })
                .addOnCompleteListener(executor, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email sent successfully");
                    } else {
                        handleSendFailure(email, subject, body, imageUri.toString(),
                                task.getException());
                    }
                });
    }

    private void handleSendFailure(String email, String subject, String body,
                                   String imagePath, Exception exception) {
        Log.e(TAG, "Email sending failed", exception);

        if (shouldRetry(exception)) {
            queueEmail(new EmailData(email, subject, body, imagePath));
        } else {
            Log.w(TAG, "Max retry attempts reached for email to " + email);
        }
    }

    private boolean shouldRetry(Exception exception) {
        // Implement your retry logic based on exception type
        return true; // Simplified for example
    }

    /**
     * Queues email for later sending when offline
     */
    private void queueEmail(EmailData emailData) {
        Data inputData = new Data.Builder()
                .putString("email", emailData.recipient)
                .putString("subject", emailData.subject)
                .putString("body", emailData.body)
                .putString("imagePath", emailData.imagePath)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest emailWork = new OneTimeWorkRequest.Builder(EmailWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(emailWork);
        Log.i(TAG, "Email queued with WorkManager ID: " + emailWork.getId());
    }

    /**
     * Worker class for handling queued emails
     */
    public static class EmailWorker extends Worker {
        public EmailWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Data inputData = getInputData();
            EmailData emailData = new EmailData(
                    inputData.getString("email"),
                    inputData.getString("subject"),
                    inputData.getString("body"),
                    inputData.getString("imagePath")
            );

            try {
                EmailSender sender = new EmailSender(getApplicationContext());
                sender.sendEmailWithImage(
                        emailData.recipient,
                        emailData.subject,
                        emailData.body,
                        Uri.parse(emailData.imagePath)
                );
                return Result.success();
            } catch (Exception e) {
                Log.e(TAG, "EmailWorker failed", e);
                return Result.retry();
            }
        }
    }

    /**
     * Data container for email information
     */
    private static class EmailData {
        final String recipient;
        final String subject;
        final String body;
        final String imagePath;

        EmailData(String recipient, String subject, String body, String imagePath) {
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
            this.imagePath = imagePath;
        }
    }
}