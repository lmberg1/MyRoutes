package com.example.myroutes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.util.Consumer;

import com.example.myroutes.db.entities.WorkoutItem;

import java.util.List;

// Class to dynamically update list view for search items
public class WorkoutListAdapter extends ArrayAdapter<WorkoutItem> {
    private static final String TAG = "MyWorkoutSetAdapter";
    private final Context context;
    private List<WorkoutItem> workouts;
    private Consumer<String> onStartWorkout;
    private Consumer<Integer> onEditWorkout;

    public WorkoutListAdapter(Context context, List<WorkoutItem> workouts) {
        super(context, 0, workouts);
        this.context = context;
        this.workouts = workouts;
    }

    public void setOnStartWorkout(Consumer<String> onStartWorkout) {
        this.onStartWorkout = onStartWorkout;
    }

    public void setOnEditWorkout(Consumer<Integer> onEditWorkout) {
        this.onEditWorkout = onEditWorkout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the view for the search item (from list_search.xml)
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_workout_item, parent, false);
        }

        WorkoutItem item = getItem(position);
        if (item == null) return convertView;

        TextView name = convertView.findViewById(R.id.name);
        ImageView edit = convertView.findViewById(R.id.editWorkout);
        Button start = convertView.findViewById(R.id.startWorkout);

        if (onStartWorkout != null) {
            start.setOnClickListener(v -> onStartWorkout.accept(item.getWorkout_id()));
        }

        if (onEditWorkout != null) {
            edit.setOnClickListener(v -> onEditWorkout.accept(position));
        }

        // Set information
        name.setText(item.getWorkout_name());

        return convertView;
    }
}
