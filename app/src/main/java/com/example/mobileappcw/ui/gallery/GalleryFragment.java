package com.example.mobileappcw.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobileappcw.R;
import com.example.mobileappcw.ReminderEditorActivity;
import com.example.mobileappcw.ReminderViewerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;


//This file is for the Planner fragment within the application, this is because the navigation view drawers template was used
public class GalleryFragment extends Fragment {

    private CalendarView calendarView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // This function is called when the fragment is being created and initialized
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Initialize Firebase authentication and Firestore database
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find the CalendarView from the layout
        calendarView = root.findViewById(R.id.calendarView);

        // Set the minimum date allowed to be selected on the CalendarView to today's date
        Calendar calendar = Calendar.getInstance();
        calendarView.setMinDate(calendar.getTimeInMillis());

        // Set a listener for when the date is changed on the CalendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Generate a unique document ID for the reminder based on user ID and selected date
            String userId = auth.getCurrentUser().getUid();
            String docId = userId + "_" + year + "_" + (month + 1) + "_" + dayOfMonth;

            // Check if a reminder exists for the selected date in Firestore
            db.collection("reminders").document(docId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    // If a reminder exists, start the ReminderViewerActivity to view the reminder details
                    Intent intent = new Intent(getContext(), ReminderViewerActivity.class);
                    intent.putExtra("reminderId", docId);
                    startActivity(intent);
                } else {
                    // If no reminder exists, start the ReminderEditorActivity to create a new reminder
                    Intent intent = new Intent(getContext(), ReminderEditorActivity.class);
                    intent.putExtra("date", year + "-" + (month + 1) + "-" + dayOfMonth);
                    startActivity(intent);
                }
            });
        });

        // Return the root view of the fragment
        return root;
    }
}
