package com.example.myroutes.ui.workouts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.entities.Wall;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.util.WallLoadingErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WorkoutFragment extends Fragment {
    private static final String TAG = "ManageWallsFragment";

    // View models
    private SharedViewModel model;
    private WorkoutModel fragmentModel;

    // Scrollable view
    NestedScrollView scrollView;

    // Workout list variables
    private TextView createWorkoutTitle;
    private Button deleteWorkout;
    private EditText workoutName;
    private ListView workoutList;
    private WorkoutListAdapter workoutListAdapter;
    private List<WorkoutItem> workoutItems;
    private List<List<List<BoulderItem>>> workoutListItems;

    // Expandable list view variables
    private ExpandableListView expandableListView;
    private WorkoutExpandableListAdapter setListAdapter;
    private List<List<BoulderItem>> expandableListData;

    // Current wall variables
    private Wall wall;

    // Set if we are currently editing a workout
    private WorkoutItem workoutItem;

    private static final class MESSAGE {
        static final String NO_BOULDERS = "You have no boulders. Add some from the \"Add Boulder\" page to start creating workouts.";
        static final String NO_WORKOUTS = "You have no workouts. Add one using the form above.";
        static final String NO_SETS = "Your workout doesn't have any sets";
        static final String NO_WORKOUT_NAME = "You must give your workout a name";
        static final String NO_SET_NUMBER = "You must provide a positive number of boulders";
        static final String NO_WORKOUT_CHANGES = "You didn't make any changes to this workout";
        static final String TITLE_LENGTH = "Your workout name must have fewer than 50 characters";
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_workouts, container, false);
        root.findViewById(R.id.container).setVisibility(View.GONE);
        return root;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize models
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        fragmentModel = new ViewModelProvider(requireActivity()).get(WorkoutModel.class);

        // Check for changes in the current wall
        final ProgressBar progressBar = view.findViewById(R.id.progressBar_cyclic);
        final TextView messageText = view.findViewById(R.id.error_message);
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            // Check if there was an error
            boolean success = WallLoadingErrorHandler.handleError(result, progressBar, messageText);
            if (success) {
                view.findViewById(R.id.container).setVisibility(View.VISIBLE);
                this.wall = model.getWall(model.getCurrentWallId());
                this.workoutItems = wall.getWorkouts();

                // Display message if there are no boulders
                if (wall.getBoulders().isEmpty()) {
                    messageText.setText(MESSAGE.NO_BOULDERS);
                    messageText.setVisibility(View.VISIBLE);
                    return;
                }
                // Display message if there are no workouts
                if (workoutItems.isEmpty()) {
                    messageText.setText(MESSAGE.NO_WORKOUTS);
                    messageText.setVisibility(View.VISIBLE);
                }
                // Set up the view
                setupView(view);
            }
        });
    }

    private void setupView(View view) {
        // Set title
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(String.format("%s", wall.getName()));

        // Get views
        scrollView = view.findViewById(R.id.container);
        workoutList = view.findViewById(R.id.workout_list);
        View dialog = view.findViewById(R.id.workout_dialog);
        expandableListView = dialog.findViewById(R.id.setList);
        createWorkoutTitle = dialog.findViewById(R.id.createWorkoutTitle);
        deleteWorkout = dialog.findViewById(R.id.deleteWorkout);
        workoutName = dialog.findViewById(R.id.workoutName);

        // Initialize the workout list view
        setupWorkoutList();

        // Initialize the expandable list view for workout sets
        setupWorkoutSetExpandableList();

        // Set up buttons
        Button addSet = dialog.findViewById(R.id.addSet);
        Button saveWorkout = dialog.findViewById(R.id.save);
        Button cancel = dialog.findViewById(R.id.cancel);
        addSet.setOnClickListener(this::onClickAddSet);
        saveWorkout.setOnClickListener(this::onClickSaveWorkout);
        cancel.setOnClickListener(v -> resetExpandableListData());
        deleteWorkout.setOnClickListener(this::onDeleteWorkout);
    }

    /*-------------------------------Handle Workout Set List--------------------------------------*/

    private void setupWorkoutSetExpandableList() {
        // Initialize expandable list view data
        expandableListData = new ArrayList<>();

        // Setup expandable list view
        List<String> currentGrades = getSortedWallGrades(wall.getBoulders().keySet());
        setListAdapter = new WorkoutExpandableListAdapter(requireContext(), null,
                expandableListData, currentGrades);
        setListAdapter.setOnDropdownClick(this::toggleDropdown);
        setListAdapter.setOnDeleteClick(this::onClickDeleteSet);
        setListAdapter.setOnInputChangedListener(this::onCreateRandomizedSet);
        expandableListView.setAdapter(setListAdapter);
    }

    private void resetExpandableListData() {
        expandableListData.clear();
        setListAdapter.setItems(expandableListData);
        workoutName.getText().clear();
        workoutName.setError(null);
        createWorkoutTitle.setText(String.format("%s", "Add Workout"));
        deleteWorkout.setVisibility(View.GONE);
        setListViewHeightBasedOnChildren(expandableListView);
    }

    private void onClickAddSet(View v) {
        // Mark previous set
        expandableListData.add(new ArrayList<>());
        setListAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(expandableListView);
    }

    private void onClickDeleteSet(int listPosition) {
        expandableListData.remove(listPosition);
        setListAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(expandableListView);
    }

    private void onCreateRandomizedSet(int groupPosition, int selectedNumber, String selectedGrade) {
        // Create randomized boulder set
        List<BoulderItem> allBoulderOfGrade = Objects.requireNonNull(wall.getBoulders().get(selectedGrade));
        List<BoulderItem> boulders = createRandomizedBoulderList(selectedNumber, allBoulderOfGrade);
        expandableListData.set(groupPosition, boulders);
        setListAdapter.setItems(expandableListData);
    }

    private boolean toggleDropdown(ImageButton dropdown, int groupPosition) {
        if (expandableListView.isGroupExpanded(groupPosition)) {
            expandableListView.collapseGroup(groupPosition);
            dropdown.setImageResource(R.drawable.ic_baseline_expand_more_24);
        }
        else {
            expandableListView.expandGroup(groupPosition, true);
            dropdown.setImageResource(R.drawable.ic_baseline_expand_less_24);
        }
        setListViewHeightBasedOnChildren(expandableListView);
        return false;
    }

    /*---------------------------Handle Existing Workout List-------------------------------------*/

    private void setupWorkoutList() {
        // Setup data for workout list
        workoutListItems = new ArrayList<>();
        for (WorkoutItem w : workoutItems) {
            workoutListItems.add(wall.workoutToBoulders(w));
        }

        // Setup list of workout
        workoutListAdapter = new WorkoutListAdapter(requireContext(), workoutItems, workoutListItems);
        workoutListAdapter.setOnStartWorkout(this::onClickStartWorkout);
        workoutListAdapter.setOnEditWorkout(this::onClickEditWorkout);
        workoutList.setAdapter(workoutListAdapter);
        setListViewHeightBasedOnChildren(workoutList);
    }

    private void onClickStartWorkout(String workout_id) {
        fragmentModel.setWorkout_id(workout_id);
        fragmentModel.setWall_id(wall.getId());
        fragmentModel.setBoulderIdx(0);
        fragmentModel.setSetIdx(0);
        NavHostFragment.findNavController(WorkoutFragment.this)
                .navigate(R.id.nav_start_workout);
    }

    private void onClickEditWorkout(int listPosition) {
        this.workoutItem = workoutItems.get(listPosition);
        // Clear the current expandable list data
        expandableListData.clear();
        // Update the create workout view
        workoutName.setText(workoutItem.getWorkout_name());
        createWorkoutTitle.setText(String.format("%s", "Edit Workout"));
        deleteWorkout.setVisibility(View.VISIBLE);
        // Update the expandable list data
        expandableListData = wall.workoutToBoulders(workoutItem);
        setListAdapter.setItems(expandableListData);
        setListViewHeightBasedOnChildren(expandableListView);
        // Scroll to top
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    private void onDeleteWorkout(View view) {
        if (workoutItem == null) return;
        // Create Alert dialog
        AlertDialog alertDialog = AlertDialogManager.createDeleteWorkoutDialog(
                workoutItem.getWorkout_name(), requireContext());
        alertDialog.show();

        // Get views
        Button cancel = alertDialog.findViewById(R.id.cancel);
        Button delete = alertDialog.findViewById(R.id.delete);
        assert (cancel != null) && (delete != null);

        cancel.setOnClickListener(v -> alertDialog.cancel());
        delete.setOnClickListener(v -> {
            alertDialog.cancel();
            // Delete item
            int index = workoutItems.indexOf(workoutItem);
            model.deleteWorkoutItem(workoutItem);
            // Clear edit workout dialog
            resetExpandableListData();
            // Remove item from workout list
            workoutListItems.remove(index);
            workoutListAdapter.setItems(workoutItems, workoutListItems);
            setListViewHeightBasedOnChildren(workoutList);
        });
    }

    private void onClickSaveWorkout(View v) {
        // Make sure user can't click any buttons while saving
        v.setClickable(false);
        deleteWorkout.setClickable(false);
        saveWorkout();
        v.setClickable(true);
        deleteWorkout.setClickable(true);
    }

    /*------------------------------Handle Creating New Workout-----------------------------------*/

    private void saveWorkout() {
        // Check for errors in the form
        String name = workoutName.getText().toString();
        List<List<String>> boulderIds = bouldersToIds(expandableListData);
        if (checkForError(name, boulderIds)) return;

        // Check if we are editing a workout or creating a new one
        String workout_id = (workoutItem == null) ?
                UUID.randomUUID().toString().substring(0, 8) : workoutItem.getWorkout_id();

        // Create workout item
        WorkoutItem newWorkoutItem = new WorkoutItem(model.getStitchUserId(), wall.getId(), workout_id,
                name, boulderIds);

        // Add workout item to repository
        model.addWorkoutItem(newWorkoutItem);

        // Update list view
        if (workoutItems.contains(workoutItem)) {
            int index = workoutItems.indexOf(workoutItem);
            workoutItems.set(index, newWorkoutItem);
            workoutListItems.set(index, wall.workoutToBoulders(workoutItem));
            Toast.makeText(requireContext(), String.format("Updated Workout \"%s\"", name), Toast.LENGTH_SHORT).show();
        }
        else {
            this.workoutItem = newWorkoutItem;
            workoutListItems.add(wall.workoutToBoulders(workoutItem));
            Toast.makeText(requireContext(), String.format("Saved Workout \"%s\"", name), Toast.LENGTH_SHORT).show();
        }
        workoutListAdapter.setItems(workoutItems, workoutListItems);
        setListViewHeightBasedOnChildren(workoutList);
    }

    private boolean checkForError(String name, List<List<String>> boulderIds) {
        // Make sure there is at least one set
        if (expandableListData.size() == 0) {
            Toast.makeText(requireContext(), MESSAGE.NO_SETS, Toast.LENGTH_SHORT).show();
            return true;
        }
        boolean wasError = false;
        // Make sure user gave a title
        String title = workoutName.getText().toString();
        if (title.isEmpty()) {
            workoutName.setError(MESSAGE.NO_WORKOUT_NAME);
            wasError = true;
        }
        // Make sure title wasn't too long
        if (title.length() > 50) {
            workoutName.setError(MESSAGE.TITLE_LENGTH);
            wasError = true;
        }
        // Make sure no set number has 0 climbs
        for (int i = 0; i < expandableListData.size(); i++) {
            if (expandableListData.get(i).isEmpty()) {
                View child = expandableListView.getChildAt(i);
                ((TextView) (child.findViewById(R.id.errorMsg))).setError(MESSAGE.NO_SET_NUMBER);
                wasError = true;
            }
        }
        // If we are editing a workout, make sure workout has actually been edited
        if (workoutItem != null) {
            if (workoutItem.getWorkout_name().equals(name) &&
                    workoutItem.getWorkoutSets().equals(boulderIds)) {
                Toast.makeText(requireContext(), MESSAGE.NO_WORKOUT_CHANGES, Toast.LENGTH_SHORT).show();
                wasError = true;
            }
        }
        return wasError;
    }

    /*-------------------------------------Helper Functions---------------------------------------*/

    private static List<String> getSortedWallGrades(Set<String> currentGradesSet) {
        // Get all of the grades that the current wall has in sorted order
        List<String> currentGrades = new ArrayList<>();
        for (String s : SharedViewModel.BOULDER_GRADES) {
            if (currentGradesSet.contains(s)) { currentGrades.add(s); }
        }
        return currentGrades;
    }

    private static List<BoulderItem> createRandomizedBoulderList(int n, List<BoulderItem> boulders) {
        Random rand = new Random();
        List<BoulderItem> randomBoulders = new ArrayList<>();
        int maxIndex = boulders.size();
        for (int i = 0; i < n; i++) {
            randomBoulders.add(boulders.get(rand.nextInt(maxIndex)));
        }
        return randomBoulders;
    }

    private static List<List<String>> bouldersToIds(List<List<BoulderItem>> setBoulders) {
        List<List<String>> setBoulderIds = new ArrayList<>();
        for (List<BoulderItem> boulders : setBoulders) {
            List<String> ids = new ArrayList<>();
            for (BoulderItem b : boulders) {
                ids.add(b.getBoulder_id());
            }
            setBoulderIds.add(ids);
        }
        return setBoulderIds;
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        // Measure height of each child
        int totalHeight = 0;
        for (int i = 0, len = listAdapter.getCount(); i < len; i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        // Set the list view height to exactly fit its children
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight
                + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        workoutList = null;
        expandableListView = null;
        workoutListItems = null;
    }
}
