package com.example.myroutes.ui.addWall;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.AsyncTask;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class FindHoldsAsyncTask extends AsyncTask<Bitmap, Object, List<List<Point>>> {
    // The paths associated with the found points
    private List<Path> paths;
    // Called when task is done finding holds
    private BiConsumer<List<List<Point>>, List<Path>> onFinish;

    private static int MIN_HOLD_SIZE = 40;

    public FindHoldsAsyncTask(BiConsumer<List<List<Point>>, List<Path>> onFinish) {
        this.onFinish = onFinish;
    }

    @Override
    protected List<List<Point>> doInBackground(Bitmap... bitmaps) {
        List<List<Point>> points = findHoldContours(bitmaps[0]);
        this.paths = pathsFromPoints(points);
        return points;
    }

    @Override
    protected void onPostExecute(List<List<Point>> points) {
        super.onPostExecute(points);
        onFinish.accept(points, paths);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    // Helper function to convert points to paths
    private static List<Path> pathsFromPoints(List<List<Point>> points) {
        List<Path> paths = new ArrayList<>();
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

    // Try to find contours representing climbing holds
    private static List<List<Point>> findHoldContours(Bitmap resizedBitmap) {
        // Get bitmap dimensions
        int h = resizedBitmap.getHeight();
        int w = resizedBitmap.getWidth();

        // Convert image to gray
        Mat imgMat = new Mat(w, h, CvType.CV_8UC4);
        Mat grayMat = new Mat(w, h, CvType.CV_8UC1);
        Utils.bitmapToMat(resizedBitmap, imgMat);
        Imgproc.cvtColor(imgMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // Perform gaussian blur
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(7, 7), 3);

        // Find morphological gradient
        Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(7, 7));
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_GRADIENT, kernel);

        // Binarize gradient (to only keep foreground objects)
        Core.inRange(grayMat, new Scalar(15), new Scalar(255), grayMat);

        // Floodfill rows from edges (to make sure background isn't mistaken as foreground)
        for (int i = 0; i < h; i++) {
            if (grayMat.get(i, 0)[0] == 255) {
                Imgproc.floodFill(grayMat, new Mat(), new Point(0, i), new Scalar(0));
            }
            if (grayMat.get(i, w - 1)[0] == 255) {
                Imgproc.floodFill(grayMat, new Mat(), new Point(w - 1, i), new Scalar(0));
            }
        }

        // Floodfill cols from edges (to make sure background isn't mistaken as foreground)
        for (int i = 0; i < w; i++) {
            if (grayMat.get(0, i)[0] == 255) {
                Imgproc.floodFill(grayMat, new Mat(), new Point(i, 0), new Scalar(0));
            }
            if (grayMat.get(h - 1, i)[0] == 255) {
                Imgproc.floodFill(grayMat, new Mat(), new Point(i, h - 1), new Scalar(0));
            }
        }

        // Join and clean up edges of foreground objects
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.dilate(grayMat, grayMat, kernel, new Point(-1, -1), 3);
        Imgproc.erode(grayMat, grayMat, kernel, new Point(-1, -1), 3);

        // Estimate contours of holds
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.Canny(grayMat, grayMat, 50, 100);
        Imgproc.findContours(grayMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find convex hulls of contours to decrease noise
        // Save them as paths for drawing, and as a list of points for database
        MatOfInt hull = new MatOfInt();
        List<List<Point>> points = new ArrayList<>();
        for (MatOfPoint c : contours) {
            // Get hull
            Imgproc.convexHull(c, hull);

            // Skip contour if not enough points
            List<Point> contourPoints = c.toList();
            if (contourPoints.size() < MIN_HOLD_SIZE) continue;

            // Find actual points of hull
            List<Integer> contourIndices = hull.toList();
            List<Point> hullPoints = new ArrayList<>();
            for (int i : contourIndices) {
                hullPoints.add(contourPoints.get(i));
            }
            // Save points
            points.add(hullPoints);
        }

        return points;
    }
}
