package com.example.myroutes.ui.addWall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.mongoClasses.WallDataItem;
import com.example.myroutes.util.WallDrawingHelper;
import com.example.myroutes.db.mongoClasses.WallImageItem;
import com.example.myroutes.ui.manageWalls.SelectFromGalleryUtil;
import com.ortiz.touchview.TouchImageView;

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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.example.myroutes.ui.manageWalls.SelectFromGalleryUtil.GALLERY_REQUEST_CODE;

public class AddWallFragment extends Fragment {
    private static final String TAG = "AddWallFragment";

    // View models
    private SharedViewModel model;
    private AddWallFragmentModel fragmentModel;

    // Helper class to select image from gallery
    private SelectFromGalleryUtil galleryUtil;

    // Drawing variables
    private int vw_width;
    private int vw_height;
    private Matrix matrix;
    private Bitmap imgBitmap;
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;

    // Views
    private TouchImageView imageView;
    private MyFrameLayout frameLayout;
    private LinearLayout loadingLayout;
    private LinearLayout containerView;
    private TextView helperText;

    // Buttons and variables to keep track of adding/removing holds
    private Button addButton;
    private Button removeButton;
    private boolean addingHolds = false;
    private boolean removingHolds = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_addwall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(requireActivity()).get(AddWallFragmentModel.class);
        galleryUtil = new SelectFromGalleryUtil(requireActivity());

        // Get bitmap
        imgBitmap = fragmentModel.getImageBitmap();
        assert imgBitmap != null;

        // Get views
        imageView = view.findViewById(R.id.imageView4);
        frameLayout = view.findViewById(R.id.frameLayout2);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        containerView = view.findViewById(R.id.containerView);
        helperText = view.findViewById(R.id.helperMessage);

        // Handle buttons
        Button chooseNewImage = view.findViewById(R.id.changeImage);
        removeButton = view.findViewById(R.id.removeHolds);
        addButton = view.findViewById(R.id.addHolds);
        chooseNewImage.setOnClickListener(this::onClickChooseWall);
        removeButton.setOnClickListener(this::onRemoveHoldsClick);
        addButton.setOnClickListener(this::onAddHoldsClick);

        Button saveWall = view.findViewById(R.id.saveWallButton);
        saveWall.setOnClickListener(this::onSaveHoldsClick);

        // Listen for frameLayout to be inflated so we can resize it
        ViewTreeObserver vto = frameLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                adjustFrameSize();
            }
        });
    }

    private void onClickChooseWall(View view) {
        galleryUtil.requestExternalStoragePermissions();
        Intent intent = galleryUtil.createSelectFromGalleryIntent();
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Called when user selects an image from their gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                imgBitmap = galleryUtil.getBitmapFromIntent(data);

                // Something went wrong
                if (imgBitmap == null) { return; }

                // Reset wall
                fragmentModel.setImgBitmap(imgBitmap);
                imageView.setImageBitmap(null);
                loadingLayout.setVisibility(View.VISIBLE);
                adjustFrameSize();
            }
        }
    }

    private void onRemoveHoldsClick(View view) {
        removingHolds = !removingHolds;
        addingHolds = false;
        frameLayout.setIsDrawing(removingHolds);
        if (removingHolds) {
            removeButton.setBackgroundColor(0xffd6d7d7);
            addButton.setBackgroundColor(0x00000000);
        }
        else {
            removeButton.setBackgroundColor(0x00000000);
        }
    }

    private void onAddHoldsClick(View view) {
        addingHolds = !addingHolds;
        removingHolds = false;
        frameLayout.setIsDrawing(addingHolds);
        if (addingHolds) {
            addButton.setBackgroundColor(0xffd6d7d7);
            removeButton.setBackgroundColor(0x00000000);
        }
        else {
            addButton.setBackgroundColor(0x00000000);
        }
    }

    // Resize the frameLayout to wrap around image
    @SuppressLint("ClickableViewAccessibility")
    private void adjustFrameSize() {
        // Scale imgBitmap to fit on screen
        int maxWidth = containerView.getMeasuredWidth();
        int maxHeight = containerView.getMeasuredHeight() - helperText.getMinimumHeight();
        matrix = WallDrawingHelper.getScalingMatrix(imgBitmap, maxHeight, maxWidth);
        imgBitmap = WallDrawingHelper.resizeBitmap(imgBitmap, matrix);

        // Update layout params to match image
        ViewGroup.LayoutParams params = frameLayout.getLayoutParams();
        params.width = imgBitmap.getWidth();
        params.height = imgBitmap.getHeight();
        vw_width = params.width;
        vw_height = params.height;

        // Set drawing variables
        drawingBitmap = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(drawingBitmap);
        drawPaint = WallDrawingHelper.getDrawPaint();

        // Check if paths have already been found
        if (fragmentModel.getPaths() != null) {
            loadingLayout.setVisibility(View.GONE);
            imageView.setImageBitmap(imgBitmap);
            // Only allow user to interact with wall if screen is in portrait mode for now
            if (maxHeight > maxWidth) {
                drawPaths();
                frameLayout.setOnTouchListener(new FrameOnTouchListener());
            }
            return;
        }
        // Perform image analysis asynchronously
        new FindHoldsAsyncTask().execute(imgBitmap);
    }

    private void drawPaths() {
        ArrayList<Path> paths = fragmentModel.getPaths();
        for (Path p : paths) {
            canvas.drawPath(p, drawPaint);
        }
        imageView.setImageBitmap(drawingBitmap);
    }

    private void onSaveHoldsClick(View view) {
        Context context = requireContext();
        final FragmentActivity activity = requireActivity();

        // Build alert dialog
        AlertDialog alertDialog = AlertDialogManager.createAddWallDialog(context);
        alertDialog.show();

        // Get views
        EditText wallNameEditText = alertDialog.findViewById(R.id.wallName);
        Button cancelUserDataButton = alertDialog.findViewById(R.id.connectToWall);
        Button saveUserDataButton = alertDialog.findViewById(R.id.button_save_user_data);
        assert(wallNameEditText != null) && (cancelUserDataButton != null) && (saveUserDataButton != null);

        // Let user cancel dialog
        cancelUserDataButton.setOnClickListener(v -> alertDialog.cancel());

        // Let user save data
        saveUserDataButton.setOnClickListener(v -> {
            // Make sure wall has a name
            String wallName = wallNameEditText.getText().toString();
            if (wallName.isEmpty()) {
                String error = "Your wall must have a name";
                wallNameEditText.setError(error);
                return;
            }

            // Cancel the dialog
            alertDialog.cancel();

            // Create new objects to add to database
            String wall_id = UUID.randomUUID().toString().substring(0, 6);
            String stitchId = model.getStitchUserId();
            WallDataItem dataItem = new WallDataItem(stitchId, wall_id, wallName, fragmentModel.getPoints());
            WallImageItem imageItem = new WallImageItem(stitchId, wall_id, imgBitmap);

            // Save new wall
            model.createWall(dataItem, imageItem)
                    .observe(activity, result -> {
                        if (result == SharedViewModel.Status.SUCCESS) {
                            Toast.makeText(context.getApplicationContext(),
                                    "Added Wall " + wallName, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Added wall");
                        }
                    });
        });
    }

    private static ArrayList<Path> pathsFromPoints(ArrayList<ArrayList<Point>> points) {
        ArrayList<Path> paths = new ArrayList<>();
        for (ArrayList<Point> hold : points) {
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

    // TODO: move to different file?
    // Class to do image analysis in the background
    private class FindHoldsAsyncTask extends AsyncTask<Bitmap, Object, ArrayList<ArrayList<Point>>> {
        @Override
        protected ArrayList<ArrayList<Point>> doInBackground(Bitmap... bitmaps) {
            ArrayList<ArrayList<Point>> points = findHoldContours(bitmaps[0]);
            fragmentModel.setPoints(points);
            fragmentModel.setPaths(pathsFromPoints(points));
            return points;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onPostExecute(ArrayList<ArrayList<Point>> points) {
            super.onPostExecute(points);
            loadingLayout.setVisibility(View.GONE);
            drawPaths();
            frameLayout.setOnTouchListener(new FrameOnTouchListener());
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    // Try to find contours representing climbing holds
    @SuppressLint("ClickableViewAccessibility")
    private static ArrayList<ArrayList<Point>> findHoldContours(Bitmap resizedBitmap) {
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
        ArrayList<ArrayList<Point>> points = new ArrayList<>();
        for (MatOfPoint c : contours) {
            // Get hull
            Imgproc.convexHull(c, hull);

            // Find actual points of hull
            List<Point> contourPoints = c.toList();
            List<Integer> contourIndices = hull.toList();
            ArrayList<Point> hullPoints = new ArrayList<>();
            for (int i : contourIndices) {
                hullPoints.add(contourPoints.get(i));
            }
            // Save points
            points.add(hullPoints);
        }

        return points;
    }

    private class FrameOnTouchListener implements View.OnTouchListener {
        Path drawPath = new Path();
        ArrayList<Point> pathPoints;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Detect user touch and transform to bitmap coordinates
            float touchX = event.getX();
            float touchY = event.getY();
            PointF point = transformCoordinates(touchX, touchY);

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
                    canvas.drawPath(drawPath, drawPaint);
                    imageView.setImageBitmap(drawingBitmap); // Update imageView
                    break;
                // User stops drawing
                case MotionEvent.ACTION_UP:
                    // Save a new path
                    if (addingHolds) {
                        fragmentModel.addPoints(pathPoints);
                        addPath(drawPath);
                    }
                    // Remove all paths intersecting with current drawPath
                    else { removePaths(drawPath); }
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
            // Get center point and zoom of current image view
            PointF cntr = imageView.getScrollPosition();    // fractions b/w 0 and 1
            float zoom = imageView.getCurrentZoom();

            // Transform to the coordinates in the full image bitmap
            float x = cntr.x * vw_width + (x0 - vw_width / 2f) / zoom;
            float y = cntr.y * vw_height + (y0 - vw_height / 2f) / zoom;

            return new PointF(x, y);
        }

        // Save a new path
        private void addPath(Path p) {
            // Make sure path is closed
            p.close();
            canvas.drawPath(p, drawPaint);
            imageView.setImageBitmap(drawingBitmap); // Update imageView

            // Need to create copy of path before saving
            Path copy = new Path(p);
            fragmentModel.addPath(copy);
        }

        // Remove paths intersecting with Path p
        private void removePaths(Path p) {
            // Create regions representing paths to test intersection
            Region region1 = new Region();
            Region region2 = new Region();
            Region clip = new Region(0, 0, vw_width, vw_height);
            region1.setPath(p, clip);

            // Remove any path that intersects with Path p
            ArrayList<Path> paths = fragmentModel.getPaths();
            ArrayList<ArrayList<Point>> points = fragmentModel.getPoints();
            int nPaths = paths.size();
            for (int i = nPaths - 1; i >= 0;  i--) {
                Path path = paths.get(i);
                region2.setPath(path, clip);
                if (!region1.quickReject(region2)) {
                    paths.remove(i);
                    points.remove(i);
                }
            }
            fragmentModel.setPaths(paths);
            fragmentModel.setPoints(points);

            // Redraw canvas with new paths
            canvas.drawBitmap(imgBitmap, 0, 0, new Paint(Paint.DITHER_FLAG));
            drawPaths();
        }
    }
}
