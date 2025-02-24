package com.example.mobileappcw.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobileappcw.databinding.FragmentSlideshowBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

//This code is for the Profile fragment in the application, for editing username and password

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Initialize Firebase authentication and Firestore instances
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inflate the layout for this fragment
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views from the layout
        final EditText usernameEditText = binding.usernameEditText;
        final EditText passwordEditText = binding.passwordEditText;
        final EditText retypePasswordEditText = binding.retypePasswordEditText;
        final Button updateButton = binding.updateButton;

        // Get the current authenticated user
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Fetch the username from Firestore and set it in the usernameEditText
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    usernameEditText.setText(username);
                } else {
                    Toast.makeText(getContext(), "User not found in Firestore.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Error fetching user details.", Toast.LENGTH_SHORT).show());

            // Handle the update button click event
            updateButton.setOnClickListener(v -> {
                String newUsername = usernameEditText.getText().toString();
                String newPassword = passwordEditText.getText().toString();
                String retypePassword = retypePasswordEditText.getText().toString();

                // Check if the new password matches the retyped password
                if (!newPassword.equals(retypePassword)) {
                    Toast.makeText(getContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update username in Firestore
                db.collection("users").document(userId)
                        .update("username", newUsername)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Username updated.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Username update failed.", Toast.LENGTH_SHORT).show());

                // Update password in Firebase Authentication
                user.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Password updated.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Password update failed.", Toast.LENGTH_SHORT).show());
            });
        } else {
            Toast.makeText(getContext(), "No authenticated user found.", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
