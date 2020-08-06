package com.example.myroutes.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import org.opencv.core.Point;

import java.util.List;

public class WallDrawingHelper {
    private static final String TAG = "WallDrawingHelper";

    // Paints for drawing and highlighting holds
    private static int drawColor = 0xFFff0000;
    private static int highlightColor = 0xFFffffff;
    private static int startHoldHighlightColor = 0xFF00ff00;
    private static int finishHoldHighlightColor = 0xFF0000ff;
    private static Paint drawPaint = setDrawingPaint();
    private static Paint highlightPaint = setHighlightPaint(highlightColor);
    public static Paint START_HOLD_PAINT = setHighlightPaint(startHoldHighlightColor);
    public static Paint FINISH_HOLD_PAINT = setHighlightPaint(finishHoldHighlightColor);
    private static Paint canvasPaint = setCanvasPaint();

    private static Paint setDrawingPaint() {
        Paint p = new Paint();
        p.setColor(drawColor);
        p.setAntiAlias(true);
        p.setStrokeWidth(5);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }

    private static Paint setHighlightPaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        p.setAntiAlias(true);
        p.setStrokeWidth(20);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setAlpha(150);
        return p;
    }

    private static Paint setCanvasPaint() {
        return new Paint(Paint.DITHER_FLAG);
    }

    public static Paint getDrawPaint() {
        return drawPaint;
    }

    public static Paint getHighlightPaint() {
        return highlightPaint;
    }

    public static Paint getCanvasPaint() {
        return canvasPaint;
    }

    public static Matrix getScalingMatrix(Bitmap wallBitmap, int maxHeight, int maxWidth) {
        int w = wallBitmap.getWidth();
        int h = wallBitmap.getHeight();

        // Create matrix to scale bitmap so its width and height don't exceed maxWidth and maxHeight
        Matrix matrix = new Matrix();
        float scale = (float) maxWidth / w;
        if (h * scale > maxHeight) { scale = (float) maxHeight / h; }
        matrix.postScale(scale, scale);

        return matrix;
    }

    public static float getScalingRatio(Bitmap wallBitmap, int maxHeight, int maxWidth) {
        int w = wallBitmap.getWidth();
        int h = wallBitmap.getHeight();

        // Create matrix to scale bitmap so its width and height don't exceed maxWidth and maxHeight
        float scale = (float) maxWidth / w;
        if (h * scale > maxHeight) { scale = (float) maxHeight / h; }
        return scale;
    }

    public static Path createTapePath(List<Point> points, float scale) {
        Point origin = getTapeOrigin(points);
        if (origin == null) return null;

        return getTapePath(origin, scale);
    }

    public static Point getTapeOrigin(List<Point> points) {
        if (points == null) return null;
        // Find the center of the points
        double cntrY = 0;
        double cntrX = 0;
        for (Point p : points) {
            cntrY += p.y;
            cntrX += p.x;
        }
        cntrY /= points.size();
        cntrX /= points.size();

        // Find the point that is closest to the bottom center
        Point best = null;
        double closest = 10000;
        for (Point p : points) {
            if (p.y < cntrY) continue;
            if (Math.abs(p.x - cntrX) < closest) {
                closest = Math.abs(p.x - cntrX);
                best = p;
            }
        }
        if (best == null) return null;
        return new Point(best.x ,best.y);
    }

    public static Path getTapePath(Point origin, float scale) {
        // Determine the dimensions of the tape
        int dx = 30;
        int dy = 50;
        origin.x *= scale;
        origin.y *= scale;
        // Draw the tape coming from that point
        Path tape = new Path();
        tape.moveTo((float) origin.x - dx, (float) origin.y + dy);
        tape.lineTo((float) origin.x, (float) origin.y);
        tape.lineTo((float) origin.x + dx, (float) origin.y + dy);
        return tape;
    }

    public static Bitmap resizeBitmap(Bitmap wallBitmap, Matrix matrix) {
        int w = wallBitmap.getWidth();
        int h = wallBitmap.getHeight();

        // Set new wall bitmap
        return Bitmap.createBitmap(wallBitmap, 0, 0, w, h, matrix, true);
    }
}
