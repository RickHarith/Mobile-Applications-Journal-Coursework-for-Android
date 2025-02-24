package com.example.mobileappcw;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//This code is for the user to create reminders in the Planner fragment, in the Calendar View

public class ReminderEditorActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText contentEditText;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedDate;
    private boolean isSaving = false; // To track saving state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_editor);

        Toolbar toolbar = findViewById(R.id.toolbar_editor);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        saveButton = findViewById(R.id.saveButton);
        selectedDate = getIntent().getStringExtra("date");

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        saveButton.setOnClickListener(view -> saveReminder());
    }

    private void saveReminder() {
        if (isSaving) {
            return; // Prevent saving multiple times if the button is pressed rapidly
        }

        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        lockUI(); // Disable UI elements

        String userId = auth.getCurrentUser().getUid();
        String docId = userId + "_" + selectedDate.replace("-", "_");

        Map<String, Object> reminder = new HashMap<>();
        reminder.put("title", title);
        reminder.put("content", content);

        // Convert selectedDate to a proper Firestore Timestamp
        try {
            Timestamp timestamp = new Timestamp(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate));
            reminder.put("date", timestamp);

            db.collection("reminders").document(docId)
                    .set(reminder)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ReminderEditorActivity.this, "Reminder saved", Toast.LENGTH_SHORT).show();
                        finish(); // Navigate back to the Gallery Fragment
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ReminderEditorActivity.this, "Error saving reminder", Toast.LENGTH_SHORT).show();
                        unlockUI(); // Enable UI elements on failure
                    });
        } catch (ParseException e) {
            Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show();
            unlockUI();
        }
    }

    private void lockUI() {
        isSaving = true;
        titleEditText.setEnabled(false);
        contentEditText.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void unlockUI() {
        isSaving = false;
        titleEditText.setEnabled(true);
        contentEditText.setEnabled(true);
        saveButton.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (!isSaving) {
            super.onBackPressed();
        }
        // Do nothing if isSaving is true
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!isSaving) {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
