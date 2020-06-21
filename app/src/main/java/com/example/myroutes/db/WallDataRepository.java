package com.example.myroutes.db;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.myroutes.R;
import com.example.myroutes.db.dao.BoulderDao;
import com.example.myroutes.db.dao.WallDataDao;
import com.example.myroutes.db.dao.WallDataRoomDatabase;
import com.example.myroutes.db.backends.FileService;
import com.example.myroutes.db.backends.MongoWebservice;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.db.mongoClasses.WallImageItem;
import com.example.myroutes.db.mongoClasses.WorkoutItem;
import com.mongodb.stitch.android.core.auth.StitchUser;

import java.lang.ref.WeakReference;
import java.util.List;

import com.example.myroutes.db.SharedViewModel.Status;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

public class WallDataRepository {
    private static final String TAG = "WallDataRepository";

    private WeakReference<Application> application;
    private BoulderDao boulderDao;
    private WallDataDao wallDataDao;

    // User id
    private String stitchUserId;

    WallDataRepository(Application application) {
        this.application = new WeakReference<>(application);
        WallDataRoomDatabase db = WallDataRoomDatabase.getDatabase(application);
        this.boulderDao = db.boulderDao();
        this.wallDataDao = db.wallDataDao();
    }

    String getStitchUserId() {
        assert stitchUserId != null;
        return stitchUserId;
    }

    LiveData<Status> setStichUser(String username) {
        String appId = application.get().getString(R.string.stitch_app_id);
        LiveData<Result<StitchUser>> result = MongoWebservice.loginToMongo(username, appId);
        MediatorLiveData<Status> mediator = new MediatorLiveData<>();
        mediator.addSource(result, o -> {
            if (o == null) {
                mediator.setValue(Status.LOADING);
            }
            else if (o.status == Status.SUCCESS) {
                StitchUser user = o.data;
                this.stitchUserId = user.getId();
                mediator.setValue(Status.SUCCESS);
            }
            else {
                mediator.setValue(o.status);
            }
        });
        return mediator;
    }

    /*-----------------------------------Handle Boulder Data -------------------------------------*/

    private void insertBouldersLocal(List<BoulderItem> boulderItems) {
       Log.e(TAG, "inserting boulders");
        BoulderItem[] itemArray = new BoulderItem[boulderItems.size()];
        AsyncTask.execute(() -> boulderDao.insertAll(boulderItems.toArray(itemArray)));
    }

    private void insertBoulderLocal(BoulderItem item) {
        AsyncTask.execute(() -> boulderDao.insert(item));
    }

    private void deleteBouldersLocal(String wall_id) {
        AsyncTask.execute(() -> boulderDao.deleteAllFromWall(wall_id));
    }

    private void deleteBoulderLocal(String boulder_id) {
        AsyncTask.execute(() -> boulderDao.deleteBoulder(boulder_id));
    }

    LiveData<Result<List<BoulderItem>>> getBoulders(String wall_id) {
        MediatorLiveData<Result<List<BoulderItem>>> result = new MediatorLiveData<>();

        // Try to get from local storage
        LiveData<List<BoulderItem>> boulders = boulderDao.getAllFromWall(wall_id);

        result.addSource(boulders, o -> {
            if (o == null) return;
            // If the boulders were found, return success
            if (o.size() != 0) {
                Log.e(TAG, "fetching boulders");
                result.setValue(new Result<>(o, Status.SUCCESS));
            }
            // Otherwise look for the boulder in the mongo database
            else {
                Log.e(TAG, "fetching mongo boulders");
                refreshBoulders(wall_id, result);
            }
            result.removeSource(boulders);
        });

        return result;
    }

    void refreshBoulders(String wall_id, MediatorLiveData<Result<List<BoulderItem>>> result) {
        LiveData<Result<List<BoulderItem>>> boulders = MongoWebservice.getBouldersFromMongo(wall_id);
        result.addSource(boulders, o -> {
            if (o == null) return;
            result.setValue(o);
            result.removeSource(boulders);
            // Insert items into local repository if query was successful
            if (o.status == Status.SUCCESS) {
                insertBouldersLocal(o.data);
            }
        });
    }

    LiveData<Result<RemoteInsertOneResult>> insertBoulder(BoulderItem item) {
        insertBoulderLocal(item);
        return MongoWebservice.addBoulderToMongo(item);
    }

    LiveData<Result<RemoteUpdateResult>> updateBoulder(BoulderItem item) {
        insertBoulderLocal(item);
        return MongoWebservice.editBoulderInMongo(item);
    }

    LiveData<Result<RemoteDeleteResult>> deleteAllBoulders(String wall_id) {
        deleteBouldersLocal(wall_id);
        return MongoWebservice.deleteBouldersFromMongo(wall_id);
    }

    LiveData<Result<RemoteDeleteResult>> deleteBoulder(String wall_id, String boulder_id) {
        deleteBoulderLocal(boulder_id);
        return MongoWebservice.deleteBoulderFromMongo(wall_id, boulder_id);
    }

    LiveData<Result<RemoteDeleteResult>> deleteAllUserBoulders() {
        return MongoWebservice.deleteAllUserBouldersFromMongo(stitchUserId);
    }

    /*-----------------------------------Handle Workout Data -------------------------------------*/

    LiveData<Result<RemoteInsertOneResult>> inserWorkout(WorkoutItem item) {
        return MongoWebservice.addWorkoutToMongo(item);
    }

    LiveData<Result<RemoteUpdateResult>> updateWorkout(WorkoutItem item) {
        return MongoWebservice.editWorkoutInMongo(item);
    }

    LiveData<Result<List<WorkoutItem>>> getWorkouts(String wall_id) {
        return MongoWebservice.getWorkoutsFromMongo(wall_id);
    }

    LiveData<Result<RemoteDeleteResult>> deleteAllWorkouts(String wall_id) {
        return MongoWebservice.deleteWorkoutsFromMongo(wall_id);
    }

    LiveData<Result<RemoteDeleteResult>> deleteWorkout(String wall_id, String workout_id) {
        return MongoWebservice.deleteWorkoutFromMongo(wall_id, workout_id);
    }

    /*--------------------------------------Handle Wall Data -------------------------------------*/

    private void insertWallLocal(WallDataItem item) {
        AsyncTask.execute(() -> wallDataDao.insert(item));
    }

    private void deleteWallLocal(String wall_id) {
        AsyncTask.execute(() -> wallDataDao.deleteWallData(wall_id));
    }

    LiveData<Result<WallDataItem>> getWallData(String wall_id) {
        MediatorLiveData<Result<WallDataItem>> result = new MediatorLiveData<>();
        // Try to get from local storage
        LiveData<WallDataItem> wallData = wallDataDao.getWallData(wall_id);
        result.addSource(wallData, o -> {
            if (o != null) {
                Log.e(TAG, "fetching wall");
                result.setValue(new Result<>(o, Status.SUCCESS));
            }
            // Otherwise look for the boulder in the mongo database
            else {
                Log.e(TAG, "fetching mongo wall");
                refreshWallData(wall_id, result);
            }
            result.removeSource(wallData);
        });

        return result;
    }

    void refreshWallData(String wall_id, MediatorLiveData<Result<WallDataItem>> result) {
        LiveData<Result<WallDataItem>> mongoWallData = MongoWebservice.getDataFromMongo(wall_id);
        result.addSource(mongoWallData, o1 -> {
            if (o1 == null) return;
            result.setValue(o1);
            result.removeSource(mongoWallData);
            // Write wall data to device if query was successful
            if (o1.status == Status.SUCCESS) {
                insertWallLocal(o1.data);
            }
        });
    }

    LiveData<Result<RemoteInsertOneResult>> insertWallData(WallDataItem item) {
        insertWallLocal(item);
        return MongoWebservice.addDataToMongo(item);
    }

    LiveData<Result<RemoteDeleteResult>> deleteWallData(String wall_id) {
        deleteWallLocal(wall_id);
        return MongoWebservice.deleteDataFromMongo(wall_id);
    }

    /*--------------------------------------Handle Image Data -------------------------------------*/


    LiveData<Result<RemoteInsertOneResult>> insertImageData(WallImageItem item) {
        FileService.writeImageToDevice(item, application.get());
        return MongoWebservice.addImageToMongo(item);
    }

    LiveData<Result<WallImageItem>> getImageData(String wall_id) {
        // Check device storage for image
        Application app = application.get();
        if (FileService.hasImageFile(wall_id, app)) {
            return FileService.readImageFromDevice(wall_id, app);
        }

        // Check database for image
        LiveData<Result<WallImageItem>> image = MongoWebservice.getImageFromMongo(wall_id);
        MediatorLiveData<?> mediator = new MediatorLiveData<>();
        mediator.addSource(image, o -> {
            if (o.status == Status.SUCCESS) {
                FileService.writeImageToDevice(o.data, app);
            }
        });

        return image;
    }

    LiveData<Result<RemoteDeleteResult>> deleteImageData(String wall_id) {
        FileService.deleteImageFromDevice(wall_id, application.get());
        return MongoWebservice.deleteImageFromMongo(wall_id);
    }
}