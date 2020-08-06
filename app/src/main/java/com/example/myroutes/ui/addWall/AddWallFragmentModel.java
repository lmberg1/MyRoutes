package com.example.myroutes.ui.addWall;

import android.graphics.Bitmap;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class AddWallFragmentModel extends ViewModel {

    private Bitmap imgBitmap;
    private List<Path> paths;
    private List<List<Point>> points;

    public AddWallFragmentModel() {
        this.paths = new ArrayList<>();
        this.points = new ArrayList<>();
    }

    public void setImgBitmap(Bitmap bitmap) {
        imgBitmap = bitmap;
    }

    public Bitmap getImageBitmap() {
        return imgBitmap;
    }

    public void setPaths(@NonNull List<Path> paths) {
        this.paths = paths;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void setPoints(List<List<Point>> points) {
        this.points = points;
    }

    public List<List<Point>> getPoints() {
        return points;
    }

    public void addPoints(List<Point> pointList) {
        this.points.add(pointList);
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public void clearPaths() {
        this.paths.clear();
        this.points.clear();
    }
}