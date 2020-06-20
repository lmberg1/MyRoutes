package com.example.myroutes;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.Wall;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WorkoutItem;
import com.example.myroutes.util.WallDrawingHelper;

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
    private ArrayList<Path> paths;

    // Drawing variables
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint canvasPaint;
    private Matrix matrix;

    // Views
    private LinearLayout dropdownLayout;
    private ImageButton dropdownButton;
    private TextView boulderName;
    private TextView instruction;
    private ImageView imageView;
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
    private List<Integer> groupIndices;
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
        else {
            // TODO: make more efficient
            // Get boulderItems from the boulder_ids in the workoutItem
            boulderSets = new ArrayList<>();
            groupIndices = new ArrayList<>();
            List<List<String>> boulderSetList = workoutItem.getWorkoutSets();
            int nSets = boulderSetList.size();
            for (int i = 0; i < nSets; i++) {
                List<String> boulderIds = boulderSetList.get(i);
                List<BoulderItem> boulderItems = new ArrayList<>();
                for (String boulder_id : boulderIds) {
                    BoulderItem b = wall.searchBoulderId(boulder_id);
                    if (b != null) {
                        groupIndices.add(i);
                        boulderItems.add(b);
                    }
                }
                if (!boulderItems.isEmpty()) { boulderSets.add(boulderItems); }
            }
            // Setup the view
            nBoulders = groupIndices.size();
            setupView(view);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupView(View view) {
        // Get hold paths of the current wall
        paths = wall.getPaths();
        imgBitmap = wall.getBitmap();

        // Handle back navigation
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(this::goToWorkoutFragment);

        // Get views
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(workoutItem.getWorkout_name());
        imageView = view.findViewById(R.id.imageView2);
        boulderName = view.findViewById(R.id.boulderName);
        instruction = view.findViewById(R.id.instruction);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        dropdownLayout = view.findViewById(R.id.dropdownLayout);
        nextButton = view.findViewById(R.id.nextButton);
        continueButton = view.findViewById(R.id.continueButton);

        // Set up recycler view
        recyclerView = view.findViewById(R.id.recyclerView);

        // Set up listeners
        dropdownButton.setOnClickListener(v -> onClickDrowdown());
        boulderName.setOnClickListener(v -> onClickDrowdown());

        // Setup expandable list view
        adapter = new StartWorkoutExpandableListAdapter(requireContext(), boulderSets);
        listView = view.findViewById(R.id.expandableListView);
        listView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        listView.setOnTouchListener(new VerticalSwipeListener());
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                showBoulderIdx(groupPosition, childPosition);
                return true;
            }
        });

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

    // Toggle expandable list view dropdown
    private void onClickDrowdown() {
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

    private void setTransformation() {
        progressAdapter = new WorkoutProgressAdapter(boulderSets,
                nBoulders, recyclerView.getWidth());
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(progressAdapter);

        // Scale imgBitmap to fit on screen
        int maxHeight = imageView.getMeasuredHeight();
        int maxWidth = imageView.getMeasuredWidth();
        matrix = WallDrawingHelper.getScalingMatrix(imgBitmap, maxHeight, maxWidth);
        imgBitmap = WallDrawingHelper.resizeBitmap(imgBitmap, matrix);

        // Update layout params to match image
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = imgBitmap.getWidth();
        params.height = imgBitmap.getHeight();

        // Set drawing variables
        drawingBitmap = imgBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(drawingBitmap);
        drawPaint = WallDrawingHelper.getDrawPaint();
        canvasPaint = WallDrawingHelper.getCanvasPaint();
        imageView.setImageBitmap(drawingBitmap);

        continueButton.setOnClickListener(v -> { onStartWorkout(); });
    }

    /*----------------------------Handle Display of Workout Progress------------------------------*/

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
        fragmentModel.setSetIdx(setIdx);

        // Workout is done
        if (setIdx == boulderSets.size()) { onFinishWorkout(); }
        // Workout set is done
        if (setIdx > 0) {
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
        instruction.setText(String.format(Locale.US,"Set %d Complete", setIdx));
        // Hide the dropdown views
        boulderName.setText("");
        dropdownButton.setVisibility(View.INVISIBLE);
        // Replace next button with continue button
        nextButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.VISIBLE);
    }

    private void onContinueSet(int setIdx, int boulderIdx) {
        instruction.setText(String.format(Locale.US,"%s", "Climb: "));
        // Show the dropdown views
        setBoulderIdx(setIdx, boulderIdx);
        dropdownButton.setVisibility(View.VISIBLE);
        // Replace continue button with next button
        continueButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
    }

    private void onFinishWorkout() {
        instruction.setText(String.format(Locale.US,"%s", "Workout Complete!"));
        // Hide the dropdown views
        boulderName.setText("");
        dropdownButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
    }

    /*--------------------------------Handle Displaying Boulders----------------------------------*/

    private void setBoulderIdx(int setIdx, int boulderIdx) {
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
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        imageView.setImageBitmap(drawingBitmap);

        // Get the boulderItem
        BoulderItem boulderItem = boulderSets.get(setIdx).get(boulderIdx);

        // Get the paths of the holds associated with this route
        ArrayList<Integer> holdIndices = boulderItem.getBoulder_holds();
        ArrayList<Path> route = new ArrayList<>();
        for (int holdIdx : holdIndices) {
            route.add(paths.get(holdIdx));
        }

        // Draw the route
        drawPaths(route);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
    }

    /*-----------------------------------------Helpers--------------------------------------------*/

    private void drawPaths(ArrayList<Path> paths) {
        for (Path p : paths) {
            Path transform = new Path(p);
            transform.transform(matrix);
            transform.close();
            canvas.drawPath(transform, drawPaint);
        }
        imageView.setImageBitmap(drawingBitmap);
    }

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
        adapter.updateCurrentBoulder(setIdx, boulderIdx);
    }

    private void goToPreviousBoulder() {
        int setIdx = fragmentModel.getSetIdx();
        int boulderIdx = fragmentModel.getBoulderIdx();

        // Check if set index will change
        if (boulderIdx == 0) {
            int groupIdx = setIdx - 1;
            if (groupIdx < 0) return;
            int childIdx = adapter.getChildrenCount(groupIdx) - 1;
            setSetIdx(groupIdx, childIdx);
        }
        else {
            setBoulderIdx(setIdx, boulderIdx - 1);
        }
    }

    /*-----------------------------------------Listeners------------------------------------------*/

    // Detects a vertical swipe to close expandable list view
    private class VerticalSwipeListener implements View.OnTouchListener {
        private float y1,y2;
        static final int MIN_DISTANCE = 150;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Look for swipes of size at least MIN_DISTANCE
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    y1 = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    y2 = event.getY();
                    float deltaY = y1 - y2;
                    if (deltaY > MIN_DISTANCE) {
                        onClickDrowdown();
                        return true;
                    }
                    break;
            }
            return false;
        }
    }
}
