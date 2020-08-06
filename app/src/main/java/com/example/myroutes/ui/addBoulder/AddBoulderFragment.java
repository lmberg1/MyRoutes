package com.example.myroutes.ui.addBoulder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.R;
import com.example.myroutes.SharedViewModel;
import com.example.myroutes.Wall;
import com.example.myroutes.util.WallDrawingView;
import com.example.myroutes.util.WallLoadingErrorHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.example.myroutes.SharedViewModel.BOULDER_GRADES;

public class AddBoulderFragment extends Fragment {
    private static final String TAG = "AddBoulderFragment";

    // View models
    private SharedViewModel model;
    private AddBoulderModel fragmentModel;

    // Wall data
    private String wall_id;
    private Wall wall;

    // Views
    private WallDrawingView imageView;
    private TextView messageText;

    // Edit boulder mode views
    private TextView title;
    private Button deleteBoulder;
    private Button resetBoulder;

    // Wall Image
    private Bitmap imgBitmap;

    // Keeps track of adding/removing start holds
    private Button addStartHoldButton;
    private Button addFinishHoldButton;
    private boolean addingStartHold = false;
    private boolean addingFinishHold = false;

    // Boulder item that we are editing (if in editing mode)
    private BoulderItem boulderItem;

    private static final class MESSAGE {
        static final String WRONG_HOLD_NUMBER = "Your boulder must have at least 3 holds.";
        static final String TITLE_LENGTH = "Your boulder name must have fewer than 50 characters";
        static final String HELPER_MESSAGE = "Tap on holds to add or remove them from a boulder. \n" +
                "Use the dropdown menu to select start/finish holds.";
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
        imageView = view.findViewById(R.id.imageView2);
        messageText = view.findViewById(R.id.helperMessage);

        // Check if homeFragment passed a boulderItem to edit
        if (fragmentModel.getBoulder() != null) { displayEditView(); }

        // Handle buttons
        Button saveButton = view.findViewById(R.id.saveBoulder);
        Button clearButton = view.findViewById(R.id.clearHolds);
        addStartHoldButton = view.findViewById(R.id.selectStartHolds);
        addFinishHoldButton = view.findViewById(R.id.selectFinishHold);
        ImageButton dropdownButton = view.findViewById(R.id.dropdownButton);
        saveButton.setOnClickListener(this::onSaveBoulderClick);
        clearButton.setOnClickListener(this::onClearHoldsClick);
        addStartHoldButton.setOnClickListener(v -> onSelectStartHoldClick(addStartHoldButton, addFinishHoldButton));
        addFinishHoldButton.setOnClickListener(v -> onSelectFinishHoldClick(addStartHoldButton, addFinishHoldButton));
        dropdownButton.setOnClickListener(v -> toggleDropDown(v, addStartHoldButton, addFinishHoldButton));

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
        fragmentModel.setHighlightedHolds(new ArrayList<>(boulderItem.getBoulder_holds()));
        fragmentModel.setStartHolds(new ArrayList<>(boulderItem.getStart_holds()));
        fragmentModel.setFinishHold(boulderItem.getFinish_hold());
        // Change title of fragment
        title.setText(String.format("Editing \"%s (%s)\"",
                boulderItem.getBoulder_name(), boulderItem.getBoulder_grade()));
        // Show buttons for deleting boulder and resetting boulder holds
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
        int maxHeight = imageView.getMeasuredHeight() - messageText.getMinimumHeight();
        int maxWidth = imageView.getMeasuredWidth();

        imageView.initialize(imgBitmap, wall.getPaths(), wall.getPoints());
        imageView.setSize(maxWidth, maxHeight);

        drawPaths();

        // Set helper message
        messageText.setText(MESSAGE.HELPER_MESSAGE);

        // Allow user to select holds
        imageView.setOnTouchListener(new ImageOnTapListener(this::onDetectTap));
    }

    /*--------------------------------Handle Toolbar Button Clicks--------------------------------*/

    private void onClearHoldsClick(View view) {
        fragmentModel.clearHighlightedHolds();
        drawPaths();
    }

    // Reset highlighted holds (if editing a boulder)
    private void onResetHoldsClick(View view) {
        fragmentModel.setHighlightedHolds(new ArrayList<>(boulderItem.getBoulder_holds()));
        fragmentModel.setStartHolds(new ArrayList<>(boulderItem.getStart_holds()));
        fragmentModel.setFinishHold(boulderItem.getFinish_hold());
        drawPaths();
    }

    private void onSelectStartHoldClick(View startHoldBtn, View finishHoldBtn) {
        addingStartHold = !addingStartHold;
        addingFinishHold = false;
        if (addingStartHold) {
            startHoldBtn.setBackgroundResource(R.drawable.gradebutton_selected);
            finishHoldBtn.setBackgroundResource(0);
        }
        else {
            startHoldBtn.setBackgroundResource(0);
        }
    }

    private void onSelectFinishHoldClick(View startHoldBtn, View finishHoldBtn) {
        addingFinishHold = !addingFinishHold;
        addingStartHold = false;
        if (addingFinishHold) {
            finishHoldBtn.setBackgroundResource(R.drawable.gradebutton_selected);
            startHoldBtn.setBackgroundResource(0);
        }
        else {
            finishHoldBtn.setBackgroundResource(0);
        }
    }

    private void toggleDropDown(View dropdown, View button1, View button2) {
        if (button1.getVisibility() == View.VISIBLE) {
            ((ImageButton) dropdown).setImageResource(R.drawable.ic_baseline_expand_more_24);
            button1.setVisibility(View.GONE);
            button2.setVisibility(View.GONE);
        }
        else {
            ((ImageButton) dropdown).setImageResource(R.drawable.ic_baseline_expand_less_24);
            button1.setVisibility(View.VISIBLE);
            button2.setVisibility(View.VISIBLE);
        }
    }

    /*-----------------------------Handle User Interaction with Holds-----------------------------*/

    // Toggle highlighting of tapped holds
    private void onDetectTap(float touchX, float touchY) {
        // Find the index of the tapped hold
        int tappedIdx = imageView.findTappedHold(touchX, touchY);
        // No hold was tapped
        if (tappedIdx == -1) return;

        // Hold is already highlighted so unhighlight it
        if (fragmentModel.hasHighlighedHold(tappedIdx)) {
            // Check if we are adding/removing start holds
            if (addingStartHold) { toggleStartHold(tappedIdx); }
            // Check if we are adding/removing a finish hold
            else if (addingFinishHold) { toggleFinishHold(tappedIdx); }
            // Otherwise remove highlighted hold
            else {
                fragmentModel.removeHighlightedHold(tappedIdx);
                drawPaths();
            }
        }
        // Hold needs to be highlighted
        else {
            fragmentModel.addHighlightedHold(tappedIdx);
            imageView.highlightHold(tappedIdx);
            // Check if we're adding a start hold
            if (addingStartHold) { toggleStartHold(tappedIdx); }
            // Check if we're adding a finish hold
            if (addingFinishHold) { toggleFinishHold(tappedIdx); }
        }
    }

    private void toggleStartHold(int startHoldIdx) {
        List<Integer> startHolds = fragmentModel.getStartHolds();
        // Remove start hold highlighting if already highlighted
        if (startHolds.contains(startHoldIdx)) {
            fragmentModel.removeStartHold(startHoldIdx);
            drawPaths();
        }
        // Only allow user to select two start holds (so remove first start hold if they select more)
        else if (startHolds.size() == 2) {
            fragmentModel.removeFirstStartHold();
            fragmentModel.addStartHold(startHoldIdx);
            drawPaths();
        }
        // Otherwise just highlight the start hold
        else {
            fragmentModel.addStartHold(startHoldIdx);
            imageView.drawTape(startHoldIdx, false);
        }
        // If two start holds are selected, deselect the button for adding start holds
        if (startHolds.size() == 2) {
            onSelectStartHoldClick(addStartHoldButton, addFinishHoldButton);
        }
    }

    private void toggleFinishHold(int finishHoldIdx) {
        int finishHold = fragmentModel.getFinishHold();
        // Remove finish hold highlighting if already highlighted
        if (finishHold == finishHoldIdx) {
            fragmentModel.clearFinishHold();
            drawPaths();
        }
        // Only allow user to select one finish hold (so remove previous finish hold if necessary)
        else if (fragmentModel.hasFinishHold()) {
            fragmentModel.setFinishHold(finishHoldIdx);
            drawPaths();
        }
        // Otherwise just draw the hold with tape below
        else {
            fragmentModel.setFinishHold(finishHoldIdx);
            imageView.drawTape(finishHoldIdx, true);
        }
        // Deselect the button for adding finish holds
        onSelectFinishHoldClick(addStartHoldButton, addFinishHoldButton);
    }

    // Draw all the holds along with the highlighted holds
    private void drawPaths() {
        imageView.clearPaths();
        imageView.drawAllHolds();
        imageView.highlightBoulder(fragmentModel.getHighlightedHolds(),
                fragmentModel.getStartHolds(), fragmentModel.getFinishHold());
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
                    boulderName, grade, new ArrayList<>(fragmentModel.getHighlightedHolds()));
            if (fragmentModel.hasStartHolds()) { boulderItem.setStart_holds(new ArrayList<>(fragmentModel.getStartHolds())); }
            if (fragmentModel.hasFinishHold()) { boulderItem.setFinish_hold(fragmentModel.getFinishHold()); }

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
        if (fragmentModel.getBoulder() != null) {
            fragmentModel.setBoulder(null);
            fragmentModel.clearHighlightedHolds();
        }
    }
}
