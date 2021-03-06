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
import com.example.myroutes.db.dao.WorkoutDao;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.mongodb.stitch.android.core.auth.StitchUser;

import java.lang.ref.WeakReference;
import java.util.List;

import com.example.myroutes.SharedViewModel.Status;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

public class WallDataRepository {
    private static final String TAG = "WallDataRepository";

    private WeakReference<Application> application;
    private BoulderDao boulderDao;
    private WallDataDao wallDataDao;
    private WorkoutDao workoutDao;

    // User id
    private String stitchUserId;

    public WallDataRepository(Application application) {
        this.application = new WeakReference<>(application);
        WallDataRoomDatabase db = WallDataRoomDatabase.getDatabase(application);
        this.boulderDao = db.boulderDao();
        this.wallDataDao = db.wallDataDao();
        this.workoutDao = db.workoutDao();
    }

    public String getStitchUserId() {
        assert stitchUserId != null;
        return stitchUserId;
    }

    public LiveData<Status> setStichUser(String username) {
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

    public LiveData<Result<List<BoulderItem>>> getMongoBoulders(String wall_id) {
        return MongoWebservice.getBouldersFromMongo(wall_id);
    }

    public LiveData<Result<List<BoulderItem>>> getBoulders(String wall_id) {
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

    public void refreshBoulders(String wall_id, MediatorLiveData<Result<List<BoulderItem>>> result) {
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

    public LiveData<Result<RemoteInsertOneResult>> insertBoulder(BoulderItem item) {
        insertBoulderLocal(item);
        return MongoWebservice.addBoulderToMongo(item);
    }

    public LiveData<Result<RemoteUpdateResult>> updateBoulder(BoulderItem item) {
        insertBoulderLocal(item);
        return MongoWebservice.editBoulderInMongo(item);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteAllBoulders(String wall_id) {
        deleteBouldersLocal(wall_id);
        return MongoWebservice.deleteBouldersFromMongo(wall_id);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteBoulder(String wall_id, String boulder_id) {
        deleteBoulderLocal(boulder_id);
        return MongoWebservice.deleteBoulderFromMongo(wall_id, boulder_id);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteAllUserBoulders() {
        return MongoWebservice.deleteAllUserBouldersFromMongo(stitchUserId);
    }

    /*-----------------------------------Handle Workout Data -------------------------------------*/

    private void insertWorkoutsLocal(List<WorkoutItem> workoutItems) {
        Log.e(TAG, "inserting workouts");
        WorkoutItem[] itemArray = new WorkoutItem[workoutItems.size()];
        AsyncTask.execute(() -> workoutDao.insertAll(workoutItems.toArray(itemArray)));
    }

    private void insertWorkoutLocal(WorkoutItem item) {
        AsyncTask.execute(() -> workoutDao.insert(item));
    }

    private void deleteWorkoutsLocal(String wall_id) {
        AsyncTask.execute(() -> workoutDao.deleteAllFromWall(wall_id));
    }

    private void deleteWorkoutLocal(String workout_id) {
        AsyncTask.execute(() -> workoutDao.deleteWorkout(workout_id));
    }

    public LiveData<Result<List<WorkoutItem>>> getMongoWorkouts(String wall_id) {
        return MongoWebservice.getWorkoutsFromMongo(wall_id);
    }

    public LiveData<Result<List<WorkoutItem>>> getWorkouts(String wall_id) {
        MediatorLiveData<Result<List<WorkoutItem>>> result = new MediatorLiveData<>();

        // Try to get from local storage
        LiveData<List<WorkoutItem>> workouts = workoutDao.getAllFromWall(wall_id);

        result.addSource(workouts, o -> {
            if (o == null) return;
            // If the workouts were found, return success
            if (o.size() != 0) {
                result.setValue(new Result<>(o, Status.SUCCESS));
            }
            // Otherwise look for workouts in the mongo database
            else {
                refreshWorkouts(wall_id, result);
            }
            result.removeSource(workouts);
        });

        return result;
    }

    public void refreshWorkouts(String wall_id, MediatorLiveData<Result<List<WorkoutItem>>> result) {
        LiveData<Result<List<WorkoutItem>>> workouts = MongoWebservice.getWorkoutsFromMongo(wall_id);
        result.addSource(workouts, o -> {
            if (o == null) return;
            result.setValue(o);
            result.removeSource(workouts);
            // Insert items into local repository if query was successful
            if (o.status == Status.SUCCESS) {
                insertWorkoutsLocal(o.data);
            }
        });
    }

    public LiveData<Result<RemoteInsertOneResult>> inserWorkout(WorkoutItem item) {
        insertWorkoutLocal(item);
        return MongoWebservice.addWorkoutToMongo(item);
    }

    public LiveData<Result<RemoteUpdateResult>> updateWorkout(WorkoutItem item) {
        insertWorkoutLocal(item);
        return MongoWebservice.editWorkoutInMongo(item);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteAllWorkouts(String wall_id) {
        deleteWorkoutsLocal(wall_id);
        return MongoWebservice.deleteWorkoutsFromMongo(wall_id);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteWorkout(String wall_id, String workout_id) {
        deleteWorkoutLocal(workout_id);
        return MongoWebservice.deleteWorkoutFromMongo(wall_id, workout_id);
    }

    /*--------------------------------------Handle Wall Data -------------------------------------*/

    private void insertWallLocal(WallDataItem item) {
        AsyncTask.execute(() -> wallDataDao.insert(item));
    }

    private void deleteWallLocal(String wall_id) {
        AsyncTask.execute(() -> wallDataDao.deleteWallData(wall_id));
    }

    public LiveData<Result<WallDataItem>> getWallData(String wall_id) {
        MediatorLiveData<Result<WallDataItem>> result = new MediatorLiveData<>();
        // Try to get from local storage
        LiveData<WallDataItem> wallData = wallDataDao.getWallData(wall_id);
        result.addSource(wallData, o -> {
            if (o != null) {
                result.setValue(new Result<>(o, Status.SUCCESS));
            }
            // Otherwise look for the boulder in the mongo database
            else {
                refreshWallData(wall_id, result);
            }
            result.removeSource(wallData);
        });

        return result;
    }

    public void refreshWallData(String wall_id, MediatorLiveData<Result<WallDataItem>> result) {
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

    public LiveData<Result<RemoteInsertOneResult>> insertWallData(WallDataItem item) {
        insertWallLocal(item);
        return MongoWebservice.addDataToMongo(item);
    }

    public LiveData<Result<RemoteDeleteResult>> deleteWallData(String wall_id) {
        deleteWallLocal(wall_id);
        return MongoWebservice.deleteDataFromMongo(wall_id);
    }

    /*--------------------------------------Handle Image Data -------------------------------------*/


    public LiveData<Result<RemoteInsertOneResult>> insertImageData(WallImageItem item) {
        FileService.writeImageToDevice(item, application.get());
        return MongoWebservice.addImageToMongo(item);
    }

    public LiveData<Result<WallImageItem>> getImageData(String wall_id) {
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

    public LiveData<Result<RemoteDeleteResult>> deleteImageData(String wall_id) {
        FileService.deleteImageFromDevice(wall_id, application.get());
        return MongoWebservice.deleteImageFromMongo(wall_id);
    }
}