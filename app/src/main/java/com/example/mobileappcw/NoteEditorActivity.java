package com.example.mobileappcw;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappcw.databinding.ActivityNoteEditorBinding;
import com.example.mobileappcw.ImagePickerUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

//This code is for creating a new journal entry in the Home fragment

public class NoteEditorActivity extends AppCompatActivity {

    private ActivityNoteEditorBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private List<Uri> selectedImageUris; // List to store selected image URIs
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        selectedImageUris = new ArrayList<>(); // Initialize the list

        binding.btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Disable the Save button to prevent multiple clicks
                binding.btnSaveNote.setEnabled(false);
                binding.btnBack.setEnabled(false);
                binding.btnGallery.setEnabled(false);

                // Get title and content
                String title = binding.etNoteTitle.getText().toString().trim();
                String content = binding.etNoteContent.getText().toString().trim();

                // Check if title and content are empty
                if (title.isEmpty() || content.isEmpty()) {
                    // Display a message to the user
                    Toast.makeText(NoteEditorActivity.this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();

                    // Re-enable the buttons
                    binding.btnSaveNote.setEnabled(true);
                    binding.btnBack.setEnabled(true);
                    binding.btnGallery.setEnabled(true);

                    // Reset the saving flag
                    isSaving = false;
                } else {
                    // Check if saving is already in progress
                    if (!isSaving) {
                        // Set saving in progress flag to true
                        isSaving = true;
                        // Show progress bar
                        binding.progressBar.setVisibility(View.VISIBLE);
                        // Save note with images
                        saveNoteWithImages();
                    }
                }
            }
        });


        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePickerUtils.dispatchPickImageIntent(NoteEditorActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ImagePickerUtils.REQUEST_IMAGE_PICK) {
                Uri selectedImageUri = ImagePickerUtils.handleActivityResult(requestCode, resultCode, data, this);
                if (selectedImageUri != null) {
                    selectedImageUris.add(selectedImageUri); // Add the selected image URI to the list
                    displayImagePreviews(); // Display image previews
                } else {
                    Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void displayImagePreviews() {
        binding.imagePreviewLayout.removeAllViews(); // Clear existing previews
        for (Uri uri : selectedImageUris) {
            // Create a FrameLayout to hold each image preview and its overlay
            FrameLayout frameLayout = new FrameLayout(this);
            LinearLayout.LayoutParams frameLayoutParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.image_preview_size),
                    getResources().getDimensionPixelSize(R.dimen.image_preview_size)
            );
            frameLayoutParams.setMargins(8, 0, 8, 0);
            frameLayout.setLayoutParams(frameLayoutParams);

            // Create the ImageView for the image preview
            ImageView imageView = new ImageView(this);
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setImageURI(uri);
            frameLayout.addView(imageView);

            // Create a transparent overlay
            View overlay = new View(this);
            FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            overlay.setLayoutParams(overlayParams);
            overlay.setBackgroundColor(getResources().getColor(android.R.color.black)); // Set transparent black color
            overlay.setAlpha(0.5f); // Set transparency level
            frameLayout.addView(overlay);

            // Create the ImageView for the remove icon
            ImageView removeIcon = new ImageView(this);
            removeIcon.setImageResource(R.drawable.ic_garbage_can);
            removeIcon.setColorFilter(Color.RED); // Set the color of the icon to red
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.remove_icon_size),
                    getResources().getDimensionPixelSize(R.dimen.remove_icon_size),
                    Gravity.CENTER // Center in the FrameLayout
            );
            removeIcon.setLayoutParams(iconParams);
            frameLayout.addView(removeIcon);

            // Set click listener to remove the image
            removeIcon.setOnClickListener(v -> {
                selectedImageUris.remove(uri); // Remove the image URI from the list
                displayImagePreviews(); // Refresh the image previews
            });

            binding.imagePreviewLayout.addView(frameLayout);
        }
        binding.imagePreviewLayout.setVisibility(View.VISIBLE); // Make the preview layout visible
    }


    private void saveNoteWithImages() {
        // Check if there are any selected images to save
        if (selectedImageUris.isEmpty()) {
            // No images to save, so save the note without images
            saveNoteWithoutImage();
            return;
        }

        // Upload each image to Firebase Storage and save their download URLs
        List<String> imageUrls = new ArrayList<>();

        // Track number of images uploaded
        AtomicInteger imagesUploaded = new AtomicInteger(0);

        // Iterate over each selected image URI
        for (Uri imageUri : selectedImageUris) {
            // Generate a random image name
            String imageName = String.valueOf(System.currentTimeMillis());

            // Create a reference to 'images/userId/noteId/imageName.jpg'
            String userId = mAuth.getCurrentUser().getUid();
            String noteId = db.collection("notes").document().getId(); // Create a new note document
            String imagePath = "images/" + userId + "/" + noteId + "/" + imageName + ".jpg";

            // Create a reference to the image file in Firestore Storage
            StorageReference imageRef = storage.getReference().child(imagePath);

            // Upload the image file to Firestore Storage
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Image uploaded successfully, now get the download URL
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Image download URL retrieved, add it to the list of image URLs
                            imageUrls.add(uri.toString());

                            // Check if all images have been uploaded
                            if (imagesUploaded.incrementAndGet() == selectedImageUris.size()) {
                                // All images uploaded, save the note with image URLs
                                saveNoteWithImageUrls(imageUrls);
                            }
                        }).addOnFailureListener(e -> {
                            // Error getting image download URL
                            Toast.makeText(NoteEditorActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Error uploading image to Firestore Storage
                        Toast.makeText(NoteEditorActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveNoteWithImageUrls(List<String> imageUrls) {
        // Create a new note with image URLs
        String title = binding.etNoteTitle.getText().toString().trim();
        String content = binding.etNoteContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Get current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        note.put("userId", userId);
        note.put("imageUrls", imageUrls); // Add the list of image URLs to the note
        note.put("date", currentDate); // Add the current date to the note

        // Save the note details to Firestore
        db.collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NoteEditorActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NoteEditorActivity.this, "Failed to save note", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNoteWithoutImage() {
        String title = binding.etNoteTitle.getText().toString().trim();
        String content = binding.etNoteContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Get current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        note.put("userId", userId);
        note.put("date", currentDate); // Add the current date to the note

        db.collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NoteEditorActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NoteEditorActivity.this, "Failed to save note", Toast.LENGTH_SHORT).show();
                });
    }
}
