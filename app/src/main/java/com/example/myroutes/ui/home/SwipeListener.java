package com.example.myroutes.ui.home;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

// Detects vertical and horizontal swipes
public class SwipeListener implements View.OnTouchListener {
    private float x1, y1;
    private int MIN_DISTANCE = 150;
    public enum SWIPE {LEFT, RIGHT, UP, DOWN};
    private Consumer<SWIPE> onSwipe;

    public SwipeListener(@NonNull Consumer<SWIPE> onSwipe) {
        this.onSwipe = onSwipe;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Look for swipes of size at least MIN_DISTANCE
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float x2 = event.getX();
                float y2 = event.getY();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;

                // Check for horizontal swipe
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // Left to Right swipe action
                    if (x2 > x1) { onSwipe.accept(SWIPE.LEFT); }
                    // Right to left swipe action
                    else { onSwipe.accept(SWIPE.RIGHT); }
                }
                // Check for vertical swipe
                if (Math.abs(deltaY) > MIN_DISTANCE) {
                    // Downward swipe action
                    if (y2 > y1) { onSwipe.accept(SWIPE.DOWN); }
                    else { onSwipe.accept(SWIPE.UP); }
                }
                break;
        }
        return true;
    }

    public void setMinDistance(int minDistance) {
        this.MIN_DISTANCE = minDistance;
    }

    public int getMin_distance() {
        return MIN_DISTANCE;
    }
}
