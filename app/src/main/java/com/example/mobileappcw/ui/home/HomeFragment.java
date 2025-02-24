package com.example.mobileappcw.ui.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappcw.ExistingNoteEditorActivity;
import com.example.mobileappcw.NoteEditorActivity;
import com.example.mobileappcw.R;
import com.example.mobileappcw.ReminderNotificationReceiver;
import com.example.mobileappcw.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<Note> noteList;

    private boolean isAscending = true;

    // Initializes the fragment's view
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase authentication and Firestore database
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Start the ReminderNotificationReceiver service
        Intent serviceIntent = new Intent(getContext(), ReminderNotificationReceiver.class);
        requireContext().startService(serviceIntent);

        // Initialize RecyclerView and its adapter
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        noteList = new ArrayList<>();
        adapter = new NoteAdapter(noteList);
        recyclerView.setAdapter(adapter);

        // Set up spinner for sorting options
        Spinner spinnerSort = root.findViewById(R.id.spinnerSort);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        // Set listener for sorting options spinner
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) parent.getChildAt(0);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(android.R.color.white));
                }
                sortNotes(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set listener for sort order button
        ImageButton btnSortOrder = root.findViewById(R.id.btnSortOrder);
        btnSortOrder.setOnClickListener(v -> {
            isAscending = !isAscending;
            sortNotes(spinnerSort.getSelectedItemPosition());
            updateSortOrderButton();
        });

        // Fetch notes from Firestore
        fetchNotes();

        // Set listener for "Add Note" button
        binding.btnAddNote.setOnClickListener(v -> startActivityForResult(new Intent(getActivity(), NoteEditorActivity.class), 1));

        // Set item click listener for RecyclerView
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    int position = rv.getChildAdapterPosition(child);
                    Note clickedNote = noteList.get(position);
                    Intent intent = new Intent(getActivity(), ExistingNoteEditorActivity.class);
                    intent.putExtra("noteId", clickedNote.getId());
                    startActivityForResult(intent, 1);
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        return root;
    }

    // Sorts the notes based on the selected sorting option
    private void sortNotes(int sortOption) {
        if (sortOption == 0) {
            if (isAscending) {
                Collections.sort(noteList, (note1, note2) -> note1.getDate().compareTo(note2.getDate()));
            } else {
                Collections.sort(noteList, (note1, note2) -> note2.getDate().compareTo(note1.getDate()));
            }
        } else if (sortOption == 1) {
            if (isAscending) {
                Collections.sort(noteList, (note1, note2) -> note1.getTitle().compareTo(note2.getTitle()));
            } else {
                Collections.sort(noteList, (note1, note2) -> note2.getTitle().compareTo(note1.getTitle()));
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Updates the sort order button icon based on the current sorting order
    private void updateSortOrderButton() {
        ImageButton btnSortOrder = binding.getRoot().findViewById(R.id.btnSortOrder);
        if (isAscending) {
            btnSortOrder.setImageResource(R.drawable.ic_arrow_up);
        } else {
            btnSortOrder.setImageResource(R.drawable.ic_arrow_down);
        }
    }

    // Handles the result of activities started for result
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            fetchNotes();
        }
    }

    // Fetches notes from Firestore for the current user
    private void fetchNotes() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("notes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    noteList.clear();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String noteId = documentSnapshot.getId();
                        String title = documentSnapshot.getString("title");
                        String content = documentSnapshot.getString("content");
                        List<String> imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        String date = documentSnapshot.getString("date");
                        noteList.add(new Note(noteId, title, content, imageUrls, date));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch notes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Releases the binding when the view is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // AsyncTask to load images from URLs in the background
    private static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public ImageLoaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (imageViewReference != null && result != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(result);
                }
            }
        }
    }

    // RecyclerView adapter for displaying notes
    private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

        private List<Note> noteList;
        private ImageLoaderTask currentImageLoaderTask;

        public NoteAdapter(List<Note> noteList) {
            this.noteList = noteList;
        }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = noteList.get(position);
            holder.bind(note);
        }

        @Override
        public int getItemCount() {
            return noteList.size();
        }

        // ViewHolder class for individual note items
        public class NoteViewHolder extends RecyclerView.ViewHolder {
            private TextView txtTitle;
            private TextView txtContent;
            private LinearLayout imageContainer;
            private TextView txtDate;
            private ImageView imgDelete;
            private ImageView imgShare;

            public NoteViewHolder(@NonNull View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtContent = itemView.findViewById(R.id.txtContent);
                imageContainer = itemView.findViewById(R.id.imageContainer);
                txtDate = itemView.findViewById(R.id.txtDate);
                imgDelete = itemView.findViewById(R.id.imgDelete);
                imgShare = itemView.findViewById(R.id.imgShare);
            }

            // Binds data to the ViewHolder
            public void bind(Note note) {
                txtTitle.setText(note.getTitle());
                txtContent.setText(note.getContent());
                txtDate.setText(note.getDate());
                displayImages(note.getImageUrls());

                // Set listener for delete button
                imgDelete.setOnClickListener(v -> new AlertDialog.Builder(itemView.getContext())
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteNote(note))
                        .setNegativeButton("No", null)
                        .show());

                // Set listener for share button
                imgShare.setOnClickListener(v -> {
                    try {
                        SharingMethod sharingMethod = new SharingMethod(getActivity());
                        sharingMethod.shareNote(note.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Deletes the note from Firestore
            private void deleteNote(Note note) {
                String noteId = note.getId();
                DocumentReference noteRef = db.collection("notes").document(noteId);

                noteRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Note deleted successfully", Toast.LENGTH_SHORT).show();
                            fetchNotes();
                        })
                        .addOnFailureListener(e -> Toast.makeText(itemView.getContext(), "Error deleting note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            // Displays images in the image container
            private void displayImages(List<String> imageUrls) {
                imageContainer.setVisibility(View.VISIBLE);
                imageContainer.removeAllViews();

                // Set background color of the imageContainer to transparent or any desired color
                imageContainer.setBackgroundColor(Color.TRANSPARENT); // or Color.BLACK, Color.BLUE, etc.

                if (imageUrls != null) {
                    for (String imageUrl : imageUrls) {
                        ImageView imageView = new ImageView(itemView.getContext());
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 90); // Adjust width and height as needed
                        imageView.setLayoutParams(layoutParams);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Crop images to fit uniformly
                        imageView.setPadding(4, 4, 4, 4); // Add padding for the border

                        // Create a drawable with rounded corners
                        GradientDrawable drawable = new GradientDrawable();
                        drawable.setShape(GradientDrawable.RECTANGLE);
                        drawable.setCornerRadius(16); // Adjust the corner radius as needed
                        // Remove setting background color for the drawable to prevent white border

                        // Set the drawable as the background of the ImageView
                        imageView.setBackground(drawable);
                        imageView.setClipToOutline(true); // Clip to rounded corners

                        imageContainer.addView(imageView);
                        new ImageLoaderTask(imageView).execute(imageUrl);
                    }
                }
            }
        }
    }
}
