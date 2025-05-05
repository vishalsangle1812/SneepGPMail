package com.example.snapgpmail;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
public class LoginAttemptReceiver extends BroadcastReceiver {
    private static final String TAG = "LoginAttemptReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Get configured values
        int maxAttempts = Integer.parseInt(
                prefs.getString("attempt_threshold", "3"));
        String senderEmail = prefs.getString("sender_email", "");
        String appPassword = prefs.getString("app_password", "");

        int attempts = prefs.getInt("failed_attempts", 0);

        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            // Reset on successful unlock
            prefs.edit().putInt("failed_attempts", 0).apply();
        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            attempts++;
            prefs.edit().putInt("failed_attempts", attempts).apply();

            if (attempts >= maxAttempts) {
                if (!senderEmail.isEmpty() && !appPassword.isEmpty()) {
                    Intent serviceIntent = new Intent(context, CameraService.class);
                    context.startService(serviceIntent);
                } else {
                    Log.e(TAG, "Email not configured - cannot send alerts");
                }
            }
        }
    }

    private void startCameraService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, CameraService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.i(TAG, "Camera service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera service", e);
            showNotification(context, "Error", "Failed to start camera: " + e.getMessage());
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppController.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

