package com.example.myroutes.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myroutes.db.dao.IntegerArrayConverter;

import org.bson.BsonArray;
import org.bson.BsonInt32;

import java.util.ArrayList;

@Entity(tableName = "boulder_table")
public class BoulderItem {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "boulder_id")
    private final String boulder_id;

    @ColumnInfo(name = "wall_id")
    private final String wall_id;
    @ColumnInfo(name = "grade")
    private final String boulder_grade;

    private final String user_id;
    private final String boulder_name;

    @TypeConverters({IntegerArrayConverter.class})
    private final ArrayList<Integer> boulder_holds;

    public BoulderItem(
            final String user_id,
            final String wall_id,
            final String boulder_id,
            final String boulder_name,
            final String boulder_grade,
            final ArrayList<Integer> boulder_holds) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.boulder_id = boulder_id;
        this.boulder_name = boulder_name;
        this.boulder_grade = boulder_grade;
        this.boulder_holds = boulder_holds;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getBoulder_id() {
        return boulder_id;
    }

    public String getBoulder_name() {
        return boulder_name;
    }

    public String getBoulder_grade() {
        return boulder_grade;
    }

    public ArrayList<Integer> getBoulder_holds() {
        return boulder_holds;
    }

    public BsonArray holdsToBson() {
        BsonArray holds = new BsonArray();
        for (int ind : boulder_holds) {
            holds.add(new BsonInt32(ind));
        }
        return holds;
    }
}

