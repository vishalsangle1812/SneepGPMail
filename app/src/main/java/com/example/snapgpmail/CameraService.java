package com.example.snapgpmail;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraService extends Service {
    private static final String TAG = "CameraService";
    private static final int NOTIFICATION_ID = 101;
    private Camera camera;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Initializing security capture..."));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    AppController.CHANNEL_ID,
                    "Security Captures",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String message) {
        return new NotificationCompat.Builder(this, AppController.CHANNEL_ID)
                .setContentTitle("Security Capture")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_camera)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        captureImage();
        return START_NOT_STICKY;
    }

    private void captureImage() {
        new Thread(() -> {
            try {
                updateNotification("Accessing camera...");
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

                if (camera == null) {
                    throw new RuntimeException("Failed to open camera");
                }

                // Set camera parameters
                Camera.Parameters params = camera.getParameters();
                params.setRotation(90); // Correct orientation for front camera
                camera.setParameters(params);
                camera.setDisplayOrientation(90);

                updateNotification("Ready to capture...");
                camera.takePicture(null, null, (data, camera) -> {
                    try {
                        updateNotification("Saving image...");
                        File imageFile = saveImage(data); // This now calls the properly defined method

                        if (imageFile != null) {
                            String location = "Location unavailable";
                            try {
                                location = LocationService.getLastKnownLocation(CameraService.this);
                            } catch (SecurityException e) {
                                Log.e(TAG, "Location permission not granted", e);
                            }

                            updateNotification("Sending alert...");
                            EmailSender.sendAlertEmail(CameraService.this, imageFile, location);

                            // Make image visible in gallery
                            scanFile(imageFile);
                        }
                    } finally {
                        camera.release();
                        stopSelf();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Camera error", e);
                updateNotification("Error: " + e.getMessage());
                if (camera != null) {
                    camera.release();
                }
                stopSelf();
            }
        }).start();
    }

    /**
     * Saves the captured image to internal storage
     * @param imageData Byte array of the image data
     * @return File object of the saved image, or null if failed
     */
    private File saveImage(byte[] imageData) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "SGP_CAPTURE_" + timeStamp + ".jpg";

        File storageDir = new File(getExternalFilesDir(null), "SecurityCaptures");
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + storageDir.getAbsolutePath());
            return null;
        }

        File imageFile = new File(storageDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageData);
            Log.i(TAG, "Image saved: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    private void scanFile(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    private void updateNotification(String message) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(message));
    }

    @Override
    public void onDestroy() {
        if (camera != null) {
            camera.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}