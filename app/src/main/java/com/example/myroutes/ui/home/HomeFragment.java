package com.example.myroutes.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import com.example.myroutes.WorkoutFragment;
import com.example.myroutes.db.mongoClasses.BoulderItem;
import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.Wall;
import com.example.myroutes.ui.addBoulder.AddBoulderModel;
import com.example.myroutes.util.WallDrawingHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
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
    private ArrayList<Path> paths;

    // Drawing variables
    private Bitmap drawingBitmap;
    private Canvas canvas;
    private Paint drawPaint;
    private Paint canvasPaint;
    private Matrix matrix;

    // Views
    private ImageButton editBoulder;
    private ImageButton dropdownButton;
    private TextView boulderName;
    private LinearLayout toggleButtonLayout;
    private ImageView imageView;

    // Grade button variables
    private List<String> allGrades;
    private RadioGroup gradeButtonGroup;

    // List variables
    private ExpandableListView listView;
    private ExpandableListAdapter adapter;

    // The boulder items in the current selected grade
    private List<BoulderItem> boulderItems;
    private int nBoulders;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        view.findViewById(R.id.dropdownLayout).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.floatingActionButton).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.editButton).setVisibility(View.INVISIBLE);
        // Inflate the layout for this fragment
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get models
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(this).get(HomeModel.class);
        addBoulderModel = new ViewModelProvider(requireActivity()).get(AddBoulderModel.class);

        // Check for changes in the current wall
        final ProgressBar progressBar = view.findViewById(R.id.progressBar_cyclic);
        final TextView messageText = view.findViewById(R.id.error_message);
        messageText.setVisibility(View.VISIBLE);
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                progressBar.setVisibility(View.GONE);
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
            else {
                messageText.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                view.findViewById(R.id.dropdownLayout).setVisibility(View.VISIBLE);
                view.findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
                this.wall = model.getWall(model.getCurrentWallId());
                this.imgBitmap = wall.getBitmap();
                if (adapter != null) {
                    adapter.setItems(wall.getBoulders());
                }
                setupView(view);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupView(View view) {
        // Get hold paths of the current wall
        paths = wall.getPaths();

        // Handle navigation
        FloatingActionButton goToAddBoulder = view.findViewById(R.id.floatingActionButton);
        goToAddBoulder.setOnClickListener(this::goToAddBoulderFragment);

        // Get views
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(wall.getName());
        toggleButtonLayout = view.findViewById(R.id.toggleButtonLayout);
        imageView = view.findViewById(R.id.imageView2);
        editBoulder = view.findViewById(R.id.editButton);
        boulderName = view.findViewById(R.id.boulderName);
        dropdownButton = view.findViewById(R.id.dropdownButton);

        // Set up listeners
        editBoulder.setOnClickListener(this::onClickEditBoulder);
        dropdownButton.setOnClickListener(v -> onClickDrowdown());
        boulderName.setOnClickListener(v -> onClickDrowdown());
        imageView.setOnTouchListener(new SwipeListener());

        // Figure out which grades this wall has and only show buttons for those grades
        gradeButtonGroup = view.findViewById(R.id.gradeButtonGroup);
        allGrades = Arrays.asList(SharedViewModel.BOULDER_GRADES);
        Set<String> currentGradesSet = wall.getBoulders().keySet();
        List<String> currentGrades = new ArrayList<>();
        for (String s : allGrades) {
            if (currentGradesSet.contains(s)) { currentGrades.add(s); }
            int buttonIdx = allGrades.indexOf(s);
            gradeButtonGroup.getChildAt(buttonIdx).setVisibility(
                    (currentGradesSet.contains(s)) ? View.VISIBLE : View.GONE);
        }

        // Called when new grade button is clicked
        gradeButtonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton button = (RadioButton) group.findViewById(checkedId);
            String grade = button.getText().toString();
            // Show the first boulder of that grade
            if (!grade.equals(fragmentModel.getGrade())) {
                onClickGradeButton(button.getText().toString(), 0);
            }
        });

        if (wall.getBoulders().size() == 0) {
            TextView messageText = view.findViewById(R.id.noBouldersMessage);
            String message = "You don't have any boulders! Add some by clicking the plus button below.";
            messageText.setText(message);
            messageText.setVisibility(View.VISIBLE);
        }

        // Setup expandable list view
        adapter = new ExpandableListAdapter(requireContext(), currentGrades, wall.getBoulders());
        listView = view.findViewById(R.id.expandableListView);
        listView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        listView.setOnTouchListener(new VerticalSwipeListener());
        listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Update the view
            RadioButton b = (RadioButton) gradeButtonGroup.getChildAt(groupPosition);
            onClickGradeButton(b.getText().toString(), childPosition);
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

    private void onClickEditBoulder(View v) {
        addBoulderModel.setBoulder(boulderItems.get(fragmentModel.getBoulderIdx()));
        goToAddBoulderFragment(v);
    }

    private void goToAddBoulderFragment(View v) {
        NavHostFragment.findNavController(HomeFragment.this)
                .navigate(R.id.nav_addBoulder);
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
        if (adapter.getGroupCount() > 0) {
            BoulderItem first = (BoulderItem) adapter.getChild(0, 0);
            onClickGradeButton(first.getBoulder_grade(), 0);

            // Allow editing of current boulder
            editBoulder.setVisibility(View.VISIBLE);
        }
    }

    private void onClickGradeButton(String currentGrade, int boulderIdx) {
        // Make sure current grade button is selected
        fragmentModel.setGrade(currentGrade);
        ((RadioButton) gradeButtonGroup.getChildAt(allGrades.indexOf(currentGrade))).setChecked(true);

        // Remove radioButtons from previous click
        toggleButtonLayout.removeAllViews();

        // Clear previous drawn paths
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        imageView.setImageBitmap(drawingBitmap);

        // Get boulders of the selected grade
        boulderItems = wall.getBoulders().get(currentGrade);
        nBoulders = boulderItems.size();

        // Create toggle radio buttons to show the index of the current boulder
        for (int j = 0; j < nBoulders; j++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setId(j);
            radioButton.setOnClickListener(v -> setBoulderIdx(v.getId()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            toggleButtonLayout.addView(radioButton, lp);
        }

        // Draw the selected boulder
        setBoulderIdx(boulderIdx);
    }

    private void toggleRadioButton(View view) {
        // Unselect all other buttons
        for (int i = 0; i < nBoulders; i++) {
            RadioButton previous = toggleButtonLayout.findViewById(i);
            previous.setClickable(true);
            previous.setChecked(false);
        }

        // Select current button
        RadioButton button = (RadioButton) view;
        button.setClickable(false);
        button.setChecked(true);
    }

    private void setBoulderIdx(int boulderIdx) {
        // Clear previous drawn paths
        canvas.drawBitmap(imgBitmap, 0, 0, canvasPaint);
        imageView.setImageBitmap(drawingBitmap);

        // Select the current toggle button
        toggleRadioButton(toggleButtonLayout.findViewById(boulderIdx));

        // Update view model
        fragmentModel.setBoulderIdx(boulderIdx);

        // Get the paths of the holds associated with this route
        BoulderItem boulderItem = boulderItems.get(boulderIdx);
        ArrayList<Integer> holdIndices = boulderItem.getBoulder_holds();
        ArrayList<Path> route = new ArrayList<>();
        for (int holdIdx : holdIndices) {
            route.add(paths.get(holdIdx));
        }

        // Get name of this route
        String name = (boulderItem.getBoulder_name().isEmpty()) ? "Unnamed" : boulderItem.getBoulder_name();
        boulderName.setText(String.format("%s - %s", fragmentModel.getGrade(), name));

        // Draw the route
        drawPaths(route);
    }

    private void drawPaths(ArrayList<Path> paths) {
        for (Path p : paths) {
            Path transform = new Path(p);
            transform.transform(matrix);
            transform.close();
            canvas.drawPath(transform, drawPaint);
        }
        imageView.setImageBitmap(drawingBitmap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
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
            if (boulderItems == null) return true;
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
                        int boulderIdx = fragmentModel.getBoulderIdx();

                        // Left to Right swipe action (prev boulder)
                        if (x2 > x1) {
                            int idx = (boulderIdx == 0) ? nBoulders - 1 : boulderIdx - 1;
                            setBoulderIdx(idx);
                        }

                        // Right to left swipe action (next boulder)
                        else {
                            int idx = (boulderIdx + 1) % nBoulders;
                            setBoulderIdx(idx);
                        }

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
