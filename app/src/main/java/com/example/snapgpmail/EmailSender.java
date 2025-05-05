package com.example.snapgpmail;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.File;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailSender {
    private static final String TAG = "SnapGPMail_Email";

    public static void sendAlertEmail(Context context, File imageFile, String location) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String email = prefs.getString("sender_email", "");
        String password = prefs.getString("email_password", "");
        String recipient = prefs.getString("recipient_email", email);

        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "Email credentials not configured");
            return;
        }

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(email, password);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(email));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                message.setSubject("SnapGPMail Alert: Unauthorized Access Detected");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText("Security Alert!\n\n" +
                        "Location: " + location + "\n\n" +
                        "Attached photo captured after multiple failed unlock attempts.");

                MimeBodyPart imagePart = new MimeBodyPart();
                imagePart.setDataHandler(new DataHandler(new FileDataSource(imageFile)));
                imagePart.setFileName("intruder.jpg");

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(imagePart);
                message.setContent(multipart);

                Transport.send(message);
                Log.i(TAG, "Alert email sent successfully");
            } catch (Exception e) {
                Log.e(TAG, "Email failed: " + e.getMessage());
            }
        }).start();
    }
}