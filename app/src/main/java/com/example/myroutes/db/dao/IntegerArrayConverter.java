package com.example.myroutes.db.dao;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntegerArrayConverter {
    @TypeConverter
    public static String toString(List<Integer> holds) {
        StringBuilder s = new StringBuilder();
        for (Integer i : holds) {
            s.append(i.toString());
            s.append(",");
        }
        return s.toString();
    }

    @TypeConverter
    public static List<Integer> toList(String s) {
        List<String> items = Arrays.asList(s.split("\\s*,\\s*"));
        List<Integer> list = new ArrayList<>();
        for (String i : items) {
            list.add(Integer.parseInt(i));
        }
        return list;
    }
}
