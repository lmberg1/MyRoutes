package com.example.myroutes.ui.addWall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.text.method.Touch;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myroutes.util.WallDrawingHelper;
import com.example.myroutes.util.WallDrawingTouchImageView;
import com.ortiz.touchview.TouchImageView;

import org.opencv.core.Point;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;

public class DrawingListener implements View.OnTouchListener {
    private static final String TAG = "DrawingListener";

    // Keep track of current path
    Path drawPath = new Path();
    ArrayList<Point> pathPoints;

    // Image view variables
    private WeakReference<WallDrawingTouchImageView> imageViewWeakReference;
    private int vw_width;
    private int vw_height;

    // Callback after user completes a path
    private BiConsumer<Path, ArrayList<Point>> drawCallback;

    public DrawingListener(WeakReference<WallDrawingTouchImageView> imageView) {
        this.imageViewWeakReference = imageView;
        this.vw_width = imageViewWeakReference.get().getWidth();
        this.vw_height = imageViewWeakReference.get().getHeight();
    }

    public void setDrawCallback(BiConsumer<Path, ArrayList<Point>> drawCallback) {
        this.drawCallback = drawCallback;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Get image view
        TouchImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return false;
        imageView.performClick();

        // Detect user touch and transform to bitmap coordinates
        float touchX = event.getX();
        float touchY = event.getY();
        PointF point = transformCoordinates(touchX, touchY);
        if (point == null) return false;

        // Draw the path the user makes on the canvas
        switch (event.getAction()) {
            // User starts drawing
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(point.x, point.y);
                pathPoints = new ArrayList<>(Collections.singleton(new Point(point.x, point.y)));
                break;
            // User continues drawing
            case MotionEvent.ACTION_MOVE:
                // Draw path
                drawPath.lineTo(point.x, point.y);
                pathPoints.add(new Point(point.x, point.y));
                imageViewWeakReference.get().drawPath(drawPath);
                break;
            // User stops drawing
            case MotionEvent.ACTION_UP:
                // Call callback if it exists
                if (drawCallback != null) { drawCallback.accept(drawPath, pathPoints); }
                // Reset path
                drawPath.reset();
                break;
            default:
                return false;
        }

        return true;
    }

    // Transform touchImageView coordinates to image bitmap coordinates
    private PointF transformCoordinates(float x0, float y0) {
        TouchImageView imageView = imageViewWeakReference.get();
        if (imageView == null) return null;
        // Get center point and zoom of current image view
        PointF cntr = imageView.getScrollPosition();    // fractions b/w 0 and 1
        float zoom = imageView.getCurrentZoom();

        // Transform to the coordinates in the full image bitmap
        float x = cntr.x * vw_width + (x0 - vw_width / 2f) / zoom;
        float y = cntr.y * vw_height + (y0 - vw_height / 2f) / zoom;

        return new PointF(x, y);
    }

}
