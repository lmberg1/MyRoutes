package com.example.myroutes.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.mongodb.lang.NonNull;

import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkoutItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(WorkoutItem... items);

    @Query("select * from workout_table where wall_id = :wall_id")
    LiveData<List<WorkoutItem>> getAllFromWall(@NonNull String wall_id);

    @Query("delete from workout_table where workout_id = :workout_id")
    void deleteWorkout(String workout_id);

    @Query("delete from workout_table where workout_id = :workout_id")
    void deleteAllFromWall(@NonNull String workout_id);

    @Query("delete from workout_table")
    void deleteAll();
}
