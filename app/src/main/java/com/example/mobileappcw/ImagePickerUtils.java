package com.example.mobileappcw;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class ImagePickerUtils {

    // Request code for image pick intent
    public static final int REQUEST_IMAGE_PICK = 2;

    // Dispatches an intent to pick an image from the device's gallery
    public static void dispatchPickImageIntent(Activity activity) {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }

    // Handles the result of the image pick intent
    public static Uri handleActivityResult(int requestCode, int resultCode, Intent data, Activity activity) {
        // Check if the result is OK and the request code matches the image pick request code
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_PICK) {
            // If the data is not null, return the selected image URI
            return data != null ? data.getData() : null;
        }
        // If the result is not OK or the request code does not match, return null
        return null;
    }
}


