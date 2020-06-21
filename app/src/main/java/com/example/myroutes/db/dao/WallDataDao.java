package com.example.myroutes.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myroutes.db.entities.WallDataItem;
import com.mongodb.lang.NonNull;

import java.util.List;

@Dao
public interface WallDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WallDataItem wallData);

    @Query("select * from walldata_table where wall_id = :wall_id")
    LiveData<WallDataItem> getWallData(@NonNull String wall_id);

    @Query("select * from walldata_table")
    LiveData<List<WallDataItem>> getAllWalls();

    @Query("delete from walldata_table where wall_id = :wall_id")
    void deleteWallData(String wall_id);

    @Query("delete from walldata_table")
    void deleteAll();
}
