package com.example.myroutes.db.backends;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myroutes.db.Result;
import com.example.myroutes.db.entities.WallImageItem;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static com.example.myroutes.SharedViewModel.Status;

public class FileService {
    private static final String TAG = "FileService";

    public static boolean hasImageFile(String wall_id, Application application) {
        // Check if file exists
        String fname = wall_id + ".png";
        String fpath = application.getFilesDir().getAbsolutePath() + "/" + fname;
        File file = new File(fpath);
        return file.exists();
    }

    public static LiveData<Result<WallImageItem>> readImageFromDevice(String wall_id, Application application) {
        assert wall_id != null;
        MutableLiveData<Result<WallImageItem>> result = new MutableLiveData<>();

        if (!hasImageFile(wall_id, application)) {
            result.setValue(new Result<>(null, Status.NOT_FOUND));
            return result;
        }

        // Run image read on background thread
        new Thread(() -> {
            String fname = wall_id + ".png";
            // Try to read in wall image
            try {
                FileInputStream fis = application.openFileInput(fname);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                WallImageItem item = new WallImageItem("", wall_id, bitmap);
                result.postValue(new Result<>(item, Status.SUCCESS));
                Log.e(TAG, "Successful read of file " + fname);
            }
            catch (Exception e) {
                Log.e("frag", "Failed to read file", e);
                deleteImageFromDevice(wall_id, application);
                result.postValue(new Result<>(null, Status.FAILURE));
            }
        }).start();

        return result;
    }

    public static LiveData<Result<RemoteInsertOneResult>> writeImageToDevice(WallImageItem item, Application application) {
        assert item != null;
        MutableLiveData<Result<RemoteInsertOneResult>> result = new MutableLiveData<>();

        // Run image write on background thread
        new Thread(() -> {
            String fname = item.getWall_id() + ".png";
            File file = new File(application.getFilesDir(), fname);
            // Try to write file
            try {
                FileOutputStream fos = new FileOutputStream(file);
                item.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                result.postValue(new Result<>(null, Status.SUCCESS));
                Log.e(TAG, "Successful write of file " + fname);
            }
            catch (Exception e) {
                Log.e(TAG, "Error writing image file", e);
                file.delete();
                result.postValue(new Result<>(null, Status.FAILURE));
            }
        }).start();

        return result;
    }

    public static LiveData<Result<RemoteDeleteResult>> deleteImageFromDevice(String wall_id, Application application) {
        assert wall_id != null;
        MutableLiveData<Result<RemoteDeleteResult>> result = new MutableLiveData<>();

        // Try to delete file
        String fname = wall_id + ".png";
        File file = new File(application.getFilesDir(), fname);
        boolean deleted = file.delete();

        result.setValue((deleted) ?
                new Result<>(null, Status.SUCCESS) :
                new Result<>(null, Status.FAILURE));

        return result;
    }
}
