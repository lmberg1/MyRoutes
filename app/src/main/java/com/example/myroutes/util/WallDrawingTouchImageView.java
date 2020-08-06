package com.example.myroutes.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.example.myroutes.db.entities.BoulderItem;
import com.ortiz.touchview.TouchImageView;

import org.opencv.core.Point;

import java.util.List;

public class WallDrawingTouchImageView extends TouchImageView {
    // Drawing variables
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint canvasPaint;

    private Bitmap imgBitmap;

    public WallDrawingTouchImageView(Context context) {
        super(context);
    }

    public WallDrawingTouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WallDrawingTouchImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(Bitmap imgBitmap) {
        this.imgBitmap = imgBitmap;
    }

    public Bitmap setSize(int maxWidth, int maxHeight) {
        // Image has not been initialized yet
        if (imgBitmap == null) return null;

        // Find the matrix to scale image to fit on screen
        float matrixScale = WallDrawingHelper.getScalingRatio(imgBitmap, maxHeight, maxWidth);
        Matrix matrix = new Matrix();
        matrix.postScale(matrixScale, matrixScale);
        this.imgBitmap = WallDrawingHelper.resizeBitmap(imgBitmap, matrix);

        // Update layout params to match image
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = imgBitmap.getWidth();
        params.height = imgBitmap.getHeight();

        // Set drawing variables
        drawingBitmap = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(drawingBitmap);
        drawPaint = WallDrawingHelper.getDrawPaint();
        canvasPaint = WallDrawingHelper.getCanvasPaint();

        // Draw paths
        this.setImageBitmap(drawingBitmap);
        return imgBitmap;
    }

    public void drawAllPaths(List<Path> paths) {
        for (Path p : paths) {
            canvas.drawPath(p, drawPaint);
        }
        this.setImageBitmap(drawingBitmap);
    }

    public void drawPath(Path p) {
        canvas.drawPath(p, drawPaint);
        this.setImageBitmap(drawingBitmap);
    }

    // Clear previous drawn paths
    public void clearPaths() {
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        this.setImageBitmap(drawingBitmap);
    }
}
