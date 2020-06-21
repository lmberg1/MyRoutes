package com.example.myroutes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.db.SharedViewModel;
import com.example.myroutes.db.entities.Wall;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.example.myroutes.util.AlertDialogManager;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Workout list variables
    private TextView createWorkoutTitle;
    private ImageButton deleteWorkout;
    private EditText workoutName;
    private ListView workoutList;
    private WorkoutListAdapter workoutListAdapter;
    private List<WorkoutItem> workoutItems;

    // Expandable list view variables
    private ExpandableListView expandableListView;
    private WorkoutExpandableListAdapter setListAdapter;
    private List<List<BoulderItem>> expandableListData;
    private List<WorkoutExpandableListAdapter.Mode> expandableListGroupMode;

    // Current wall variables
    private List<String> currentGrades;
    private Wall wall;

    // Set if we are currently editing a workout
    private WorkoutItem workoutItem;
    private boolean edited = false;

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
            else if (result == SharedViewModel.Status.SUCCESS){
                progressBar.setVisibility(View.GONE);
                messageText.setVisibility(View.GONE);
                view.findViewById(R.id.container).setVisibility(View.VISIBLE);
                this.wall = model.getWall(model.getCurrentWallId());
                this.workoutItems = wall.getWorkouts();

                // Display message if there are no boulders
                if (this.wall.getBoulders().isEmpty()) {
                    messageText.setText(String.format("%s", "You have no boulders. Add some from the Home page to start creating workouts"));
                    return;
                }
                // Otherwise, set up the view
                setupView(view);
            }
        });
    }

    private void setupView(View view) {
        // Set title
        TextView wallName = view.findViewById(R.id.wallName);
        wallName.setText(String.format("%s", wall.getName()));

        // Get all of the grades that the current wall has
        List<String> allGrades = Arrays.asList(SharedViewModel.BOULDER_GRADES);
        Set<String> currentGradesSet = wall.getBoulders().keySet();
        currentGrades = new ArrayList<>();
        for (String s : allGrades) {
            if (currentGradesSet.contains(s)) { currentGrades.add(s); }
        }

        // Setup list of workout
        workoutList = view.findViewById(R.id.workout_list);
        workoutListAdapter = new WorkoutListAdapter(requireContext(), workoutItems);
        workoutListAdapter.setOnStartWorkout(this::onClickStartWorkout);
        workoutListAdapter.setOnEditWorkout(this::onClickEditWorkout);
        workoutList.setAdapter(workoutListAdapter);

        // Get views in the create workout dialog
        View dialog = view.findViewById(R.id.workout_dialog);
        expandableListView = dialog.findViewById(R.id.setList);
        createWorkoutTitle = dialog.findViewById(R.id.createWorkoutTitle);
        deleteWorkout = dialog.findViewById(R.id.deleteWorkout);
        workoutName = dialog.findViewById(R.id.workoutName);

        // Initialize expandable list view data
        expandableListData = new ArrayList<>();
        expandableListData.add(new ArrayList<>());

        // Setup expandable list view
        setListAdapter = new WorkoutExpandableListAdapter(requireContext(), expandableListGroupMode, expandableListData, currentGrades);
        setListAdapter.setOnDropdownClick(this::toggleDropdown);
        expandableListView.setAdapter(setListAdapter);
        setListAdapter.setOnInputChangedListener((groupPosition, selectedNumber, selectedGrade) -> {
            edited = true;
            // Create randomized boulder set
            List<BoulderItem> allBoulderOfGrade = Objects.requireNonNull(wall.getBoulders().get(selectedGrade));
            List<BoulderItem> boulders = createRandomizedBoulderList(selectedNumber, allBoulderOfGrade);
            expandableListData.set(groupPosition, boulders);
            setListAdapter.setItems(expandableListData);
        });

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

    private void resetExpandableListData() {
        expandableListData.clear();
        expandableListData.add(new ArrayList<BoulderItem>());
        setListAdapter.setItems(expandableListData);
        workoutName.getText().clear();
        workoutName.setError(null);
        createWorkoutTitle.setText(String.format("%s", "Add Workout"));
        deleteWorkout.setVisibility(View.GONE);
    }

    private void onClickAddSet(View v) {
        // Mark previous set
        expandableListData.add(new ArrayList<BoulderItem>());
        setListAdapter.notifyDataSetChanged();
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

        return false;
    }

    /*---------------------------Handle Existing Workout List-------------------------------------*/

    private void onClickStartWorkout(String workout_id) {
        fragmentModel.setWorkout_id(workout_id);
        fragmentModel.setWall_id(wall.getId());
        fragmentModel.setBoulderIdx(0);
        fragmentModel.setSetIdx(0);
        NavHostFragment.findNavController(WorkoutFragment.this)
                .navigate(R.id.nav_start_workout);
    }

    private void onClickEditWorkout(int listPosition) {
        // Clear the current expandable list data
        expandableListData.clear();
        // Update the expandable list data
        this.workoutItem = workoutItems.get(listPosition);
        expandableListData = idsToBoulders(wall, workoutItem.getWorkoutSets());
        setListAdapter.setItems(expandableListData);
        // Update the create workout view
        workoutName.setText(workoutItem.getWorkout_name());
        createWorkoutTitle.setText(String.format("%s", "Edit Workout"));
        deleteWorkout.setVisibility(View.VISIBLE);
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
            model.deleteWorkoutItem(workoutItem);
            // Clear edit workout dialog
            resetExpandableListData();
            // Remove item from workout list
            workoutItems.remove(workoutItem);
            workoutListAdapter.notifyDataSetChanged();
        });
    }

    /*------------------------------Handle Creating New Workout-----------------------------------*/

    private void onClickSaveWorkout(View v) {
        // Make sure user can't click any buttons while saving
        v.setClickable(false);
        deleteWorkout.setClickable(false);
        saveWorkout();
        v.setClickable(true);
        deleteWorkout.setClickable(true);
    }

    private void saveWorkout() {
        // Handle errors
        if (expandableListData.size() == 0) {
            Toast.makeText(requireContext(), "Your workout doesn't have any sets", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean wasError = false;
        if (workoutName.getText().toString().isEmpty()) {
            String error = "You must give your workout a name";
            workoutName.setError(error);
            wasError = true;
        }
        String error = "You must provide a number";
        for (int i = 0; i < expandableListData.size(); i++) {
            if (expandableListData.get(i).isEmpty()) {
                View child = expandableListView.getChildAt(i);
                ((TextView) (child.findViewById(R.id.errorMsg))).setError(error);
                wasError = true;
            }
        }
        if (wasError) { return; }

        // Check if we are editing a workout or creating a new one
        String workout_id = (workoutItem == null) ?
                UUID.randomUUID().toString().substring(0, 8) : workoutItem.getWorkout_id();

        // If we are editing a workout, make sure workout has actually been edited
        String name = workoutName.getText().toString();
        List<List<String>> boulderIds = bouldersToIds(expandableListData);
        if (workoutItem != null) {
            if (workoutItem.getWorkout_name().equals(name) &&
                    workoutItem.getWorkoutSets().equals(boulderIds)) {
                Toast.makeText(requireContext(), "You didn't make any changes to this workout", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create workout item
        WorkoutItem newWorkoutItem = new WorkoutItem(model.getStitchUserId(), wall.getId(), workout_id,
                name, boulderIds);

        // Add workout item to repository
        model.addWorkoutItem(newWorkoutItem);

        // Update list view
        if (workoutItems.contains(workoutItem)) {
            workoutItems.set(workoutItems.indexOf(workoutItem), newWorkoutItem);
            Toast.makeText(requireContext(), String.format("Updated Workout \"%s\"", name), Toast.LENGTH_SHORT).show();
        }
        else {
            workoutItems.add(workoutItem);
            Toast.makeText(requireContext(), String.format("Saved Workout \"%s\"", name), Toast.LENGTH_SHORT).show();
        }
        this.workoutItem = newWorkoutItem;
        workoutListAdapter.notifyDataSetChanged();

    }

    /*-------------------------------------Helper Functions---------------------------------------*/

    private static List<BoulderItem> createRandomizedBoulderList(int n, List<BoulderItem> boulders) {
        Random rand = new Random();
        List<BoulderItem> randomBoulders = new ArrayList<>();
        int maxIndex = boulders.size();
        for (int i = 0; i < n; i++) {
            randomBoulders.add(boulders.get(rand.nextInt(maxIndex)));
        }
        return randomBoulders;
    }

    private static List<List<BoulderItem>> idsToBoulders(Wall wall, List<List<String>> ids) {
        List<List<BoulderItem>> boulders = new ArrayList<>();
        for (List<String> setIds : ids) {
            List<BoulderItem> setBoulders = new ArrayList<>();
            for (String id : setIds) {
                BoulderItem b = wall.searchBoulderId(id);
                if (b != null) { setBoulders.add(b); }
            }
            if (!setBoulders.isEmpty()) { boulders.add(setBoulders); }
        }
        return boulders;
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
}
