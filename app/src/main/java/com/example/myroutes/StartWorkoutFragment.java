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
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.Wall;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.db.mongoClasses.WorkoutItem;
import com.example.myroutes.ui.home.ExpandableListAdapter;
import com.example.myroutes.ui.home.HomeModel;
import com.example.myroutes.util.WallDrawingHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private ImageButton dropdownButton;
    private TextView boulderName;
    private LinearLayout toggleButtonLayout;
    private ImageView imageView;
    private CheckBox checkBox;

    // List variables
    private ExpandableListView listView;
    private StartWorkoutExpandableListAdapter adapter;

    // Workout data
    private WorkoutItem workoutItem;
    private List<Integer> groupIndices;
    private List<List<BoulderItem>> boulderSets;
    private int nBoulders;

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
        Log.e(TAG, fragmentModel.getWall_id());
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
                    BoulderItem b = wall.searchBoulder(boulder_id);
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
        toggleButtonLayout = view.findViewById(R.id.toggleButtonLayout);
        imageView = view.findViewById(R.id.imageView2);
        boulderName = view.findViewById(R.id.boulderName);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        checkBox = view.findViewById(R.id.checkBox);

        // Set up listeners
        dropdownButton.setOnClickListener(v -> onClickDrowdown());
        boulderName.setOnClickListener(v -> onClickDrowdown());
        imageView.setOnTouchListener(new SwipeListener());

        // Setup expandable list view
        adapter = new StartWorkoutExpandableListAdapter(requireContext(), boulderSets);
        listView = view.findViewById(R.id.expandableListView);
        listView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        listView.setOnTouchListener(new VerticalSwipeListener());
        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            if (fragmentModel.getSetIdx() != groupPosition) {
                setSetIdx(fragmentModel.getSetIdx(), fragmentModel.getBoulderIdx());
            }
            else {
                setBoulderIdx(groupPosition, childPosition);
            }
            return true;
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
        }
    }

    private void setTransformation() {
        // Scale imgBitmap to fit on screen
        int maxHeight = imageView.getMeasuredHeight() - toggleButtonLayout.getMinimumHeight();
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

        // Select first boulder if it exists
        setSetIdx(fragmentModel.getSetIdx(), fragmentModel.getBoulderIdx());

        // Watch checkbox
        checkBox.setOnClickListener(v -> {
            goToNextBoulder();
            checkBox.setChecked(false);
        });
    }

    // Create toggle radio buttons to show the index of the current boulder
    private void createToggleButtons(int setIdx, int nButtons) {
        for (int j = 0; j < nButtons; j++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setId(j);
            radioButton.setOnClickListener(v -> setBoulderIdx(setIdx, v.getId()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            toggleButtonLayout.addView(radioButton, lp);
        }
    }

    private void toggleRadioButton(View view) {
        // Unselect all other buttons
        int nButtons = toggleButtonLayout.getChildCount();
        for (int i = 0; i < nButtons; i++) {
            RadioButton previous = toggleButtonLayout.findViewById(i);
            previous.setClickable(true);
            previous.setChecked(false);
        }

        // Select current button
        RadioButton button = (RadioButton) view;
        button.setClickable(false);
        button.setChecked(true);
    }

    private void setSetIdx(int setIdx, int boulderIdx) {
        fragmentModel.setSetIdx(setIdx);

        toggleButtonLayout.removeAllViews();
        createToggleButtons(setIdx, adapter.getChildrenCount(setIdx));
        setBoulderIdx(setIdx, boulderIdx);
    }

    private void setBoulderIdx(int setIdx, int boulderIdx) {
        // Clear previous drawn paths
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        imageView.setImageBitmap(drawingBitmap);

        // Get the boulderItem
        BoulderItem boulderItem = boulderSets.get(groupIndices.get(boulderIdx)).get(boulderIdx);

        // Select the current toggle button
        toggleRadioButton(toggleButtonLayout.findViewById(boulderIdx));

        // Update view model
        fragmentModel.setBoulderIdx(boulderIdx);

        // Get the paths of the holds associated with this route
        ArrayList<Integer> holdIndices = boulderItem.getBoulder_holds();
        ArrayList<Path> route = new ArrayList<>();
        for (int holdIdx : holdIndices) {
            route.add(paths.get(holdIdx));
        }

        // Get name of this route
        String name = (boulderItem.getBoulder_name().isEmpty()) ? "Unnamed" : boulderItem.getBoulder_name();
        boulderName.setText(String.format(Locale.US, "Set %d - %s", setIdx + 1, name));

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

        // Check if set index will change
        if (boulderIdx == adapter.getChildrenCount(setIdx) - 1) {
            int groupIdx = setIdx + 1;
            if (groupIdx >= adapter.getGroupCount()) return;
            setSetIdx(groupIdx, 0);
        }
        else {
            setBoulderIdx(setIdx, boulderIdx + 1);
        }
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

    // Detects horizontal swipes to switch between boulders of the same grade
    private class SwipeListener implements View.OnTouchListener {
        private float x1,x2;
        static final int MIN_DISTANCE = 150;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Ignore touch if there aren't any boulders
            if (boulderSets == null) return true;
            if (nBoulders == 0) return true;

            // Look for swipes of size at least MIN_DISTANCE
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    float deltaX = x2 - x1;

                    if (Math.abs(deltaX) > MIN_DISTANCE) {
                        // Left to Right swipe action (prev boulder)
                        if (x2 > x1) { goToPreviousBoulder(); }

                        // Right to left swipe action (next boulder)
                        else { goToNextBoulder();  }

                    }
                    break;
            }
            return true;
        }
    }

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
