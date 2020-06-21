package com.example.myroutes.db.dao;

import androidx.room.TypeConverter;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StringArrayConverter {
    @TypeConverter
    public static String toString(List<List<String>> lists) {
        StringBuilder builder = new StringBuilder();
        for (List<String> list : lists) {
            for (String s : list) {
                builder.append(String.format("%s,", s));
            }
            builder.append(";");
        }
        return builder.toString();
    }

    @TypeConverter
    public static List<List<String>> toList(String s) {
        List<List<String>> lists = new ArrayList<>();
        // Check if empty
        if (s == null) { return lists; }
        // Get the x and y lists of points
        String[] groups = s.split(";");
        for (int i = 0; i < groups.length; i++) {
            String[] strs = groups[i].split(",");
            lists.add(new ArrayList<>(Arrays.asList(strs)));
        }

        return lists;
    }
}
