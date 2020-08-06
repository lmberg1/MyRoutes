package com.example.myroutes.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.R;
import com.example.myroutes.SharedViewModel;
import com.example.myroutes.Wall;
import com.example.myroutes.ui.addBoulder.AddBoulderModel;
import com.example.myroutes.util.WallDrawingView;
import com.example.myroutes.util.WallLoadingErrorHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // View models
    private SharedViewModel model;
    private HomeModel fragmentModel;
    private AddBoulderModel addBoulderModel;

    // Wall data
    private Wall wall;
    private Bitmap imgBitmap;
    private List<Path> paths;

    // Drawing variables
    /*private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint canvasPaint;
    private Matrix matrix;
    private float matrixScale;*/

    // Views
    private ImageButton editBoulder;
    private ImageButton dropdownButton;
    private TextView boulderName;
    private LinearLayout toggleButtonLayout;
    private WallDrawingView imageView;

    // Grade button variables
    private List<String> allGrades;
    private List<String> currentGrades;
    private RadioGroup gradeButtonGroup;

    // List variables
    private ExpandableListView listView;
    private ExpandableListAdapter adapter;

    // The boulder items in the current selected grade
    private List<BoulderItem> boulderItems;
    private int nBoulders;

    private static final class MESSAGE {
        static final String NO_BOULDERS = "You don't have any boulders. Add some by going to the \"Add Boulder\" page.";
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        view.findViewById(R.id.editButton).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.syncBoulders).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.dropdownLayout).setVisibility(View.INVISIBLE);
        // Inflate the layout for this fragment
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get models
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(requireActivity()).get(HomeModel.class);
        addBoulderModel = new ViewModelProvider(requireActivity()).get(AddBoulderModel.class);

        // Check for changes in the current wall
        final ProgressBar progressBar = view.findViewById(R.id.progressBar_cyclic);
        final TextView messageText = view.findViewById(R.id.error_message);
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            // Check if there was an error
            boolean success = WallLoadingErrorHandler.handleError(result, progressBar, messageText);
            if (success) {
                this.wall = model.getWall(model.getCurrentWallId());
                this.imgBitmap = wall.getBitmap();
                this.paths = wall.getPaths();

                // Show message if wall has no boulders
                if (wall.getBoulders().size() == 0) {
                    TextView noBouldersText = view.findViewById(R.id.noBouldersMessage);
                    noBouldersText.setText(MESSAGE.NO_BOULDERS);
                    noBouldersText.setVisibility(View.VISIBLE);
                }
                else {
                    view.findViewById(R.id.dropdownLayout).setVisibility(View.VISIBLE);
                }
                setupView(view);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupView(View view) {
        // Set the wall name
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(wall.getName());

        // Show sync button
        ImageButton syncBoulders = view.findViewById(R.id.syncBoulders);
        syncBoulders.setVisibility(View.VISIBLE);

        // Get views
        imageView = view.findViewById(R.id.imageView2);
        editBoulder = view.findViewById(R.id.editButton);
        boulderName = view.findViewById(R.id.boulderName);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        gradeButtonGroup = view.findViewById(R.id.gradeButtonGroup);
        toggleButtonLayout = view.findViewById(R.id.toggleButtonLayout);
        listView = view.findViewById(R.id.expandableListView);

        // Set up the expandable list of boulders
        setupExpandableBoulderList();
        // Set up list of grade buttons available for this wall
        setupGradeButtons();

        // Set up listeners
        syncBoulders.setOnClickListener(this::onClickSyncBoulders);
        editBoulder.setOnClickListener(this::onClickEditBoulder);
        dropdownButton.setOnClickListener(v -> onClickDrowdown());
        boulderName.setOnClickListener(v -> onClickDrowdown());
        imageView.setOnTouchListener(new SwipeListener(this::handleSwipe));

        // Wait until imageView is set up to allow user interaction
        final ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setTransformation();
            }
        });
    }

    private void setTransformation() {
        //if (matrix != null) return;
        // Scale imgBitmap to fit on screen
        int maxHeight = imageView.getMeasuredHeight() - toggleButtonLayout.getMinimumHeight();
        int maxWidth = imageView.getMeasuredWidth();

        imageView.initialize(imgBitmap, wall.getPaths(), wall.getPoints());
        imageView.setSize(maxWidth, maxHeight);

        // TODO: set dynamic list height
        int maxListViewHeight = imageView.getLayoutParams().height + toggleButtonLayout.getHeight();
        listView.getLayoutParams().height = maxListViewHeight;

        // Select first boulder if it exists
        // TODO: show boulder in model
        if (adapter.getGroupCount() > 0) {
            int groupIdx = getNextGradeIndex(allGrades, currentGrades, fragmentModel.getGrade());
            int boulderIdx = (currentGrades.contains(fragmentModel.getGrade())) ? fragmentModel.getBoulderIdx() : 0;
            boulderIdx = Math.min(boulderIdx, adapter.getChildrenCount(groupIdx) - 1);

            BoulderItem first = (BoulderItem) adapter.getChild(groupIdx, boulderIdx);
            onClickGradeButton(first.getBoulder_grade(), boulderIdx);

            // Allow editing of current boulder
            editBoulder.setVisibility(View.VISIBLE);
        }
    }

    private static int getNextGradeIndex(List<String> allGrades, List<String> currentGrades, String lastGrade) {
        if (lastGrade == null) return 0;
        // lastGrade still exists in the list of currentGrades
        if (currentGrades.contains(lastGrade)) return currentGrades.indexOf(lastGrade);
        // There are no grades smaller than lastGrade in the list of currentGrades,
        // so return the first index of currentGrades
        int gradeIdx = allGrades.indexOf(lastGrade);
        if (gradeIdx < allGrades.indexOf(currentGrades.get(0))) return 0;
        // Otherwise find the index of the next smallest grade to lastGrade
        while (!currentGrades.contains(allGrades.get(gradeIdx))) {
            gradeIdx--;
        }
        return currentGrades.indexOf(allGrades.get(gradeIdx));
    }

    /*---------------------------------Handle App Bar Buttons-------------------------------------*/

    private void onClickSyncBoulders(View v) {
        model.syncBoulders(wall.getId()).observe(getViewLifecycleOwner(), result -> {
            if (result == null) { return; }
            if (result == SharedViewModel.Status.SUCCESS) {
                // Update expandable list view
                HashMap<String, List<BoulderItem>> boulders = wall.getBoulders();
                List<String> grades = getSortedWallGrades(boulders.keySet());
                adapter.setItems(grades, boulders);
                // Update grade button list
                setupGradeButtons();
                // Reset the current boulder view
                onClickGradeButton(fragmentModel.getGrade(), fragmentModel.getBoulderIdx());
            }
        });
    }

    private void onClickEditBoulder(View v) {
        BoulderItem boulderItem = boulderItems.get(fragmentModel.getBoulderIdx());
        addBoulderModel.setBoulder(new BoulderItem(boulderItem));
        goToAddBoulderFragment(v);
    }

    private void goToAddBoulderFragment(View v) {
        NavHostFragment.findNavController(HomeFragment.this)
                .navigate(R.id.nav_addBoulder);
    }

    /*----------------------------Handle Expandable Boulder List----------------------------------*/

    private void setupExpandableBoulderList() {
        // Initialize expandable list view data
        HashMap<String, List<BoulderItem>> boulders = wall.getBoulders();
        List<String> grades = getSortedWallGrades(boulders.keySet());

        // Setup expandable list view
        adapter = new ExpandableListAdapter(requireContext(), grades, boulders);
        listView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Show the boulder that was clicked
            int gradeButtonIdx = allGrades.indexOf((String) adapter.getGroup(groupPosition));
            RadioButton b = (RadioButton) gradeButtonGroup.getChildAt(gradeButtonIdx);
            onClickGradeButton(b.getText().toString(), childPosition);
            return true;
        });
        // Expand all of the groups
        int nGroups = adapter.getGroupCount();
        for (int i = 0; i < nGroups; i++) {
            listView.expandGroup(i);
        }
    }

    // Toggle expandable list view dropdown
    private void onClickDrowdown() {
        if (listView.getVisibility() == View.GONE) {
            listView.setVisibility(View.VISIBLE);
            dropdownButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
        }
        else {
            listView.setVisibility(View.GONE);
            dropdownButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
        }
    }

    /*----------------------------Handle Boulder Navigation Buttons-------------------------------*/

    private void setupGradeButtons() {
        // Figure out which grades this wall has and only show buttons for those grades
        allGrades = Arrays.asList(SharedViewModel.BOULDER_GRADES);
        currentGrades = getSortedWallGrades(wall.getBoulders().keySet());
        for (String s : currentGrades) {
            int buttonIdx = allGrades.indexOf(s);
            gradeButtonGroup.getChildAt(buttonIdx).setVisibility(View.VISIBLE);
        }

        // Called when new grade button is clicked
        gradeButtonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton button = group.findViewById(checkedId);
            String grade = button.getText().toString();
            // Show the first boulder of that grade
            if (!grade.equals(fragmentModel.getGrade())) {
                onClickGradeButton(button.getText().toString(), 0);
            }
        });
    }

    private void onClickGradeButton(String currentGrade, int boulderIdx) {
        // Make sure current grade button is selected
        fragmentModel.setGrade(currentGrade);
        ((RadioButton) gradeButtonGroup.getChildAt(allGrades.indexOf(currentGrade))).setChecked(true);

        // Remove radioButtons from previous click
        toggleButtonLayout.removeAllViews();

        // Clear previous drawn paths
        imageView.clearPaths();

        // Get boulders of the selected grade
        boulderItems = wall.getBoulders().get(currentGrade);
        if (boulderItems == null) { return; }
        nBoulders = boulderItems.size();

        // Create the toggle buttons
        setupToggleButtons(nBoulders);

        // Draw the selected boulder
        setBoulderIdx(boulderIdx);
    }

    // Create toggle radio buttons to show the index of the current boulder
    private void setupToggleButtons(int n) {
        // Dynamically create toggle buttons
        for (int j = 0; j < n; j++) {
            RadioButton toggleButton = new RadioButton(getContext());
            toggleButton.setId(j);
            toggleButton.setOnClickListener(v -> setBoulderIdx(v.getId()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            toggleButtonLayout.addView(toggleButton, lp);
        }
    }

    private void toggleRadioButton(View view) {
       // Deselect previous toggle button
        int prevIdx = fragmentModel.getBoulderIdx();
        if (prevIdx < nBoulders) {
            RadioButton previous = toggleButtonLayout.findViewById(prevIdx);
            previous.setClickable(true);
            previous.setChecked(false);
        }

        // Select current button
        RadioButton button = (RadioButton) view;
        button.setClickable(false);
        button.setChecked(true);
    }

    /*--------------------------------Handle Displaying Boulder-----------------------------------*/

    private void setBoulderIdx(int boulderIdx) {
        // Clear previous drawn paths
        imageView.clearPaths();

        // Select the current toggle button
        toggleRadioButton(toggleButtonLayout.findViewById(boulderIdx));

        // Update view model
        fragmentModel.setBoulderIdx(boulderIdx);

        // Update display to show this boulder
        BoulderItem boulderItem = boulderItems.get(boulderIdx);
        String name = (boulderItem.getBoulder_name().isEmpty()) ? "Unnamed" : boulderItem.getBoulder_name();
        boulderName.setText(String.format("%s - %s", fragmentModel.getGrade(), name));
        imageView.drawBoulder(boulderItem);
    }

    /*-----------------------------------Helper Functions-----------------------------------------*/

    private static List<Path> indicesToPaths(List<Integer> indices, ArrayList<Path> allPaths) {
        List<Path> paths = new ArrayList<>();
        for (int holdIdx : indices) {
            paths.add(allPaths.get(holdIdx));
        }
        return paths;
    }

    private static List<String> getSortedWallGrades(Set<String> currentGradesSet) {
        // Get all of the grades that the current wall has in sorted order
        List<String> currentGrades = new ArrayList<>();
        for (String s : SharedViewModel.BOULDER_GRADES) {
            if (currentGradesSet.contains(s)) { currentGrades.add(s); }
        }
        return currentGrades;
    }

    private void handleSwipe(SwipeListener.SWIPE swipe) {
        // Ignore if no boulders
        if (boulderItems == null) return;
        if (boulderItems.isEmpty()) return;
        // Go to previous or next boulder
        switch(swipe) {
            case LEFT:
                goToNextBoulder();
                break;
            case RIGHT:
                goToPrevBoulder();
        }
    }

    private void goToNextBoulder() {
        int boulderIdx = fragmentModel.getBoulderIdx();
        int idx = (boulderIdx == 0) ? nBoulders - 1 : boulderIdx - 1;
        setBoulderIdx(idx);
    }

    private void goToPrevBoulder() {
        int boulderIdx = fragmentModel.getBoulderIdx();
        int idx = (boulderIdx + 1) % nBoulders;
        setBoulderIdx(idx);
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
    }
}
