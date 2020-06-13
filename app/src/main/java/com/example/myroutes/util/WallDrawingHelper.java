package com.example.myroutes.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

public class WallDrawingHelper {
    private static final String TAG = "WallDrawingHelper";

    // Paints for drawing and highlighting holds
    private static int drawColor = 0xFFff0000;
    private static int highlightColor = 0xFFffffff;
    private static Paint drawPaint = setDrawingPaint();
    private static Paint highlightPaint = setHighlightPaint();
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

    private static Paint setHighlightPaint() {
        Paint p = new Paint();
        p.setColor(highlightColor);
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

    public static Bitmap resizeBitmap(Bitmap wallBitmap, Matrix matrix) {
        int w = wallBitmap.getWidth();
        int h = wallBitmap.getHeight();

        // Set new wall bitmap
        return Bitmap.createBitmap(wallBitmap, 0, 0, w, h, matrix, true);
    }

}
