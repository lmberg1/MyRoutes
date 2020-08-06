package com.example.myroutes.ui.addWall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.R;
import com.example.myroutes.SharedViewModel;
import com.example.myroutes.db.entities.WallDataItem;
import com.example.myroutes.util.WallDrawingHelper;
import com.example.myroutes.db.entities.WallImageItem;
import com.example.myroutes.ui.manageWalls.SelectFromGalleryUtil;
import com.example.myroutes.util.WallDrawingTouchImageView;
import com.ortiz.touchview.TouchImageView;

import org.opencv.core.Point;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.example.myroutes.ui.manageWalls.SelectFromGalleryUtil.GALLERY_REQUEST_CODE;

public class AddWallFragment extends Fragment {
    private static final String TAG = "AddWallFragment";

    // View models
    private SharedViewModel model;
    private AddWallFragmentModel fragmentModel;

    // Helper class to select image from gallery
    private SelectFromGalleryUtil galleryUtil;

    // Image variables
    private WallDrawingTouchImageView imageView;
    private Bitmap imgBitmap;
    private Region imageRegion;

    // Views
    private MyFrameLayout frameLayout;
    private LinearLayout loadingLayout;
    private LinearLayout containerView;
    private TextView helperText;

    // Buttons and variables to keep track of adding/removing holds
    private Button addButton;
    private Button removeButton;
    private boolean addingHolds = false;
    private boolean removingHolds = false;

    private static final class MESSAGE {
        static final String EMPTY_NAME = "Your wall must have a name";
        static final String WALL_NAME_LENGTH = "Your wall name must have fewer than 50 characters";
    }

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

        // Handle back navigation
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(this::goToManageWallsFragment);

        // Get views
        imageView = view.findViewById(R.id.imageView4);
        frameLayout = view.findViewById(R.id.frameLayout2);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        containerView = view.findViewById(R.id.containerView);
        helperText = view.findViewById(R.id.helperMessage);

        // Get bitmap
        imgBitmap = fragmentModel.getImageBitmap();
        if (imgBitmap == null) return;

        // Handle buttons
        Button saveWall = view.findViewById(R.id.saveWallButton);
        Button chooseNewImage = view.findViewById(R.id.changeImage);
        removeButton = view.findViewById(R.id.removeHolds);
        addButton = view.findViewById(R.id.addHolds);
        chooseNewImage.setOnClickListener(this::onClickChooseWall);
        removeButton.setOnClickListener(this::onRemoveHoldsClick);
        addButton.setOnClickListener(this::onAddHoldsClick);
        saveWall.setOnClickListener(this::onSaveHoldsClick);

        // Listen for frameLayout to be inflated so we can resize it
        ViewTreeObserver vto = frameLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                frameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setTransformation();
            }
        });
    }

    private void goToManageWallsFragment(View v) {
        NavHostFragment.findNavController(AddWallFragment.this)
                .navigate(R.id.nav_manage_walls);
    }

    // Resize the frameLayout to wrap around image
    @SuppressLint("ClickableViewAccessibility")
    private void setTransformation() {
        // Scale imgBitmap to fit on screen
        int maxWidth = containerView.getMeasuredWidth();
        int maxHeight = containerView.getMeasuredHeight() - helperText.getMinimumHeight();

        imageView.initialize(imgBitmap);
        imgBitmap = imageView.setSize(maxWidth, maxHeight);

        imageRegion = new Region(0, 0, imageView.getLayoutParams().width, imageView.getLayoutParams().height);
        frameLayout.getLayoutParams().width = imageView.getLayoutParams().width;
        frameLayout.getLayoutParams().height = imageView.getLayoutParams().height;

        // Check if paths have already been found
        if (fragmentModel.getPaths().size() != 0) {
            loadingLayout.setVisibility(View.GONE);
            imageView.setImageBitmap(imgBitmap);
            // Only allow user to interact with wall if screen is in portrait mode for now
            if (maxHeight > maxWidth) {
                drawPaths();
                //frameLayout.setOnTouchListener(new FrameOnTouchListener());
            }
            return;
        }
        // Perform image analysis asynchronously
        new FindHoldsAsyncTask(this::onFindHoldsFinish).execute(imgBitmap);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onFindHoldsFinish(List<List<Point>> points, List<Path> paths) {
        // Update fragment model
        fragmentModel.setPoints(points);
        fragmentModel.setPaths(paths);
        // Remove loading progress bar and draw the holds
        loadingLayout.setVisibility(View.GONE);
        drawPaths();
        // Allow user to interact with image
        DrawingListener drawingListener = new DrawingListener(new WeakReference<>(imageView));
        drawingListener.setDrawCallback(this::onDrawPathFinished);
        frameLayout.setOnTouchListener(drawingListener);
    }

    /*--------------------------------Handle Choosing a New Wall----------------------------------*/

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
                fragmentModel.clearPaths();
                fragmentModel.setImgBitmap(imgBitmap);
                // Remove old image and display loading dialog
                imageView.setImageBitmap(null);
                loadingLayout.setVisibility(View.VISIBLE);
                setTransformation();
            }
        }
    }

    /*-------------------------------Handle Editing Buttons---------------------------------------*/

    private void onRemoveHoldsClick(View view) {
        removingHolds = !removingHolds;
        addingHolds = false;
        frameLayout.setIsDrawing(removingHolds);
        if (removingHolds) {
            selectDrawable(removeButton);
            deselectDrawable(addButton, requireContext());
        }
        else {
            deselectDrawable(removeButton, requireContext());
        }
    }

    private void onAddHoldsClick(View view) {
        addingHolds = !addingHolds;
        removingHolds = false;
        frameLayout.setIsDrawing(addingHolds);
        if (addingHolds) {
            selectDrawable(addButton);
            deselectDrawable(removeButton, requireContext());
        }
        else {
            deselectDrawable(addButton, requireContext());
        }
    }

    private static void setDrawableTint(Button button, int color) {
        button.setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }

    private static void selectDrawable(Button button) {
        setDrawableTint(button, 0xFFE857A2);
    }

    private static void deselectDrawable(Button button, Context context) {
        setDrawableTint(button, context.getColor(R.color.colorAccent));
    }

    /*-------------------------------------Drawing Callbacks--------------------------------------*/

    private void onDrawPathFinished(Path p, ArrayList<Point> pathPoints) {
        // Add a new path
        if (addingHolds) { addPath(p, pathPoints); }
        // Remove all paths intersecting with Path p
        else { removePath(p); }
    }

    private void addPath(Path p, ArrayList<Point> pathPoints) {
        // Make sure path is closed
        p.close();
        imageView.drawPath(p);
        // Need to create copy of path before saving
        Path copy = new Path(p);
        fragmentModel.addPath(copy);
        fragmentModel.addPoints(pathPoints);
    }

    private void removePath(Path p) {
        // Create regions representing paths to test intersection
        Region region1 = new Region();
        Region region2 = new Region();
        region1.setPath(p, imageRegion);

        // Remove any path that intersects with Path p
        List<Path> paths = fragmentModel.getPaths();
        List<List<Point>> points = fragmentModel.getPoints();
        int nPaths = paths.size();
        for (int i = nPaths - 1; i >= 0;  i--) {
            Path path = paths.get(i);
            region2.setPath(path, imageRegion);
            if (!region1.quickReject(region2)) {
                paths.remove(i);
                points.remove(i);
            }
        }
        fragmentModel.setPaths(paths);
        fragmentModel.setPoints(points);

        // Redraw canvas with new paths
        drawPaths();
    }

    private void drawPaths() {
        imageView.clearPaths();
        imageView.drawAllPaths(fragmentModel.getPaths());
    }

    /*-----------------------------------Handle Saving Wall---------------------------------------*/

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

        // Handle cancel and save buttons
        cancelUserDataButton.setOnClickListener(v -> alertDialog.cancel());
        saveUserDataButton.setOnClickListener(v -> {
            // Check for errors
            if (checkForWallIdError(wallNameEditText)) return;

            // Cancel the dialog
            alertDialog.cancel();

            // Resize image and points to store in DB
            resizeDataforDB();

            // Create new objects to add to database
            String wallName = wallNameEditText.getText().toString();
            String wall_id = UUID.randomUUID().toString().substring(0, 6);
            String stitchId = model.getStitchUserId();
            WallDataItem dataItem = new WallDataItem(stitchId, wall_id, wallName, fragmentModel.getPoints());
            WallImageItem imageItem = new WallImageItem(stitchId, wall_id, imgBitmap);

            // Save new wall
            model.createWall(dataItem, imageItem).observe(activity, result -> {
                if (result == SharedViewModel.Status.SUCCESS) {
                    Toast.makeText(context.getApplicationContext(),
                            "Added Wall " + wallName, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Added wall");
                }
            });
        });
    }

    private void resizeDataforDB() {
        int maxDim = 1000;
        int h = imgBitmap.getHeight();
        int w = imgBitmap.getWidth();
        // Scale imgBitmap down so that its maximum dimension is 1000 pixels
        Matrix scalingMatrix = WallDrawingHelper.getScalingMatrix(imgBitmap, maxDim, maxDim);
        imgBitmap = WallDrawingHelper.resizeBitmap(imgBitmap, scalingMatrix);

        // Scale down the points in hold contours to match the scaled bitmap
        List<List<Point>> points = fragmentModel.getPoints();
        double scalingRatio = (w > h) ? (double) maxDim / w :  (double) maxDim / h;
        for (List<Point> hold : points) {
            for (Point p : hold) {
                p.x *= scalingRatio;
                p.y *= scalingRatio;
            }
        }
        fragmentModel.setPoints(points);
    }

    private boolean checkForWallIdError(EditText editText) {
        String wallId = editText.getText().toString();
        if (wallId.isEmpty()) {
            editText.setError(MESSAGE.EMPTY_NAME);
            return true;
        }
        if (wallId.length() > 50) {
            editText.setError(MESSAGE.WALL_NAME_LENGTH);
            return true;
        }
        return false;
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        //drawingBitmap.recycle();
    }
}
