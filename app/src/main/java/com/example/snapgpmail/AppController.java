package com.example.snapgpmail;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class AppController extends Application {
    public static final String CHANNEL_ID = "security_capture_channel";
    private static final String LOG_FILE = "snapgp_logs.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Security Captures",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for security captures");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    public static void logToFile(Context context, String message) {
        try {
            File logFile = new File(context.getExternalFilesDir(null), LOG_FILE);
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((new Date() + ": " + message + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("FileLog", "Error writing log", e);
        }
    }
}