package com.example.myroutes.ui.addWall;

import android.graphics.Bitmap;
import android.graphics.Path;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.opencv.core.Point;

import java.util.ArrayList;

public class AddWallFragmentModel extends ViewModel {

    private Bitmap imgBitmap;
    private ArrayList<Path> paths;
    private ArrayList<ArrayList<Point>> points;

    public AddWallFragmentModel() { }

    public void setImgBitmap(Bitmap bitmap) {
        imgBitmap = bitmap;
    }

    public Bitmap getImageBitmap() {
        return imgBitmap;
    }

    public void setPaths(ArrayList<Path> paths) {
        this.paths = paths;
    }

    public ArrayList<Path> getPaths() {
        return paths;
    }

    public void setPoints(ArrayList<ArrayList<Point>> points) {
        this.points = points;
    }

    public ArrayList<ArrayList<Point>> getPoints() {
        return points;
    }

    public void addPoints(ArrayList<Point> pointList) {
        this.points.add(pointList);
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }
}