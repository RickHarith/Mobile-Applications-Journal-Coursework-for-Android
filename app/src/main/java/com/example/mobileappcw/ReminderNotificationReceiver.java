package com.example.mobileappcw;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mobileappcw.ui.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

//This is a service that generates a local notification for any reminders made in the Planner

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class ReminderNotificationReceiver extends FirebaseMessagingService {

    private static final String TAG = "ReminderNotification";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Call fetchAndHandleReminders when the service is created
        fetchAndHandleReminders();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("body");
            // Passing current date as reminderDate for FCM messages for demonstration
            sendNotification(title, message, new Date());
        }
    }

    private void fetchAndHandleReminders() {
        // Check if the user is authenticated
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            Log.d(TAG, "User UID: " + userId);

            // Query the reminders collection for documents starting with the user's ID
            db.collection("reminders")
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), userId)
                    .whereLessThanOrEqualTo(FieldPath.documentId(), userId + "\uf8ff")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean found = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    Log.d(TAG, "Reminder document exists: " + document.getId());
                                    handleReminders(document);
                                    found = true;
                                }
                            }
                            if (!found) {
                                Log.d(TAG, "No reminders found for user: " + userId);
                                Toast.makeText(this, "No reminders found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch reminders", task.getException());
                            Toast.makeText(this, "Failed to fetch reminders: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Handle the case where the user is not authenticated
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleReminders(@NonNull DocumentSnapshot document) {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Accessing reminder's fields directly from the document
        String title = document.getString("title");
        Date reminderDate = document.getDate("date");
        String content = document.getString("content");

        // Logging the fetched values
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Content: " + content);
        Log.d(TAG, "Reminder Date: " + (reminderDate != null ? reminderDate.toString() : "null"));

        // Remove time from currentDate
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date adjustedCurrentDate = calendar.getTime();

        // Adjusting reminderDate to remove time
        Calendar reminderCal = Calendar.getInstance();
        reminderCal.setTime(reminderDate);
        reminderCal.set(Calendar.HOUR_OF_DAY, 0);
        reminderCal.set(Calendar.MINUTE, 0);
        reminderCal.set(Calendar.SECOND, 0);
        reminderCal.set(Calendar.MILLISECOND, 0);
        Date adjustedReminderDate = reminderCal.getTime();

        // Calculate the date two days before the reminder
        Calendar twoDaysBeforeReminderCal = Calendar.getInstance();
        twoDaysBeforeReminderCal.setTime(adjustedReminderDate);
        twoDaysBeforeReminderCal.add(Calendar.DAY_OF_MONTH, -2);
        Date twoDaysBeforeReminder = twoDaysBeforeReminderCal.getTime();

        // Check if the current date is during or two days before the reminder date
        if (!adjustedCurrentDate.before(twoDaysBeforeReminder) && !adjustedCurrentDate.after(adjustedReminderDate)) {
            sendNotification(title, content, reminderDate);
        }
    }

    private void sendNotification(String title, String message, Date reminderDate) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "YourChannelId";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "YourChannelName";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }

        // Format the reminder date as a string
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(reminderDate);

        // Update the message to include the reminder date
        String fullMessage = "J-planner here! You set a reminder on " + formattedDate + "\n" + title + "\n" + message;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)  // replace with your own icon
                .setContentTitle("Reminder for " + formattedDate)  // Set the title to show the reminder date
                .setContentText(fullMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullMessage))  // Use BigTextStyle for longer texts
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Create a unique int for each notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}

