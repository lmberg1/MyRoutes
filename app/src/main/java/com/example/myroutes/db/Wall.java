package com.example.myroutes.db;

import android.graphics.Bitmap;
import android.graphics.Path;

import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.db.mongoClasses.WallImageItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Wall {
    private WallDataItem data;
    private WallImageItem image;
    private HashMap<String, List<BoulderItem>> boulders;

    public Wall(WallDataItem data, WallImageItem image, HashMap<String, List<BoulderItem>> boulders) {
        this.data = data;
        this.image = image;
        this.boulders = (boulders == null) ? new HashMap<>() : boulders;
    }

    public String getId() {
        return data.getWall_id();
    }

    public String getName() {
        return data.getWall_name();
    }

    public void setName(String name) {
        data.setWall_name(name);
    }

    public ArrayList<Path> getPaths() {
        return data.getPaths();
    }

    public Bitmap getBitmap() {
        return image.getBitmap();
    }

    public HashMap<String, List<BoulderItem>> getBoulders() {
        return boulders;
    }

    public void addBoulder(BoulderItem item) {
        String grade = item.getBoulder_grade();
        List<BoulderItem> boulderItems = boulders.getOrDefault(grade, new ArrayList<>());
        assert boulderItems != null;
        boulderItems.add(item);
        this.boulders.put(grade, boulderItems);
    }
}
