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
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.entities.Wall;
import com.example.myroutes.util.WallDrawingHelper;
import com.example.myroutes.util.WallLoadingErrorHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static com.example.myroutes.db.SharedViewModel.BOULDER_GRADES;

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

    // Edit boulder mode views
    private TextView title;
    private Button deleteBoulder;
    private Button resetBoulder;

    // Drawing variables
    private Bitmap imgBitmap;
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint highlightPaint;
    private Paint canvasPaint;

    // Boulder item that we are editing (if in editing mode)
    private BoulderItem boulderItem;

    private static final class MESSAGE {
        static final String WRONG_HOLD_NUMBER = "Your boulder must have at least 3 holds.";
        static final String TITLE_LENGTH = "Your boulder name must have fewer than 50 characters";
    }

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
        fragmentModel = new ViewModelProvider(requireActivity()).get(AddBoulderModel.class);

        // Handle setting of current wall
        final ProgressBar progressBar = view.findViewById(R.id.progressBar_cyclic);
        final TextView messageText = view.findViewById(R.id.error_message);
        messageText.setVisibility(View.VISIBLE);
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            // Check if there was an error
            boolean success = WallLoadingErrorHandler.handleError(result, progressBar, messageText);
            if (success) {
                this.wall_id = model.getCurrentWallId();
                this.wall = model.getWall(wall_id);
                this.imgBitmap = wall.getBitmap();
                setupView(view);
            }
        });
    }

    private void setupView(View view) {
        // Get views
        title = view.findViewById(R.id.title);
        deleteBoulder = view.findViewById(R.id.deleteBoulder);
        resetBoulder = view.findViewById(R.id.resetHolds);
        imageBottom = view.findViewById(R.id.helperMessage);
        imageView = view.findViewById(R.id.imageView2);

        // Initialize paths
        ArrayList<Path> allPaths = wall.getPaths();
        holdPaths = new ArrayList<>();
        for (Path p : allPaths) {
            Path copy = new Path(p);
            holdPaths.add(copy);
        }

        // Check if homeFragment passed a boulderItem to edit
        if (fragmentModel.getBoulder() != null) { displayEditView(); }

        // Handle buttons
        Button saveButton = view.findViewById(R.id.saveBoulder);
        Button clearButton = view.findViewById(R.id.clearHolds);
        saveButton.setOnClickListener(this::onSaveBoulderClick);
        clearButton.setOnClickListener(v -> { fragmentModel.clearHighlightedHolds(); drawPaths(); });

        // Listen for imageView to be inflated so we can resize it
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setTransformation();
            }
        });
    }

    // Change the Add Boulder display if we are editing a boulder rather than creating one
    private void displayEditView() {
        this.boulderItem = fragmentModel.getBoulder();
        fragmentModel.setHighlightedHolds(boulderItem.getBoulder_holds());
        // Change title of fragment
        title.setText(String.format("Editing \"%s (%s)\"",
                boulderItem.getBoulder_name(), boulderItem.getBoulder_grade()));
        // Show buttons for deleting boulder and reseting boulder holds
        deleteBoulder.setVisibility(View.VISIBLE);
        resetBoulder.setVisibility(View.VISIBLE);
        deleteBoulder.setOnClickListener(this::onDeleteBoulderClick);
        resetBoulder.setOnClickListener(this::onResetHoldsClick);
    }

    // Change the Add Boulder display if we are creating a boulder
    private void displayAddView() {
        // Hide buttons and reset title
        title.setText(String.format("%s", "Add Boulder"));
        deleteBoulder.setVisibility(View.GONE);
        resetBoulder.setVisibility(View.GONE);
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

        // Transform hold paths to fit on image
        for (Path p : holdPaths) { p.transform(matrix); }

        // Draw paths
        imageView.setImageBitmap(drawingBitmap);
        drawPaths();

        // Allow user to select holds
        imageView.setOnTouchListener(new ImageOnTouchListener(this::onDetectTap));
    }

    /*-------------------------------Handle Display of Holds--------------------------------------*/

    // Toggle highlighting of tapped holds
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

            // Tap did not intersect with hold so continue
            if (region1.quickReject(region2)) continue;

            // Hold is already highlighted so unhighlight it
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
            // Break so that user tap doesn't affect multiple holds
            break;
        }
    }

    // Reset highlighted holds (if editing a boulder)
    private void onResetHoldsClick(View view) {
        fragmentModel.setHighlightedHolds(boulderItem.getBoulder_holds());
        drawPaths();
    }

    // Draw all the holds along with the highlighted holds
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

    /*---------------------------Handle Saving/Deleting Boulders----------------------------------*/

    private void onDeleteBoulderClick(View view) {
        Context context = requireContext();
        FragmentActivity activity = requireActivity();

        // Create alert dialog
        AlertDialog alertDialog = AlertDialogManager.createDeleteBoulderDialog(boulderItem, context);
        alertDialog.show();

        // Get views
        Button cancel = alertDialog.findViewById(R.id.cancel);
        Button delete = alertDialog.findViewById(R.id.delete);
        assert (cancel != null) && (delete != null);

        cancel.setOnClickListener(v -> alertDialog.cancel());
        delete.setOnClickListener(v -> {
            // Delete the boulder
            model.deleteBoulderItem(boulderItem);
            String name = boulderItem.getBoulder_name();
            Toast.makeText(activity.getApplicationContext(), "Deleted Boulder " + name,
                    Toast.LENGTH_SHORT).show();
            // Clear the boulder item and any highlighted holds
            alertDialog.cancel();
            boulderItem = null;
            fragmentModel.clearHighlightedHolds();
            drawPaths();
            // Reset the Add Boulder view
            displayAddView();
        });
    }

    private void onSaveBoulderClick(View view) {
        Context context = requireContext();
        FragmentActivity activity = requireActivity();

        // Don't show dialog if there aren't enough boulders selected
        if (checkSaveBoulderErrors()) return;

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

        // Autofill dialog if we are editing a boulder rather than creating one
        if (boulderItem != null) {
            boulderNameEditText.setText(boulderItem.getBoulder_name());
            spinner.setSelection(Arrays.asList(BOULDER_GRADES).indexOf(boulderItem.getBoulder_grade()));
        }

        cancelUserDataButton.setOnClickListener(v -> alertDialog.cancel());
        saveUserDataButton.setOnClickListener(v -> {
            // Get wall info
            String boulderName = boulderNameEditText.getText().toString();
            String grade = (String) spinner.getSelectedItem();

            // Check for errors in the title
            if (boulderName.length() > 50) {
                boulderNameEditText.setError(MESSAGE.TITLE_LENGTH);
                return;
            }

            // Close the dialog
            alertDialog.cancel();

            // Get or create boulder id
            String boulder_id = (boulderItem != null) ? boulderItem.getBoulder_id() :
                    UUID.randomUUID().toString().substring(0, 8);

            // Create boulder item
            boulderItem = new BoulderItem(model.getStitchUserId(), wall_id, boulder_id,
                    boulderName, grade, fragmentModel.getHighlightedHolds());

            // Save the boulder item
            model.addBoulderItem(boulderItem);
            Toast.makeText(activity.getApplicationContext(), "Added Boulder " + boulderName,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkSaveBoulderErrors() {
        // Handle errors (there must be at least 3 holds to save a boulder)
        int nHolds = fragmentModel.getHighlightedHolds().size();
        if (nHolds < 3) {
            Toast.makeText(requireContext(), MESSAGE.WRONG_HOLD_NUMBER, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentModel.setBoulder(null);
        fragmentModel.clearHighlightedHolds();
    }
}
