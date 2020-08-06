package com.example.myroutes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myroutes.db.Result;
import com.example.myroutes.db.SharedPreferencesModel;
import com.example.myroutes.db.WallDataRepository;
import com.example.myroutes.db.backends.FileService;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.example.myroutes.util.PreferenceManager;
import com.example.myroutes.util.WallMetadata;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteDeleteResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SharedViewModel extends AndroidViewModel {
    private static final String TAG = "SharedViewModel";

    // Statuses for fetching/inserting data
    public enum Status {SUCCESS, FAILURE, NOT_FOUND, LOADING};

    // All possible boulder grades
    public static final String[] BOULDER_GRADES = {"V0", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "V10"};

    // LiveData to communicate with fragments about status of current wall
    MutableLiveData<Status> currentWallStatus = new MutableLiveData<>();

    // Id of current wall
    private String currentWallId;

    // Stores all currently cached walls under their wall_id
    private HashMap<String, Wall> loadedWalls = new HashMap<>();

    // Repository
    private WallDataRepository wallDataRepository;
    private SharedPreferencesModel sharedPreferencesModel;

    public SharedViewModel(@NonNull Application application) {
        super(application);

        wallDataRepository = new WallDataRepository(application);
        sharedPreferencesModel = new SharedPreferencesModel(application);
    }

    /*---------------------------------MAIN WALL FUNCTIONS----------------------------------------*/

    public String getCurrentWallId() {
        return currentWallId;
    }

    public LiveData<Status> getCurrentWallStatus() {
        return currentWallStatus;
    }

    public Wall getWall(String wall_id) {
        assert wall_id != null;
        return loadedWalls.getOrDefault(wall_id, null);
    }

    public LiveData<Status> createWall(WallDataItem dataItem, WallImageItem imageItem) {
        assert dataItem != null;
        assert imageItem != null;

        // Immediately add data to cache
        addWallToCache(dataItem, imageItem, new ArrayList<>(), new ArrayList<>());

        MediatorLiveData<Status> mediator = new MediatorLiveData<>();
        LiveData<Result<RemoteInsertOneResult>> addData = wallDataRepository.insertWallData(dataItem);
        LiveData<Result<RemoteInsertOneResult>> addImage = wallDataRepository.insertImageData(imageItem);

        // Wait until both data and image have been saved
        waitForAll(addData, addImage).observeForever(o -> {
            if (o == Status.LOADING) {
                mediator.setValue(Status.LOADING);
                return;
            }
            mediator.setValue(checkForLoadError(addData.getValue(), addImage.getValue()));
        });

        return mediator;
    }

    public LiveData<Status> downloadWall(String wall_id) {
        assert wall_id != null;

        MediatorLiveData<Status> mediator = new MediatorLiveData<>();

        // Try to fetch data, image, and boulders from repository
        LiveData<Result<WallDataItem>> data = wallDataRepository.getWallData(wall_id);
        LiveData<Result<WallImageItem>> image = wallDataRepository.getImageData(wall_id);
        LiveData<Result<List<BoulderItem>>> boulders = wallDataRepository.getMongoBoulders(wall_id);
        LiveData<Result<List<WorkoutItem>>> workouts = wallDataRepository.getMongoWorkouts(wall_id);

        // Wait until data, image, and boulders results have loaded
        waitForAll(data, image, boulders, workouts).observeForever(o -> {
            // Check if data is still loading
            if (o == Status.LOADING) {
                mediator.setValue(Status.LOADING);
                return;
            }
            // Check if data or image loading resulted in errors
            Status s = checkForLoadError(data.getValue(), image.getValue());
            if (s != Status.SUCCESS) {
                mediator.setValue(s);
                return;
            }
            // No errors so save wall
            addWallToCache(
                    Objects.requireNonNull(data.getValue()).data,
                    Objects.requireNonNull(image.getValue()).data,
                    Objects.requireNonNull(boulders.getValue()).data,
                    Objects.requireNonNull(workouts.getValue()).data
            );
            mediator.setValue(Status.SUCCESS);
        });

        return mediator;
    }

    public LiveData<Status> setCurrentWall(String wall_id) {
        assert wall_id != null;

        currentWallStatus.setValue(Status.LOADING);

        // Check if wall is already loaded
        if (loadedWalls.containsKey(wall_id)) {
            currentWallId = wall_id;
            currentWallStatus.setValue(Status.SUCCESS);
            return currentWallStatus;
        }

        // Otherwise we need to download it first
        LiveData<Status> downloadStatus = downloadWall(wall_id);
        //currentWallStatus = (MutableLiveData<Status>) downloadWall(wall_id);
        downloadStatus.observeForever(o -> {
            if (o == Status.SUCCESS) {
                currentWallId = wall_id;
                currentWallStatus.setValue(Status.SUCCESS);
            }
        });

        return currentWallStatus;
    }

    public LiveData<Status> deleteWallPermanent(String wall_id) {
        assert wall_id != null;

        // Remove from device
        deleteWallLocal(wall_id);

        // Delete wall from database
        LiveData<Result<RemoteDeleteResult>> data = wallDataRepository.deleteWallData(wall_id);
        LiveData<Result<RemoteDeleteResult>> image = wallDataRepository.deleteImageData(wall_id);
        LiveData<Result<RemoteDeleteResult>> boulders = wallDataRepository.deleteAllBoulders(wall_id);
        LiveData<Result<RemoteDeleteResult>> workouts = wallDataRepository.deleteAllWorkouts(wall_id);

        MediatorLiveData<Status> mediator = new MediatorLiveData<>();
        // Wait until data, image, and boulders have all been deleted
        waitForAll(data, image, boulders, workouts).observeForever(o -> {
            // Deletions are still in progress
            if (o == Status.LOADING) {
                mediator.setValue(Status.LOADING);
                return;
            }
            // Deletions are finished, but check if there were any errors
            mediator.setValue(checkForLoadError(data.getValue(), image.getValue(),
                    boulders.getValue(), workouts.getValue()));
        });
        return mediator;
    }


    public void deleteWallLocal(String wall_id) {
        assert wall_id != null;
        removeWallFromCache(wall_id);
        FileService.deleteImageFromDevice(wall_id, getApplication());
    }

    public void updateWall(WallMetadata metadata) {
        String wall_id = metadata.getWall_id();
        if (loadedWalls.containsKey(wall_id)) {
            loadedWalls.get(wall_id).setName(metadata.getWall_name());
        }
        // Update preferences
        sharedPreferencesModel.setWall_metadata(metadata);
    }

    public LiveData<Status> syncBoulders(String wall_id) {
        MediatorLiveData<Status> mediator = new MediatorLiveData<>();
        LiveData<Result<List<BoulderItem>>> boulders = wallDataRepository.getMongoBoulders(wall_id);
        waitForAll(boulders).observeForever(o -> {
            // Check if data is still loading
            if (o == Status.LOADING) {
                mediator.setValue(Status.LOADING);
                return;
            }
            // Check if data or image loading resulted in errors
            Result<List<BoulderItem>> boulderItems = boulders.getValue();
            if (boulderItems == null) return;
            Status s = checkForLoadError(boulderItems);
            if (s != Status.SUCCESS) {
                mediator.setValue(s);
                return;
            }
            getWall(wall_id).setBoulders(orderByGrade(boulderItems.data));
            mediator.setValue(Status.SUCCESS);
        });
        return mediator;
    }

    /*-----------------------------------HANDLE PREFERENCES---------------------------------------*/

    public String getUsername() {
        return sharedPreferencesModel.getUsername();
    }

    public String getDefault_id() {
        return sharedPreferencesModel.getDefault_id();
    }

    public Set<String> getWall_ids() {
        return sharedPreferencesModel.getWall_ids();
    }

    public boolean hasWall(String wall_id) {
        return sharedPreferencesModel.hasWall(wall_id);
    }

    public WallMetadata getWall_metadata(String wall_id) {
        return sharedPreferencesModel.getWall_metadata(wall_id);
    }

    public void setUsername(String username) {
        sharedPreferencesModel.setUsername(username);
    }

    public void setDefault_id(String default_id) {
        sharedPreferencesModel.setDefault_id(default_id);
    }

    /*-------------------------------------HANDLE CACHE-------------------------------------------*/

    // Update variables and shared preferences
    private void addWallToCache(WallDataItem data, WallImageItem image,
                                List<BoulderItem> boulders, List<WorkoutItem> workouts) {
        // Create wall item
        Wall wall = new Wall(data, image, orderByGrade(boulders), workouts);

        // Update variables
        loadedWalls.putIfAbsent(wall.getId(), wall);
        if (currentWallId == null) {
            currentWallId = wall.getId();
            currentWallStatus.setValue(Status.SUCCESS);
        }

        // Update preferences
        PreferenceManager.Role role = (getStitchUserId().equals(data.getUser_id())) ?
                PreferenceManager.Role.OWNER : PreferenceManager.Role.NON_OWNER;
        sharedPreferencesModel.addWall(data, role);
    }

    private void removeWallFromCache(String wall_id) {
        // Remove wall from cache if it was there
        this.loadedWalls.remove(wall_id);

        // Check if we're trying to delete the current wall
        if (wall_id.equals(currentWallId)) {
            String default_id = sharedPreferencesModel.getDefault_id();
            // No more walls
            if (default_id == null) { currentWallId = null; }
            // Need to update current wall
            else { setCurrentWall(default_id); }
        }

        // Update preferences
        sharedPreferencesModel.removeWall(wall_id);
    }

    /*-----------------------------------HELPER FUNCTIONS-----------------------------------------*/

    // Create LiveData that only returns SUCCESS when all items have loaded, and false otherwise
    private static LiveData<Status> waitForAll(LiveData<?>... items) {
        MediatorLiveData<Status> mediator = new MediatorLiveData<>();
        for (LiveData<?> item : items) {
            mediator.addSource(item, o -> {
                // Check if all other items have loaded
                for (LiveData<?> result: items) {
                    if (result.getValue() == null) {
                        mediator.setValue(Status.LOADING);
                        return;
                    }
                }
                mediator.setValue(Status.SUCCESS);
            });
        }
        return mediator;
    }

    // Check for Status errors in each LiveData item
    private static Status checkForLoadError(Result<?>... items) {
        for (Result<?> item : items) {
            if (item.status != Status.SUCCESS) {
                return item.status;
            }
        }
        return Status.SUCCESS;
    }

    private static HashMap<String, List<BoulderItem>> orderByGrade(List<BoulderItem> items) {
        // Initialize hashmap
        HashMap<String, List<BoulderItem>> boulderMap = new HashMap<>();
        if (items == null) { return boulderMap; }

        // Sort boulders into their correct grade
        for (BoulderItem item : items) {
            String grade = item.getBoulder_grade();
            List<BoulderItem> gradeList = boulderMap.getOrDefault(grade, new ArrayList<>());
            assert gradeList != null;
            gradeList.add(item);
            boulderMap.put(item.getBoulder_grade(), gradeList);
        }

        return boulderMap;
    }

    /*------------------------------------HANDLE BOULDERS-----------------------------------------*/

    public void addBoulderItem(BoulderItem boulderItem) {
        assert boulderItem != null;

        // Update walls
        Wall wall = loadedWalls.get(boulderItem.getWall_id());
        assert wall != null;

        // Check if boulder already exists
        BoulderItem existingBoulder = wall.searchBoulderId(boulderItem.getBoulder_id());
        if (existingBoulder != null) {
            wall.removeBoulder(existingBoulder);
            wall.addBoulder(boulderItem);
            wallDataRepository.updateBoulder(boulderItem);
            return;
        }

        // Boulder doesn't already exist so add it to cache and repository
        wall.addBoulder(boulderItem);
        wallDataRepository.insertBoulder(boulderItem);
    }

    public void deleteBoulderItem(BoulderItem boulderItem) {
        assert boulderItem != null;

        // Update walls
        Wall wall = loadedWalls.get(boulderItem.getWall_id());
        assert wall != null;
        wall.removeBoulder(boulderItem);

        // Update database
        wallDataRepository.deleteBoulder(boulderItem.getWall_id(), boulderItem.getBoulder_id());
    }

    /*------------------------------------HANDLE WORKOUTS-----------------------------------------*/

    public void addWorkoutItem(WorkoutItem workoutItem) {
        assert workoutItem != null;

        // Update walls
        Wall wall = loadedWalls.get(workoutItem.getWall_id());
        assert wall != null;

        // Check if workout already exists
        WorkoutItem existingWorkout = wall.searchWorkout(workoutItem.getWorkout_id());
        if (existingWorkout != null) {
            List<WorkoutItem> workouts = wall.getWorkouts();
            int index = workouts.indexOf(existingWorkout);
            workouts.set(index, workoutItem);
            wallDataRepository.updateWorkout(workoutItem);
            return;
        }

        // Update database
        wall.addWorkout(workoutItem);
        wallDataRepository.inserWorkout(workoutItem);
    }

    public void deleteWorkoutItem(WorkoutItem item) {
        assert item != null;

        // Update walls
        Wall wall = loadedWalls.get(item.getWall_id());
        assert wall != null;
        wall.removeWorkout(item);

        // Update database
        wallDataRepository.deleteWorkout(item.getWall_id(), item.getWorkout_id());
    }

    /*---------------------------------HANDLE AUTHENTICATION--------------------------------------*/

    public String getStitchUserId() {
        return wallDataRepository.getStitchUserId();
    }

    public LiveData<Status> setStitchUser(String username) {
        return wallDataRepository.setStichUser(username);
    }
}
