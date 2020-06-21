package com.example.myroutes.db.dao;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WallDataItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {WallDataItem.class, BoulderItem.class}, version = 1, exportSchema = false)
public abstract class WallDataRoomDatabase extends RoomDatabase {

    public abstract WallDataDao wallDataDao();
    public abstract BoulderDao boulderDao();

    private static volatile WallDataRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static WallDataRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WallDataRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            WallDataRoomDatabase.class, "walldata_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
