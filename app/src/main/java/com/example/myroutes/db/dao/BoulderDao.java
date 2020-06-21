package com.example.myroutes.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myroutes.db.entities.BoulderItem;
import com.mongodb.lang.NonNull;

import java.util.List;

@Dao
public interface BoulderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BoulderItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(BoulderItem... items);

    @Query("select * from boulder_table where wall_id = :wall_id")
    LiveData<List<BoulderItem>> getAllFromWall(@NonNull String wall_id);

    @Query("select * from boulder_table where wall_id = :wall_id and grade = :grade")
    LiveData<List<BoulderItem>> getBouldersOfGrade(@NonNull String wall_id, @NonNull String grade);

    @Query("delete from boulder_table where boulder_id = :boulder_id")
    void deleteBoulder(String boulder_id);

    @Query("delete from boulder_table where wall_id = :wall_id")
    void deleteAllFromWall(@NonNull String wall_id);

    @Query("delete from boulder_table")
    void deleteAll();
}
