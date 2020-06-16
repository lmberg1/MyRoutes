package com.example.myroutes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.example.myroutes.db.mongoClasses.BoulderItem;

import java.util.List;
import java.util.Locale;

public class StartWorkoutExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<List<BoulderItem>> expandableListDetail;

    public StartWorkoutExpandableListAdapter(Context context, List<List<BoulderItem>> expandableListDetail) {
        this.context = context;
        this.expandableListDetail = expandableListDetail;
    }

    public void setItems(List<List<BoulderItem>> expandableListDetail) {
        this.expandableListDetail = expandableListDetail;
        notifyDataSetChanged();
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
        number.setText(String.format(Locale.US, "%d.", expandedListPosition + 1));
        expandedListTextView.setText(boulderItem.getBoulder_name());
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
            convertView = layoutInflater.inflate(R.layout.list_group, null);
        }

        List<BoulderItem> info = (List<BoulderItem>) getGroup(listPosition);
        if (info == null) return convertView;
        if (info.size() == 0) return convertView;

        // Set title
        TextView setTitle = convertView.findViewById(R.id.listTitle);
        String title = String.format(Locale.US, "Set %d", listPosition + 1);
        setTitle.setText(title);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}
