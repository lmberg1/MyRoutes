package com.example.myroutes;

import android.graphics.Bitmap;
import android.graphics.Path;

import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.db.entities.WorkoutItem;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Wall {
    private WallDataItem data;
    private WallImageItem image;
    private HashMap<String, List<BoulderItem>> boulders;
    private List<WorkoutItem> workouts;

    public Wall(WallDataItem data,
                WallImageItem image,
                HashMap<String, List<BoulderItem>> boulders,
                List<WorkoutItem> workouts) {
        this.data = data;
        this.image = image;
        this.boulders = (boulders == null) ? new HashMap<>() : boulders;
        this.workouts = (workouts == null) ? new ArrayList<>() : workouts;
    }

    public String getId() {
        return data.getWall_id();
    }

    public String getName() {
        return data.getWall_name();
    }

    public List<Path> getPaths() {
        return data.getPaths();
    }

    public List<List<Point>> getPoints() {return data.getContours(); }

    public Bitmap getBitmap() {
        return image.getBitmap();
    }

    public HashMap<String, List<BoulderItem>> getBoulders() {
        return boulders;
    }

    public List<WorkoutItem> getWorkouts() {
        return workouts;
    }

    public void setName(String name) {
        data.setWall_name(name);
    }

    public void setBoulders(HashMap<String, List<BoulderItem>> boulders) {
        this.boulders = boulders;
    }

    public void setWorkouts(List<WorkoutItem> workouts) {
        this.workouts = workouts;
    }

    public void addBoulder(BoulderItem item) {
        String grade = item.getBoulder_grade();
        List<BoulderItem> boulderItems = boulders.getOrDefault(grade, new ArrayList<>());
        assert boulderItems != null;
        boulderItems.add(item);
        this.boulders.put(grade, boulderItems);
    }

    public void removeBoulder(BoulderItem item) {
        String grade = item.getBoulder_grade();
        List<BoulderItem> boulderItems = boulders.get(grade);
        if (boulderItems == null) return;
        boulderItems.remove(searchBoulderId(item.getBoulder_id()));
        if (boulderItems.size() == 0) {
            this.boulders.remove(grade);
        }
        else {
            this.boulders.put(grade, boulderItems);
        }
    }

    public BoulderItem searchBoulderId(String boulder_id) {
        Set<String> keys = boulders.keySet();
        for (String key : keys) {
            List<BoulderItem> items = boulders.get(key);
            if (items == null) continue;
            for (BoulderItem b : items) {
                if (b.getBoulder_id().equals(boulder_id)) {
                    return b;
                }
            }
        }
        return null;
    }

    public List<List<BoulderItem>> workoutToBoulders(WorkoutItem workoutItem) {
        List<List<BoulderItem>> boulderSets = new ArrayList<>();
        List<List<String>> boulderSetList = workoutItem.getWorkoutSets();

        // Convert lists of ids to lists of boulders for each set
        int nSets = boulderSetList.size();
        for (int i = 0; i < nSets; i++) {
            List<String> boulderIds = boulderSetList.get(i);
            List<BoulderItem> boulderItems = new ArrayList<>();
            for (String boulder_id : boulderIds) {
                BoulderItem b = searchBoulderId(boulder_id);
                // Maker sure boulder exists
                if (b != null) { boulderItems.add(b); }
            }
            // Make sure there are boulders in this set
            if (!boulderItems.isEmpty()) { boulderSets.add(boulderItems); }
        }
        return boulderSets;
    }

    public void addWorkout(WorkoutItem item) {
        workouts.add(item);
    }

    public void removeWorkout(WorkoutItem item) {
        workouts.remove(item);
    }

    public WorkoutItem searchWorkout(String workout_id) {
        for (WorkoutItem workoutItem : workouts) {
            if (workoutItem.getWorkout_id().equals(workout_id)) {
                return workoutItem;
            }
        }
        return null;
    }
}
