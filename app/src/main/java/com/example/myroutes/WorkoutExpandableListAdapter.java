package com.example.myroutes;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.util.Consumer;

import com.example.myroutes.db.entities.BoulderItem;
import com.google.android.gms.common.util.BiConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutExpandableListAdapter extends BaseExpandableListAdapter {
    public enum Mode {INITIALIZING, UPDATING};
    private static final int MAX_NUMBER = 20;
    private List<String> numberOptions;
    private Context context;
    private List<Mode> expandableListGroupMode;
    private List<List<BoulderItem>> expandableListDetail;
    private List<String> gradeList;
    private BiConsumer<ImageButton, Integer> onDropdownClick;
    private Consumer<Integer> onDeleteClick;

    private int preferredPadding;

    public WorkoutExpandableListAdapter(Context context,
                                        List<Mode> expandableListGroupMode,
                                        List<List<BoulderItem>> expandableListDetail,
                                        List<String> currentGrades) {
        this.context = context;
        this.expandableListGroupMode = expandableListGroupMode;
        this.expandableListDetail = expandableListDetail;
        this.gradeList = currentGrades;
        // Possible numbers of climbs in a set
        numberOptions = new ArrayList<>();
        for (int i = 0; i < MAX_NUMBER + 1; i++) {
            numberOptions.add(Integer.toString(i));
        }
        preferredPadding = Math.round(convertDpToPixel(16, context));
    }

    public void setGroupMode(List<Mode> expandableListGroupMode) {
        this.expandableListGroupMode = expandableListGroupMode;
    }

    public void setItems(List<List<BoulderItem>> expandableListDetail) {
        this.expandableListDetail = expandableListDetail;
        notifyDataSetChanged();
    }

    public void setOnDropdownClick(BiConsumer<ImageButton, Integer> onDropdownClick) {
        this.onDropdownClick = onDropdownClick;
    }

    public void setOnDeleteClick(Consumer<Integer> onDeleteClick) {
        this.onDeleteClick = onDeleteClick;
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final BoulderItem boulderItem = (BoulderItem) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_set_child, null);
        }
        // Get views
        TextView number = convertView.findViewById(R.id.number);
        TextView expandedListTextView = convertView.findViewById(R.id.expandedListItem);

        // Set views
        String title = String.format("%s (%s)", boulderItem.getBoulder_name(), boulderItem.getBoulder_grade());
        number.setText(String.format(Locale.US, "%d.", expandedListPosition + 1));
        expandedListTextView.setText(title);

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.get(listPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.expandableListDetail.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.expandableListDetail.get(groupPosition).get(childPosition);
    }

    @Override
    public int getGroupCount() {
        return this.expandableListDetail.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_set_group, null);

            // Initialize number spinner
            Spinner numberSpinner = convertView.findViewById(R.id.numberSpinner);
            ArrayAdapter<String> numberAdapter = new ArrayAdapter<>(context, R.layout.spinner_selected, numberOptions);
            numberAdapter.setDropDownViewResource(R.layout.spinner_item);
            numberSpinner.setAdapter(numberAdapter);

            // Initialize grade spinner
            Spinner spinner = convertView.findViewById(R.id.gradeSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_selected, gradeList);
            adapter.setDropDownViewResource(R.layout.spinner_item);
            spinner.setAdapter(adapter);
        }

        List<BoulderItem> info = (List<BoulderItem>) getGroup(listPosition);
        if (info == null) return convertView;

        // Make sure error message is not carried through
        ((TextView) convertView.findViewById(R.id.errorMsg)).setError(null);

        // Get views
        ImageView deleteButton = convertView.findViewById(R.id.deleteSet);
        ImageButton imageButton = convertView.findViewById(R.id.dropdownButton);
        TextView setTitle = convertView.findViewById(R.id.set_title);
        Spinner numberSpinner = convertView.findViewById(R.id.numberSpinner);
        Spinner spinner = convertView.findViewById(R.id.gradeSpinner);

        // Check if values are already initialized
        if ((getChildrenCount(listPosition) != getNumberFromSpinner(numberSpinner))) {
            if (getChildrenCount(listPosition) != 0) {
                String grade = ((BoulderItem) getChild(listPosition, 0)).getBoulder_grade();
                spinner.setSelection(gradeList.indexOf(grade));
            }
            numberSpinner.setSelection(getChildrenCount(listPosition));
        }

        // Set listeners for buttons
        imageButton.setOnClickListener(v -> onDropdownClick.accept((ImageButton) v, listPosition));
        deleteButton.setOnClickListener(v -> onDeleteClick.accept(listPosition));

        // Listen for changes in the spinners
        AdapterView.OnItemSelectedListener spinnerListener = createOnItemSelectedListener(listPosition, numberSpinner, spinner);
        numberSpinner.setOnItemSelectedListener(spinnerListener);
        spinner.setOnItemSelectedListener(spinnerListener);

        // Set title
        String title = String.format(Locale.US, "Set %d:", listPosition + 1);
        setTitle.setText(title);

        return convertView;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private static int getNumberFromSpinner(Spinner s) {
        return Integer.parseInt((String) s.getSelectedItem());
    }

    private static String getGradeFromSpinner(Spinner s) {
        return (String) s.getSelectedItem();
    }

    // Listener for when item in group spinner is selected
    private AdapterView.OnItemSelectedListener createOnItemSelectedListener(int listPosition,
                                                                            Spinner numberSpinner,
                                                                            Spinner gradeSpinner) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int n = getNumberFromSpinner(numberSpinner);
                String grade = getGradeFromSpinner(gradeSpinner);
                // Don't call callback if the number spinner hasn't been set
                if (n == 0) return;
                if (getChildrenCount(listPosition) != 0) {
                    String currentGrade = ((BoulderItem) getChild(listPosition, 0)).getBoulder_grade();
                    if ((n == getChildrenCount(listPosition)) && currentGrade.equals(grade)) return;
                }
                if (listener != null) {
                    listener.onInputChanged(listPosition, n, grade);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    /**
     * OnInputChangedListener Listener
     */
    OnInputChangedListener listener;

    public void setOnInputChangedListener(OnInputChangedListener listener) {
        this.listener = listener;
    }

    public interface OnInputChangedListener {
        void onInputChanged(int groupPosition, int selectedNumber, String selectedGrade);
    }
}
