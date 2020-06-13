package com.example.myroutes.ui.addBoulder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.Wall;
import com.example.myroutes.util.WallDrawingHelper;

import java.util.ArrayList;

public class AddBoulderFragment extends Fragment {
    private static final String TAG = "AddBoulderFragment";

    // View models
    private SharedViewModel model;
    private AddBoulderModel fragmentModel;

    // Wall data
    private String wall_id;
    private Wall wall;
    private ArrayList<Path> holdPaths;

    // Views
    private ImageView imageView;
    private TextView imageBottom;
    private Region imageRegion;

    // Drawing variables
    private Bitmap imgBitmap;
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint highlightPaint;
    private Paint canvasPaint;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_boulder, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize models
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(this).get(AddBoulderModel.class);

        // Handle setting of current wall
        final ProgressBar progressBar = view.findViewById(R.id.progressBar_cyclic);
        final TextView messageText = view.findViewById(R.id.messageText);
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                String message = "You have no walls. Go to the Manage Walls panel to add one.";
                messageText.setText(message);
            }
            else if (result == SharedViewModel.Status.FAILURE) {
                progressBar.setVisibility(View.GONE);
                String message = "Oh no. Something went wrong. Make sure you are connected to the internet and try again.";
                messageText.setText(message);
            }
            else if (result == SharedViewModel.Status.NOT_FOUND) {
                progressBar.setVisibility(View.GONE);
                String message = "Oh no. The wall you tried to go to no longer exists.";
                messageText.setText(message);
            }
            else if (result == SharedViewModel.Status.LOADING) {
                progressBar.setVisibility(View.VISIBLE);
            }
            else if (result == SharedViewModel.Status.SUCCESS) {
                progressBar.setVisibility(View.GONE);
                this.wall_id = model.getCurrentWallId();
                this.wall = model.getWall(wall_id);
                this.imgBitmap = wall.getBitmap();
                setupView(view);
            }
        });
    }

    private void setupView(View view) {
        // Set title
        TextView title = view.findViewById(R.id.title);
        title.setText(wall.getName());

        // Handle buttons
        Button saveButton = view.findViewById(R.id.saveBoulder);
        Button clearButton = view.findViewById(R.id.clearHolds);
        saveButton.setOnClickListener(this::onSaveBoulderClick);
        clearButton.setOnClickListener(v -> { fragmentModel.clearHighlightedHolds(); drawPaths(); });

        // Initialize paths
        ArrayList<Path> allPaths = wall.getPaths();
        holdPaths = new ArrayList<>();
        for (Path p : allPaths) {
            Path copy = new Path(p);
            holdPaths.add(copy);
        }

        // Listen for imageView to be inflated so we can resize it
        imageBottom = view.findViewById(R.id.helperMessage);
        imageView = view.findViewById(R.id.imageView2);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setTransformation();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTransformation() {
        // Scale imgBitmap to fit on screen
        int maxHeight = imageView.getMeasuredHeight() - imageBottom.getMinimumHeight();
        int maxWidth = imageView.getMeasuredWidth();
        Matrix matrix = WallDrawingHelper.getScalingMatrix(imgBitmap, maxHeight, maxWidth);
        imgBitmap = WallDrawingHelper.resizeBitmap(imgBitmap, matrix);

        // Update layout params to match image
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = imgBitmap.getWidth();
        params.height = imgBitmap.getHeight();
        imageRegion = new Region(0, 0, params.width, params.height);

        // Set drawing variables
        drawingBitmap = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(drawingBitmap);
        drawPaint = WallDrawingHelper.getDrawPaint();
        highlightPaint = WallDrawingHelper.getHighlightPaint();
        canvasPaint = WallDrawingHelper.getCanvasPaint();

        // Transform hold paths
        for (Path p : holdPaths) {
            p.close();
            p.transform(matrix);
        }

        // Draw paths
        imageView.setImageBitmap(drawingBitmap);
        drawPaths();

        // Allow user to select holds
        imageView.setOnTouchListener(new ImageOnTouchListener(this::onDetectTap));
    }

    private void drawPaths() {
        // Clear canvas
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);

        // Draw all of the holds
        for (Path p : holdPaths) {
            canvas.drawPath(p, drawPaint);
        }

        // Draw the highlighted holds
        for (int j : fragmentModel.getHighlightedHolds()) {
            canvas.drawPath(holdPaths.get(j), highlightPaint);
        }
        imageView.setImageBitmap(drawingBitmap);
    }

    private void onDetectTap(float touchX, float touchY) {
        // Create regions to detect intersections between user touch and hold location
        Region region1 = new Region();
        Region region2 = new Region();

        // Create region of user tap
        Path touchRegion = new Path();
        touchRegion.moveTo(touchX, touchY);
        touchRegion.addCircle(touchX, touchY, 20, Path.Direction.CW);
        region1.setPath(touchRegion, imageRegion);

        // Check if touch is inside any of the hold contours
        int nHolds = holdPaths.size();
        for (int i = 0; i < nHolds; i++) {
            Path p = holdPaths.get(i);
            region2.setPath(p, imageRegion);

            // Check if touch intersects with hold path
            if (!region1.quickReject(region2)) {
                // Hold is already highlighted so unselect it
                if (fragmentModel.hasHighlighedHold(i)) {
                    fragmentModel.removeHighlightedHold(i);
                    drawPaths();
                }
                // Hold needs to be highlighted
                else {
                    fragmentModel.addHighlightedHold(holdPaths.indexOf(p));
                    canvas.drawPath(p, highlightPaint);
                    imageView.setImageBitmap(drawingBitmap);
                }
                break;
            }
        }
    }

    private void onSaveBoulderClick(View view) {
        Context context = requireContext();
        FragmentActivity activity = requireActivity();

        // Handle errors (there must be at least 3 holds to save a boulder)
        int nHolds = fragmentModel.getHighlightedHolds().size();
        if (nHolds < 3) {
            if (nHolds == 0) {
                String error = "Your did not select any holds!";
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
            else if (nHolds == 1) {
                String error = "You only selected one hold!";
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
            else {
                String error = "You only selected two holds!";
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Create alert dialog
        AlertDialog alertDialog = AlertDialogManager.createAddBoulderDialog(context);
        alertDialog.show();

        // Get views
        Button cancelUserDataButton = alertDialog.findViewById(R.id.connectToWall);
        Button saveUserDataButton = alertDialog.findViewById(R.id.button_save_user_data);
        EditText boulderNameEditText = alertDialog.findViewById(R.id.boulderName);
        Spinner spinner = alertDialog.findViewById(R.id.saveBoulderSpinner);
        assert (cancelUserDataButton != null) && (saveUserDataButton != null) &&
                (boulderNameEditText != null) && (spinner != null);

        // Let user cancel dialog
        cancelUserDataButton.setOnClickListener(v -> alertDialog.cancel());

        // Let user save data
        saveUserDataButton.setOnClickListener(v -> {
            // Close the dialog
            alertDialog.cancel();

            // Get wall info
            String boulderName = boulderNameEditText.getText().toString();
            String grade = (String) spinner.getSelectedItem();

            // Create new boulder item
            BoulderItem boulderItem = new BoulderItem(model.getStitchUserId(), wall_id,
                    boulderName, grade, fragmentModel.getHighlightedHolds());

            // Save the boulder item
            model.addBoulderItem(boulderItem).observe(activity, result -> {
                if (result == SharedViewModel.Status.SUCCESS) {
                    Toast.makeText(activity.getApplicationContext(),
                            "Added Boulder  " + boulderName,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
