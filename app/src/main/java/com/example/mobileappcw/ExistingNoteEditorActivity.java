package com.example.mobileappcw;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

//This code is for editing existing journal entries within the app

public class ExistingNoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private String noteId;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_note);

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        progressBar = findViewById(R.id.progressBar);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);

        // Get the note ID passed from the previous activity
        noteId = getIntent().getStringExtra("noteId");

        db = FirebaseFirestore.getInstance();

        // Retrieve the note details from Firestore
        getNoteDetails();

        // Set click listener for Save Note button
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNote();
            }
        });
    }

    private void getNoteDetails() {
        DocumentReference noteRef = db.collection("notes").document(noteId);
        noteRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Retrieve note details
                String title = documentSnapshot.getString("title");
                String content = documentSnapshot.getString("content");

                // Set the retrieved details to the EditText fields
                etNoteTitle.setText(title);
                etNoteContent.setText(content);
            } else {
                // Note does not exist
                Toast.makeText(ExistingNoteEditorActivity.this, "Note not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            // Error occurred while retrieving note details
            Toast.makeText(ExistingNoteEditorActivity.this, "Failed to get note details", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateNote() {
        String newTitle = etNoteTitle.getText().toString().trim();
        String newContent = etNoteContent.getText().toString().trim();

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Update note in Firestore
        DocumentReference noteRef = db.collection("notes").document(noteId);
        noteRef.update("title", newTitle, "content", newContent)
                .addOnSuccessListener(aVoid -> {
                    // Note updated successfully
                    Toast.makeText(ExistingNoteEditorActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    setResult(Activity.RESULT_OK); // Set result OK
                    finish(); // Finish the activity
                })
                .addOnFailureListener(e -> {
                    // Error updating note
                    Toast.makeText(ExistingNoteEditorActivity.this, "Failed to update note", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
