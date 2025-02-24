package com.example.mobileappcw;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

//This page allows users to edit existing reminder entries in the Planner

public class ReminderViewerActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView contentTextView;
    private FirebaseFirestore db;
    private String reminderId;
    private ImageView deleteIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar_viewer);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        titleTextView = findViewById(R.id.titleTextView);
        contentTextView = findViewById(R.id.contentTextView);
        deleteIcon = findViewById(R.id.ic_garbage_can);

        db = FirebaseFirestore.getInstance();
        reminderId = getIntent().getStringExtra("reminderId");

        if (reminderId != null) {
            loadReminderData();
        }

        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteReminder();
            }
        });
    }

    private void loadReminderData() {
        DocumentReference docRef = db.collection("reminders").document(reminderId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                titleTextView.setText(documentSnapshot.getString("title"));
                contentTextView.setText(documentSnapshot.getString("content"));
            } else {
                Toast.makeText(ReminderViewerActivity.this, "No data found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(ReminderViewerActivity.this, "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private void deleteReminder() {
        DocumentReference docRef = db.collection("reminders").document(reminderId);
        docRef.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(ReminderViewerActivity.this, "Reminder deleted successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(ReminderViewerActivity.this, "Error deleting reminder", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
