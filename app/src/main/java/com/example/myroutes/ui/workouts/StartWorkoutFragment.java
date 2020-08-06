package com.example.myroutes.ui.workouts;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myroutes.R;
import com.example.myroutes.SharedViewModel;
import com.example.myroutes.Wall;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.example.myroutes.util.WallDrawingHelper;
import com.example.myroutes.util.WallDrawingView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StartWorkoutFragment extends Fragment {
    private static final String TAG = "StartWorkoutFragment";

    // View models
    private SharedViewModel model;
    private WorkoutModel fragmentModel;

    // Wall data
    private Wall wall;
    private Bitmap imgBitmap;
    //private List<Path> paths;

    // Views
    private LinearLayout progressView;
    private LinearLayout dropdownLayout;
    private ImageButton dropdownButton;
    private TextView boulderName;
    private WallDrawingView imageView;
    private Button nextButton;
    private Button continueButton;

    // Progress list variables
    private WorkoutProgressAdapter progressAdapter;
    private RecyclerView recyclerView;

    // List variables
    private ExpandableListView listView;
    private StartWorkoutExpandableListAdapter adapter;

    // Workout data
    private WorkoutItem workoutItem;
    private List<List<BoulderItem>> boulderSets;
    private int nBoulders;
    private int currentShowingSetIdx;
    private int currentShowingBoulderIdx;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_workout, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get models
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(requireActivity()).get(WorkoutModel.class);

        // Get the wall and workout
        this.wall = model.getWall(fragmentModel.getWall_id());
        String workout_id = fragmentModel.getWorkout_id();
        this.workoutItem = wall.searchWorkout(workout_id);

        // Something went wrong
        if (this.workoutItem == null) {
            return;
        }
        // Initialize wall variables
        this.boulderSets = wall.workoutToBoulders(workoutItem);
        this.imgBitmap = wall.getBitmap();
        this.nBoulders = workoutItem.getBoulderCount();
        // Set up the view
        setupView(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupView(View view) {
        // Set title
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(workoutItem.getWorkout_name());

        // Handle back navigation
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(this::goToWorkoutFragment);

        // Get views
        progressView = view.findViewById(R.id.progressView);
        imageView = view.findViewById(R.id.imageView2);
        boulderName = view.findViewById(R.id.boulderName);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        dropdownLayout = view.findViewById(R.id.dropdownLayout);
        nextButton = view.findViewById(R.id.nextButton);
        continueButton = view.findViewById(R.id.continueButton);
        listView = view.findViewById(R.id.expandableListView);
        recyclerView = view.findViewById(R.id.recyclerView);

        // Initialize views
        boulderName.setText(String.format(Locale.US, "%d Total Sets", boulderSets.size()));
        setupExpandableBoulderList();
        setupWorkoutProgressView();

        // Set up listeners
        dropdownButton.setOnClickListener(v -> onClickDropdown());
        boulderName.setOnClickListener(v -> onClickDropdown());

        // Wait until imageView is setup to allow user interaction
        final ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setTransformation();
            }
        });
    }

    private void goToWorkoutFragment(View v) {
        NavHostFragment.findNavController(StartWorkoutFragment.this)
                .navigate(R.id.nav_workouts);
    }

    private void setTransformation() {
        setupWorkoutProgressView();

        // Scale imgBitmap to fit on screen
        int maxHeight = imageView.getMeasuredHeight() - progressView.getMinimumHeight();
        int maxWidth = imageView.getMeasuredWidth();

        // Initialize paths
        List<Path> allPaths = wall.getPaths();
        List<Path> holdPaths = new ArrayList<>();
        for (Path p : allPaths) {
            Path copy = new Path(p);
            holdPaths.add(copy);
        }

        imageView.initialize(imgBitmap, holdPaths, wall.getPoints());
        imageView.setSize(maxWidth, maxHeight);

        listView.getLayoutParams().height = imgBitmap.getHeight() + progressView.getMinimumHeight();

        continueButton.setOnClickListener(v -> { onStartWorkout(); });
    }

    /*-------------------------------Handle Expandable Set List-----------------------------------*/

    // Setup expandable list view
    private void setupExpandableBoulderList() {
        adapter = new StartWorkoutExpandableListAdapter(requireContext(), boulderSets);
        listView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Show the boulder that was clicked
            showBoulderIdx(groupPosition, childPosition);
            return true;
        });
        // Expand all the group
        int nGroups = adapter.getGroupCount();
        for (int i = 0; i < nGroups; i++) {
            listView.expandGroup(i);
        }
    }

    // Toggle expandable list view dropdown
    private void onClickDropdown() {
        if (listView.getVisibility() == View.GONE) {
            listView.setVisibility(View.VISIBLE);
            dropdownButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
        }
        else {
            listView.setVisibility(View.GONE);
            dropdownButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
            // If we are showing a different boulder, go back to current place in workout
            int boulderIdx = fragmentModel.getBoulderIdx();
            int setIdx = fragmentModel.getSetIdx();
            if ((currentShowingSetIdx != setIdx) || (currentShowingBoulderIdx != boulderIdx)) {
                showBoulderIdx(setIdx, boulderIdx);
            }
        }
    }

    /*----------------------------Handle Display of Workout Progress------------------------------*/

    private void setupWorkoutProgressView() {
        // Set up recycler view
        progressAdapter = new WorkoutProgressAdapter(boulderSets,
                nBoulders, recyclerView.getWidth());
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(progressAdapter);
    }

    private void onStartWorkout() {
        // Select first boulder if it exists
        setSetIdx(fragmentModel.getSetIdx(), fragmentModel.getBoulderIdx());

        // Show the dropdown layout
        dropdownLayout.setVisibility(View.VISIBLE);

        // Replace start button with next button
        continueButton.setVisibility(View.GONE);
        continueButton.setText(String.format("%s", "Continue"));
        nextButton.setVisibility(View.VISIBLE);

        // Watch next button
        nextButton.setOnClickListener(v -> {
            goToNextBoulder();
        });
    }

    private void setSetIdx(int setIdx, int boulderIdx) {
        // Workout is done
        if (setIdx == boulderSets.size()) {
            onFinishWorkout();
            return;
        }
        // Workout set is done
        else if (setIdx > 0) {
            fragmentModel.setSetIdx(setIdx);
            // Update view to tell user set if finished
            onFinishSet(setIdx);
            // Wait for user to click continue to move onto next set
            continueButton.setOnClickListener(v -> onContinueSet(setIdx, boulderIdx));
            return;
        }
        // Go to next boulder
        setBoulderIdx(setIdx, boulderIdx);
    }

    private void onFinishSet(int setIdx) {
        boulderName.setText(String.format(Locale.US,"Set %d Complete", setIdx));
        // Replace next button with continue button
        nextButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.VISIBLE);
    }

    private void onContinueSet(int setIdx, int boulderIdx) {
        boulderName.setText(String.format(Locale.US,"%s", "Climb: "));
        // Show the dropdown views
        setBoulderIdx(setIdx, boulderIdx);
        // Replace continue button with next button
        continueButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
    }

    private void onFinishWorkout() {
        boulderName.setText(String.format(Locale.US,"%s", "Workout Complete!"));
        // Hide the dropdown views
        nextButton.setVisibility(View.GONE);
    }

    /*--------------------------------Handle Displaying Boulders----------------------------------*/

    private void setBoulderIdx(int setIdx, int boulderIdx) {
        // Darken the text color of the current set
        if ((boulderIdx == 0) && (setIdx < recyclerView.getChildCount())) {
            TextView setProgressText = recyclerView.getChildAt(setIdx).findViewById(R.id.setTitle);
            setProgressText.setTextColor(0xFF444444);
        }

        // Get the boulderItem
        BoulderItem boulderItem = boulderSets.get(setIdx).get(boulderIdx);

        // Update view model
        fragmentModel.setBoulderIdx(boulderIdx);

        // Draw the boulder
        showBoulderIdx(setIdx, boulderIdx);

        // Get name of this route
        String name = (boulderItem.getBoulder_name().isEmpty()) ? "Unnamed" : boulderItem.getBoulder_name();
        boulderName.setText(String.format(Locale.US, "%s (%s)", name, boulderItem.getBoulder_grade()));
    }

    private void showBoulderIdx(int setIdx, int boulderIdx) {
        // Update variables
        currentShowingSetIdx = setIdx;
        currentShowingBoulderIdx = boulderIdx;

        // Clear previous drawn paths
        imageView.clearPaths();

        // Draw the boulderItem
        BoulderItem boulderItem = boulderSets.get(setIdx).get(boulderIdx);
        imageView.drawBoulder(boulderItem);
    }

    /*-----------------------------------------Helpers--------------------------------------------*/

    private void goToNextBoulder() {
        int setIdx = fragmentModel.getSetIdx();
        int boulderIdx = fragmentModel.getBoulderIdx();

        // Update workout progress
        WorkoutProgressAdapter.MyViewHolder child = (WorkoutProgressAdapter.MyViewHolder)
                recyclerView.getChildViewHolder(recyclerView.getChildAt(setIdx));
        child.progressBar.incrementProgressBy(1);
        child.completeBoulders.setText(String.format(Locale.US, "%d", boulderIdx + 1));

        // Check if set index will change
        if (boulderIdx == adapter.getChildrenCount(setIdx) - 1) {
            setSetIdx(setIdx += 1, boulderIdx = 0);
        }
        else {
            setBoulderIdx(setIdx, boulderIdx += 1);
        }

        // Update expandable list view
        if (setIdx < recyclerView.getChildCount()) {
            adapter.updateCurrentBoulder(setIdx, boulderIdx);
        }
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
    }
}
