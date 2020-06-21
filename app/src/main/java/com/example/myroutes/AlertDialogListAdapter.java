package com.example.myroutes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.core.util.Consumer;

import java.util.List;

// Class to dynamically update list view for search items
public class AlertDialogListAdapter extends ArrayAdapter<Integer> {
    private static final String TAG = "AlertDialogListAdapter";
    private final Context context;
    private List<Integer> workouts;
    private Consumer<String> onStartWorkout;

    public AlertDialogListAdapter(Context context, List<Integer> workouts) {
        super(context, 0, workouts);
        this.context = context;
        this.workouts = workouts;
    }

    public void setOnStartWorkout(Consumer<String> onStartWorkout) {
        this.onStartWorkout = onStartWorkout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the view for the search item (from list_search.xml)
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_set_group, parent, false);
        }

        Integer item = getItem(position);
        if (item == null) return convertView;

        return convertView;
    }
}
