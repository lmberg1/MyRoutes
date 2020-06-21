package com.example.myroutes.db.mongoClasses;

import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myroutes.db.dao.PointArrayConverter;

import org.opencv.core.Point;

import java.util.ArrayList;

@Entity(tableName = "walldata_table")
public class WallDataItem {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "wall_id")
    private final String wall_id;

    private final String user_id;
    private String wall_name;

    @TypeConverters({PointArrayConverter.class})
    private ArrayList<ArrayList<Point>> contours;
    @Ignore
    private ArrayList<Path> paths;

    public WallDataItem (
            final String user_id,
            final String wall_id,
            final String wall_name,
            final ArrayList<ArrayList<Point>> contours) {
        this.user_id = user_id;
        this.wall_id = wall_id;
        this.wall_name = wall_name;
        this.contours = contours;
        this.paths = pathsFromPoints(contours);
    }

    public String getUser_id() {
        return user_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public String getWall_name() {
        return wall_name;
    }

    public void setWall_name(String name) { this.wall_name = name; }

    public ArrayList<ArrayList<Point>> getContours() { return contours; }

    public ArrayList<Path> getPaths() {
        if (paths == null) {
            paths = pathsFromPoints(getContours());
        }
        return paths;
    }

    private static ArrayList<Path> pathsFromPoints(ArrayList<ArrayList<Point>> points) {
        ArrayList<Path> paths = new ArrayList<>();
        for (ArrayList<Point> hold : points) {
            Path path = new Path();
            int n = hold.size();
            for (int i = 0; i < n; i++) {
                Point p = hold.get(i);
                if (i == 0) { path.moveTo((float) p.x, (float) p.y); }
                else { path.lineTo((float) p.x, (float) p.y); }
            }
            path.close();
            paths.add(path);
        }
        return paths;
    }
}

