package com.example.myroutes.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.example.myroutes.db.entities.BoulderItem;

import org.opencv.core.Point;

import java.util.List;

public class WallDrawingView extends androidx.appcompat.widget.AppCompatImageView {
    // Drawing variables
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint highlightPaint;
    private Paint canvasPaint;

    private Matrix matrix;
    private float matrixScale;

    private Bitmap imgBitmap;
    private List<Path> holdPaths;
    private List<List<Point>> holdPoints;

    public WallDrawingView(Context context) {
        super(context);
    }

    public WallDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WallDrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(Bitmap imgBitmap, List<Path> holdPaths, List<List<Point>> holdPoints) {
        this.imgBitmap = imgBitmap;
        this.holdPaths = holdPaths;
        this.holdPoints = holdPoints;
    }

    public void setSize(int maxWidth, int maxHeight) {
        // Image has not been initialized yet
        if (imgBitmap == null) return;

        // Find the matrix to scale image to fit on screen
        matrixScale = WallDrawingHelper.getScalingRatio(imgBitmap, maxHeight, maxWidth);
        matrix = new Matrix();
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
        highlightPaint = WallDrawingHelper.getHighlightPaint();
        canvasPaint = WallDrawingHelper.getCanvasPaint();

        // Transform the holds
        for (Path p : holdPaths) p.transform(matrix);

        // Draw paths
        this.setImageBitmap(drawingBitmap);
    }

    public void drawAllHolds() {
        for (Path p : holdPaths) {
            canvas.drawPath(p, drawPaint);
        }
        this.setImageBitmap(drawingBitmap);
    }

    public void drawBoulder(BoulderItem boulderItem) {
        List<Integer> holdIndices = boulderItem.getBoulder_holds();
        List<Integer> startHolds = boulderItem.getStart_holds();
        int finishHold = boulderItem.getFinish_hold();
        drawBoulderStyle(holdIndices, startHolds, finishHold, drawPaint);
    }

    public void highlightBoulder(List<Integer> holdIndices, List<Integer> startHolds, int finishHold) {
        drawBoulderStyle(holdIndices, startHolds, finishHold, highlightPaint);
    }

    public void highlightHold(int holdIdx) {
        canvas.drawPath(holdPaths.get(holdIdx), highlightPaint);
        this.setImageBitmap(drawingBitmap);
    }

    public void drawTape(int holdIdx, boolean isFinishHold) {
        Path tapePath = WallDrawingHelper.createTapePath(this.holdPoints.get(holdIdx), matrixScale);
        if (tapePath == null) return;
        Paint tapePaint = (isFinishHold) ? WallDrawingHelper.FINISH_HOLD_PAINT : WallDrawingHelper.START_HOLD_PAINT;
        canvas.drawPath(tapePath, tapePaint);
        this.setImageBitmap(drawingBitmap);
    }

    private void drawBoulderStyle(List<Integer> holdIndices, List<Integer> startHolds, int finishHold, Paint drawPaint) {
        for (int index : holdIndices) {
            //Path p = new Path(holdPaths.get(index));
            canvas.drawPath(holdPaths.get(index), drawPaint);
            if (startHolds.contains(index) || (finishHold == index)) {
               drawTape(index, finishHold == index);
            }
        }
        this.setImageBitmap(drawingBitmap);
    }

    // Clear previous drawn paths
    public void clearPaths() {
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        this.setImageBitmap(drawingBitmap);
    }

    // Return the index of the hold intersected by the point (touchX, touchY), or -1 if no hold
    // was tapped
    public int findTappedHold(float touchX, float touchY) {
        // Create regions to detect intersections between user touch and hold location
        Region region1 = new Region();
        Region region2 = new Region();

        // Create region of user tap
        Path touchRegion = new Path();
        touchRegion.moveTo(touchX, touchY);
        touchRegion.addCircle(touchX, touchY, 20, Path.Direction.CW);
        Region imageRegion = new Region(0, 0, this.getLayoutParams().width, this.getLayoutParams().height);
        region1.setPath(touchRegion, imageRegion);

        // Check if touch is inside any of the hold contours
        int nHolds = holdPaths.size();
        for (int i = 0; i < nHolds; i++) {
            Path p = holdPaths.get(i);
            region2.setPath(p, imageRegion);

            // Tap did not intersect with hold so continue
            if (region1.quickReject(region2)) continue;

            return i;
        }
        return -1;
    }
}
