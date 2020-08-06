package com.example.myroutes.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myroutes.db.dao.IntegerArrayConverter;

import org.bson.BsonArray;
import org.bson.BsonInt32;

import java.util.ArrayList;
import java.util.List;

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
    private final List<Integer> boulder_holds;
    @TypeConverters({IntegerArrayConverter.class})
    @Ignore
    private List<Integer> start_holds;
    @Ignore
    private int finish_hold = -1;

    public BoulderItem(
            final String user_id,
            final String wall_id,
            final String boulder_id,
            final String boulder_name,
            final String boulder_grade,
            final List<Integer> boulder_holds) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.boulder_id = boulder_id;
        this.boulder_name = boulder_name;
        this.boulder_grade = boulder_grade;
        this.boulder_holds = boulder_holds;
    }

    public BoulderItem(BoulderItem boulderItem) {
        this.user_id = boulderItem.getUser_id();
        this.wall_id = boulderItem.getWall_id();
        this.boulder_id = boulderItem.getBoulder_id();
        this.boulder_name = boulderItem.getBoulder_name();
        this.boulder_grade = boulderItem.getBoulder_grade();
        // Create copy of holds
        this.boulder_holds = new ArrayList<>();
        this.boulder_holds.addAll(boulderItem.getBoulder_holds());
        // Check for start and finish holds
        if (boulderItem.hasStart_holds()) {
            this.start_holds = new ArrayList<>();
            this.start_holds.addAll(boulderItem.getStart_holds());
        }
        if (boulderItem.hasFinish_hold()) {
            this.finish_hold = boulderItem.getFinish_hold();
        }
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

    public List<Integer> getBoulder_holds() {
        return boulder_holds;
    }

    public List<Integer> getStart_holds() {
        if (start_holds == null) {
            start_holds = new ArrayList<>();
        }
        return start_holds;
    }

    public int getFinish_hold() {
        return finish_hold;
    }

    public void setFinish_hold(int finish_hold) {
        this.finish_hold = finish_hold;
    }

    public void setStart_holds(List<Integer> start_holds) {
        this.start_holds = start_holds;
    }

    public boolean hasFinish_hold() {
        return finish_hold != -1;
    }

    public boolean hasStart_holds() {
        if (start_holds == null) return false;
        return start_holds.size() != 0;
    }

    public BsonArray intListToBson(List<Integer> list) {
        BsonArray array = new BsonArray();
        for (int i : list) {
            array.add(new BsonInt32(i));
        }
        return array;
    }

    public BsonArray holdsToBson() {
        BsonArray holds = new BsonArray();
        for (int ind : boulder_holds) {
            holds.add(new BsonInt32(ind));
        }
        return holds;
    }
}

