package com.example.myroutes.ui.manageWalls;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class SelectFromGalleryUtil {
    private static final String TAG = "SelectFromGalleryUtil";
    public static final int GALLERY_REQUEST_CODE = 1;
    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final float MAX_IMAGE_DIM = 1920f;
    private WeakReference<Activity> activity;

    public SelectFromGalleryUtil(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public void requestExternalStoragePermissions() {
        Activity activityVal = activity.get();
        if (activityVal == null) { return; }
        int permission = ActivityCompat.checkSelfPermission(activityVal, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            activityVal.requestPermissions(PERMISSIONS_STORAGE, GALLERY_REQUEST_CODE);
        }
    }

    // Create intent to let user pick image from gallery
    public Intent createSelectFromGalleryIntent() {
        //Create an Intent with action as ACTION_PICK
        Intent intent = new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        return intent;
    }

    // Transform result of gallery picking to bitmap
    public Bitmap getBitmapFromIntent(Intent data) {
        Activity activityVal = this.activity.get();
        if ((activityVal == null) || (data == null)) { return null; }
        // data.getData returns the content URI for the selected Image
        Uri selectedImage = data.getData();
        if (selectedImage == null) {
            Log.e(TAG, "Selected image is null");
            return null;
        }

        // Check if image was downloaded from google photos (not in internal storage)
        String url = selectedImage.toString();
        if (url.startsWith("content://com.google.android.apps.photos.content")) {
            // TODO: try to get image metadata for a google photo
        }

        // Decode image and its metadata
        try {
            // Read in the image into a byte array
            InputStream iStream = activityVal.getContentResolver().openInputStream(selectedImage);
            Bitmap imgBitmap = BitmapFactory.decodeStream(iStream);

            int h = imgBitmap.getHeight();
            int w = imgBitmap.getWidth();

            // Try to get image metadata
            Matrix matrix = new Matrix();
            String imgDecodableString = getImagePath(selectedImage, activityVal);
            if (imgDecodableString != null) {
                // Get the image metadata
                ExifInterface exif = new ExifInterface(imgDecodableString);
                String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

                // Create matrix to rotate bitmap if necessary
                if (orientation != null) { rotateMatrix(matrix, Integer.parseInt(orientation));  }
            }
            // Update matrix to scale bitmap if necessary
            if (w > MAX_IMAGE_DIM) {
                float scale = MAX_IMAGE_DIM / w;
                matrix.postScale(scale, scale);
            }
            imgBitmap = Bitmap.createBitmap(imgBitmap, 0, 0, w, h, matrix, true);

            return imgBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void rotateMatrix(Matrix matrix, int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
        }
    }

    private String getImagePath(Uri selectedImage, Activity activityVal) {
        if (activityVal == null) { return null; }
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        // Get the cursor
        Cursor cursor = activityVal.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null) { return null; }
        // Move to first row
        cursor.moveToFirst();
        //Get the column index of MediaStore.Images.Media.DATA
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        //Gets the String value in the column
        String imgDecodableString = cursor.getString(columnIndex);
        cursor.close();
        return imgDecodableString;
    }
}
