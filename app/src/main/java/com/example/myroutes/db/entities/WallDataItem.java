package com.example.myroutes.db.entities;

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
import java.util.List;

@Entity(tableName = "walldata_table")
public class WallDataItem {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "wall_id")
    private String wall_id;

    private String user_id;
    private String wall_name;

    @TypeConverters({PointArrayConverter.class})
    private List<List<Point>> contours;
    @Ignore
    private List<Path> paths;

    public WallDataItem (
            final String user_id,
            final String wall_id,
            final String wall_name,
            final List<List<Point>> contours) {
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

    public List<List<Point>> getContours() {
        // Return defensive copy of points
        List<List<Point>> copy = new ArrayList<>();
        for (List<Point> list : contours) {
            List<Point> listCopy = new ArrayList<>();
            for (Point p : list) {
                listCopy.add(new Point(p.x, p.y));
            }
            copy.add(listCopy);
        }
        return copy;
    }

    public List<Path> getPaths() {
        if (paths == null) {
            paths = pathsFromPoints(contours);
        }
        // Return defensive copy of paths
        List<Path> pathsCopy = new ArrayList<>();
        for (Path p : paths) {
            Path copy = new Path(p);
            pathsCopy.add(copy);
        }
        return pathsCopy;
    }

    private static ArrayList<Path> pathsFromPoints(List<List<Point>> points) {
        ArrayList<Path> paths = new ArrayList<>();
        for (List<Point> hold : points) {
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

