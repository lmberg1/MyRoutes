package com.example.myroutes.db;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.example.myroutes.R;
import com.example.myroutes.db.backends.FileService;
import com.example.myroutes.db.backends.MongoWebservice;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.db.mongoClasses.WallImageItem;
import com.mongodb.stitch.android.core.auth.StitchUser;

import java.lang.ref.WeakReference;
import java.util.List;

import com.example.myroutes.db.SharedViewModel.Status;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

public class WallDataRepository {
    private static final String TAG = "WallDataRepository";

    private WeakReference<Application> application;

    // User id
    private String stitchUserId;

    WallDataRepository(Application application) {
        this.application = new WeakReference<>(application);
        /*WallDataRoomDatabase db = WallDataRoomDatabase.getDatabase(application);
        wallDataDao = db.wallDataDao();
        allWalls = wallDataDao.getAllWalls();*/

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
            } else if (o instanceof Result.Error) {
                mediator.setValue(((Result.Error<StitchUser>) o).getError());
            } else if (o instanceof Result.Success) {
                StitchUser user = ((Result.Success<StitchUser>) o).getResult();
                this.stitchUserId = user.getId();
                mediator.setValue(Status.SUCCESS);
            }
        });
        return mediator;
    }

    /*-----------------------------------Handle Boulder Data -------------------------------------*/

    LiveData<Result<List<BoulderItem>>> getBoulders(String wall_id) {
        // Try to get from dao
        /*if (boulderDao.hasAnyFromWall(wall_id)) {
            MediatorLiveData<Result<List<BoulderItem>>> result = new MediatorLiveData<>();
            LiveData<List<BoulderItem>> boulders = boulderDao.getAllFromWall(wall_id);

            result.addSource(boulders, o -> {
                result.setValue(new Result.Success<>(o));
            });

            return result;
        }*/

        // Otherwise check database
        return MongoWebservice.getBouldersFromMongo(wall_id);
    }

    LiveData<Result<RemoteInsertOneResult>> insertBoulder(BoulderItem item) {
        // Insert to local storage
        /*WallDataRoomDatabase.databaseWriteExecutor.execute(() -> {
            boulderDao.insert(item);
        });*/

        // Insert to mongo
        return MongoWebservice.addBoulderToMongo(item);
    }

    LiveData<Result<RemoteDeleteResult>> deleteBoulders(String wall_id) {
        /*WallDataRoomDatabase.databaseWriteExecutor.execute(() -> {
            boulderDao.deleteAll();
        });*/
        return MongoWebservice.deleteBouldersFromMongo(wall_id);
    }

    /*--------------------------------------Handle Wall Data -------------------------------------*/

    LiveData<Result<RemoteInsertOneResult>> insertWallData(WallDataItem item) {
        return MongoWebservice.addDataToMongo(item);
    }

    LiveData<Result<WallDataItem>> getWallData(String wall_id) {
        return MongoWebservice.getDataFromMongo(wall_id);
    }

    LiveData<Result<RemoteDeleteResult>> deleteWallData(String wall_id) {
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
            if (o instanceof Result.Success) {
                FileService.writeImageToDevice(((Result.Success<WallImageItem>) o).getResult(), app);
            }
        });

        // Write image to device
        Observer<Result<WallImageItem>> observeImage = new Observer<Result<WallImageItem>>() {
            @Override
            public void onChanged(Result<WallImageItem> o) {
                if (o instanceof Result.Success) {
                    FileService.writeImageToDevice(((Result.Success<WallImageItem>) o).getResult(), app);
                    image.removeObserver(this);
                }
            }
        };
        image.observeForever(observeImage);

        return image;
    }

    LiveData<Result<RemoteDeleteResult>> deleteImageData(String wall_id) {
        FileService.deleteImageFromDevice(wall_id, application.get());
        return MongoWebservice.deleteImageFromMongo(wall_id);
    }
}