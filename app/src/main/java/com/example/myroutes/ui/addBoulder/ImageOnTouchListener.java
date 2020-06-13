package com.example.myroutes.ui.addBoulder;

import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.common.util.BiConsumer;

import java.util.ArrayList;

public class ImageOnTouchListener implements View.OnTouchListener {
    private static final int MAX_TAP_SIZE = 10;
    private Path drawPath = new Path();
    private ArrayList<PointF> points = new ArrayList<>();
    private BiConsumer<Float, Float> onTapDetectCallback;

    // Create listener which calls callback when it detects a tap
    public ImageOnTouchListener(BiConsumer<Float, Float> callback) {
        this.onTapDetectCallback = callback;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        // Draw the path the user makes on the canvas
        switch (event.getAction()) {
            // User is touching
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                points.add(new PointF(touchX, touchY));
                break;
            // User stops touching
            case MotionEvent.ACTION_UP:
                if (points.size() < MAX_TAP_SIZE) {
                    onTapDetectCallback.accept(touchX, touchY);
                }
                // Reset points
                points.clear();

                break;
            default:
                return false;
        }

        return true;
    }
}
