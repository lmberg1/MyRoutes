package com.example.myroutes.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myroutes.db.dao.PointArrayConverter;
import com.example.myroutes.db.dao.StringArrayConverter;

import org.bson.BsonArray;
import org.bson.BsonString;

import java.util.List;

@Entity(tableName = "workout_table")
public class WorkoutItem {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "workout_id")
    private final String workout_id;
    private final String user_id;
    private final String wall_id;
    private final String workout_name;
    @TypeConverters({StringArrayConverter.class})
    private final List<List<String>> workoutSets; // List of sets of boulder ids

    public WorkoutItem(
            final String user_id,
            final String wall_id,
            final String workout_id,
            final String workout_name,
            final List<List<String>> workoutSets) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.workout_id = workout_id;
        this.workout_name = workout_name;
        this.workoutSets = workoutSets;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getWorkout_id() {
        return workout_id;
    }

    public String getWorkout_name() {
        return workout_name;
    }

    public List<List<String>> getWorkoutSets() {
        return workoutSets;
    }

    public BsonArray setsToBson() {
        // Add string list
        BsonArray bsonSets = new BsonArray();
        for (List<String> boulderIds : workoutSets) {
            BsonArray bsonIds = new BsonArray();
            for (String id : boulderIds) {
                bsonIds.add(new BsonString(id));
            }
            bsonSets.add(bsonIds);
        }
        return bsonSets;
    }
}

