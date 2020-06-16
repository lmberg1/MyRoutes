package com.example.myroutes.db;

import android.graphics.Bitmap;
import android.graphics.Path;

import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.db.mongoClasses.WallImageItem;
import com.example.myroutes.db.mongoClasses.WorkoutItem;
import com.google.android.gms.common.util.BiConsumer;

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

    public ArrayList<Path> getPaths() {
        return data.getPaths();
    }

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
        boulderItems.remove(item);
        if (boulderItems.size() == 0) {
            this.boulders.remove(grade);
        }
        else {
            this.boulders.put(grade, boulderItems);
        }
    }

    public BoulderItem searchBoulder(String boulder_id) {
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
