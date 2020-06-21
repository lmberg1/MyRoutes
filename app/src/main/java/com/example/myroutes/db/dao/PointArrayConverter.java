package com.example.myroutes.db.dao;

import androidx.room.TypeConverter;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Locale;

public class PointArrayConverter {
    @TypeConverter
    public static String toString(ArrayList<ArrayList<Point>> points) {
        StringBuilder xList = new StringBuilder();
        StringBuilder yList = new StringBuilder();
        for (ArrayList<Point> list : points) {
            for (Point p : list) {
                xList.append(String.format(Locale.US, "%d,", (int) p.x));
                yList.append(String.format(Locale.US, "%d,", (int) p.y));
            }
            xList.append(";");
            yList.append(";");
        }
        xList.append(":");
        xList.append(yList);
        return xList.toString();
    }

    @TypeConverter
    public static ArrayList<ArrayList<Point>> toArrayList(String s) {
        ArrayList<ArrayList<Point>> points = new ArrayList<>();
        // Check if empty
        String[] lists = s.split(":");
        if (lists.length == 0) { return points; }
        // Get the x and y lists of points
        String[] xLists = lists[0].split(";");
        String[] yLists = lists[1].split(";");
        for (int i = 0; i < xLists.length; i++) {
            String[] xPoints = xLists[i].split(",");
            String[] yPoints = yLists[i].split(",");
            ArrayList<Point> pList = new ArrayList<>();
            for (int j = 0; j < xPoints.length; j++) {
                pList.add(new Point(Integer.parseInt(xPoints[j]),
                        Integer.parseInt(yPoints[j])));
            }
            points.add(pList);
        }

        return points;
    }
}
