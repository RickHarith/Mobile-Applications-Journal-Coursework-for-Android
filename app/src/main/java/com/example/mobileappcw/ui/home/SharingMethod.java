package com.example.mobileappcw.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.mobileappcw.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// This code fetches the notes within Firebase to create an image to be shared to the user's contacts
public class SharingMethod {

    private static final String TAG = "SharingMethod";
    private Context context;

    // Constructor to initialize the context
    public SharingMethod(Context context) {
        this.context = context;
    }

    // Method to fetch note details from Firestore and initiate sharing
    public void shareNote(String noteId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference noteRef = db.collection("notes").document(noteId);
        noteRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Note note = documentSnapshot.toObject(Note.class);
                if (note != null) {
                    createContentLayout(note);
                } else {
                    Toast.makeText(context, "Note is null.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Document does not exist.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to fetch note details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Method to create the layout containing note content
    public void createContentLayout(Note note) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate the ScrollView from the XML
        ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.activity_sharing_method, null);

        // Find the LinearLayout inside the ScrollView
        LinearLayout contentLayout = scrollView.findViewById(R.id.noteContentLayout);

        // Now, use contentLayout to add your dynamic content
        TextView titleTextView = contentLayout.findViewById(R.id.noteTitleTextView);
        titleTextView.setText(note.getTitle());

        TextView dateTextView = contentLayout.findViewById(R.id.noteDateTextView);
        dateTextView.setText(note.getDate());

        TextView contentTextView = contentLayout.findViewById(R.id.noteTextView);
        contentTextView.setText(note.getContent());

        // Find the LinearLayout inside the HorizontalScrollView for images
        LinearLayout imageContainer = contentLayout.findViewById(R.id.imageContainer);

        AtomicInteger imagesLoaded = new AtomicInteger();
        List<String> imageUrls = note.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                ImageView imageView = new ImageView(context);
                imageView.setAdjustViewBounds(true);
                imageContainer.addView(imageView);
                loadBitmapFromURL(imageUrl, imageView, imagesLoaded, imageUrls.size(), contentLayout);
            }
        } else {
            // If there are no images, proceed to share the content
            convertLayoutToBitmapAndShare(contentLayout);
        }
    }

    // Method to load bitmap from URL and set it to ImageView
    private void loadBitmapFromURL(String urlString, ImageView imageView, AtomicInteger imagesLoaded, int totalImages, LinearLayout contentLayout) {
        new Thread(() -> {
            try {
                InputStream in = new URL(urlString).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                // Use a predefined max width or calculate based on contentLayout's width if it's available
                int maxWidth = contentLayout.getWidth() > 0 ? contentLayout.getWidth() / totalImages : 200; // Fallback to 200px if width is not available
                int newHeight = (bitmap.getHeight() * maxWidth) / bitmap.getWidth(); // Calculate the new height to maintain aspect ratio
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true);
                ((AppCompatActivity) context).runOnUiThread(() -> {
                    imageView.setImageBitmap(scaledBitmap);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(maxWidth, newHeight));
                    if (imagesLoaded.incrementAndGet() == totalImages) {
                        convertLayoutToBitmapAndShare(contentLayout);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
            }
        }).start();
    }

    // Method to convert layout to bitmap and initiate sharing
    private void convertLayoutToBitmapAndShare(LinearLayout contentLayout) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        contentLayout.measure(View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        contentLayout.layout(0, 0, contentLayout.getMeasuredWidth(), contentLayout.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(contentLayout.getWidth(), contentLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        contentLayout.draw(canvas);

        shareImage(bitmap);
    }

    // Method to share the image bitmap
    private void shareImage(Bitmap bitmap) {
        try {
            File file = new File(context.getExternalCacheDir(), "shared_image.png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            context.startActivity(Intent.createChooser(intent, "Share Image"));
        } catch (IOException e) {
            Log.e(TAG, "Error sharing image", e);
        }
    }
}
