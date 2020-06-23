package com.example.myroutes.ui.workouts;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.util.Consumer;
import androidx.room.Index;

import com.example.myroutes.R;
import com.example.myroutes.db.entities.BoulderItem;
import com.example.myroutes.db.entities.WorkoutItem;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Class to dynamically update list view for search items
public class WorkoutListAdapter extends ArrayAdapter<WorkoutItem> {
    private static final String TAG = "WorkoutListAdapter";
    private final Context context;
    private List<WorkoutItem> workouts;
    private List<List<List<BoulderItem>>> workoutBouldersList;
    private Consumer<String> onStartWorkout;
    private Consumer<Integer> onEditWorkout;

    public WorkoutListAdapter(Context context,
                              List<WorkoutItem> workouts,
                              List<List<List<BoulderItem>>> workoutBouldersList) {
        super(context, 0, workouts);
        this.context = context;
        this.workouts = workouts;
        this.workoutBouldersList = workoutBouldersList;
    }

    public void setItems(List<WorkoutItem> workouts,
                         List<List<List<BoulderItem>>> workoutBouldersList) {
        this.workouts = workouts;
        this.workoutBouldersList = workoutBouldersList;
        notifyDataSetChanged();
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
        TextView boulderInfo = convertView.findViewById(R.id.boulderCount);
        ImageView edit = convertView.findViewById(R.id.editWorkout);
        Button start = convertView.findViewById(R.id.startWorkout);
        BarChart barChart = convertView.findViewById(R.id.barChart);

        if (onStartWorkout != null) {
            start.setOnClickListener(v -> onStartWorkout.accept(item.getWorkout_id()));
        }

        if (onEditWorkout != null) {
            edit.setOnClickListener(v -> onEditWorkout.accept(position));
        }

        // Set information
        name.setText(item.getWorkout_name());
        boulderInfo.setText(String.format(Locale.US, "%d Total Boulders", item.getBoulderCount()));
        setupBarChart(position, barChart);

        return convertView;
    }

    private void setupBarChart(int position, BarChart barChart) {
        if (position >= workoutBouldersList.size()) return;
        // Create data for bar chart
        List<List<BoulderItem>> boulderData = workoutBouldersList.get(position);
        List<BarEntry> entries = new ArrayList<>(); // Data to plot
        List<String> topLabels = new ArrayList<>(); // Labels to go on top of the bars
        List<String> labels = new ArrayList<>();    // X-axis labels

        // Convert list of boulder sets to (x, y) bar coordinates to plot
        for (int i = 0; i < boulderData.size(); i++) {
            List<BoulderItem> boulders = boulderData.get(i);
            int count = boulders.size();
            String grade = boulders.get(0).getBoulder_grade();
            // Store index and boulder count for each set
            entries.add(new BarEntry(i, count));
            topLabels.add(String.format(Locale.US, "%d %s", count, grade));
            labels.add(String.format(Locale.US, "Set %d", i + 1));
        }

        // Create dataset
        BarDataSet dataSet = new BarDataSet(entries, null);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barData.setValueTextSize(12);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return topLabels.get((int) barEntry.getX());
            }
        });

        // Setup x-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-50f);

        // Setup y-axes
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setLabelCount(4);
        yAxis.setGranularityEnabled(true);
        yAxis.setGranularity(1f);
        yAxis.setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);

        // Setup chart
        barChart.setData(new BarData(dataSet));
        barChart.setScaleEnabled(false);
        barChart.setDragYEnabled(false);
        barChart.setFitBars(true);
        barChart.setDrawGridBackground(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        barChart.getLayoutParams().width =
                Math.min(150 * entries.size() + 100, 900);

        barChart.invalidate();
    }
}
