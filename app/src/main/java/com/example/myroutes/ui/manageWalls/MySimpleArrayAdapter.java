package com.example.myroutes.ui.manageWalls;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.util.PreferenceManager;
import com.example.myroutes.R;
import com.example.myroutes.util.WallMetadata;
import com.google.android.gms.common.util.BiConsumer;

import java.util.List;

// Class to dynamically update list view for search items
public class MySimpleArrayAdapter extends ArrayAdapter<WallMetadata> {
    private final Context context;
    private List<WallMetadata> wallInfo;
    private String currentWallId;
    private BiConsumer<WallMetadata, Boolean> onEdit;
    private Consumer<WallMetadata> onDelete;
    private Consumer<WallMetadata> onDeletePermanently;
    private int defaultPosition = 0;

    public MySimpleArrayAdapter(Context context, List<WallMetadata> wallInfo) {
        super(context, 0, wallInfo);
        this.context = context;
        this.wallInfo = wallInfo;
    }

    public void setWallItems(List<WallMetadata> wallInfo) {
        this.wallInfo = wallInfo;
        notifyDataSetChanged();
    }

    public void setCurrentId(String id) {
        this.currentWallId = id;
        notifyDataSetChanged();
    }

    public void setOnEdit(BiConsumer<WallMetadata, Boolean> onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDelete(Consumer<WallMetadata> onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnDeletePermanently(Consumer<WallMetadata> onDeletePermanently) {
        this.onDeletePermanently = onDeletePermanently;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the view for the search item (from list_search.xml)
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.card_wall_item, parent, false);
        }

        WallMetadata info = getItem(position);
        if (info == null) return convertView;

        // Put a border around the currently selected wall
        if (info.getWall_id().equals(currentWallId)) {
            convertView.setBackgroundResource(R.drawable.border);
        }
        else {
            convertView.setBackgroundResource(0);
        }

        TextView txtName = convertView.findViewById(R.id.name);
        TextView txtRole = convertView.findViewById(R.id.role);
        TextView txtDefault = convertView.findViewById(R.id.defaultTag);
        ImageView share = convertView.findViewById(R.id.shareButton);
        ImageView edit = convertView.findViewById(R.id.editWallInfo);
        share.setOnClickListener(v -> onShareClick(v, position));
        edit.setOnClickListener(v -> onEditClick(position));

        // Set information in each view
        String role = (info.getRole() == PreferenceManager.Role.OWNER) ?
                String.format("(%s)", "owner") : "";
        txtName.setText(info.getWall_name());
        txtRole.setText(role);
        Log.e("Adapter", String.format("%d %d", position, defaultPosition));
        txtDefault.setVisibility((position == defaultPosition) ? View.VISIBLE : View.GONE);
        convertView.findViewById(R.id.emptyText).setVisibility((position == defaultPosition) ? View.VISIBLE : View.GONE);

        return convertView;
    }

    private void onShareClick(View view, int pos) {
        WallMetadata item = getItem(pos);
        if (item == null) return;

        // Create AlertDialog and show.
        final AlertDialog alertDialog = AlertDialogManager.createShareWallDialog(item, context);
        alertDialog.show();

        // Let user cancel dialog
        Button done = alertDialog.findViewById(R.id.done);
        assert done != null;
        done.setOnClickListener(v -> alertDialog.cancel());
    }

    private void onEditClick(int pos) {
        WallMetadata item = getItem(pos);
        if (item == null) return;

        // Create AlertDialog and show.
        final AlertDialog alertDialog = AlertDialogManager.createEditWallDialog(item, pos == defaultPosition, context);
        alertDialog.show();

        // Get views in the alert dialog
        EditText wallName = alertDialog.findViewById(R.id.wallName);
        Button saveEdit = alertDialog.findViewById(R.id.saveEdit);
        Button deleteWall = alertDialog.findViewById(R.id.deleteWall);
        CheckBox setDefault = alertDialog.findViewById(R.id.nextButton);
        Button cancel = alertDialog.findViewById(R.id.cancel);

        // Make sure views exist
        assert (wallName != null) && (saveEdit != null) &&
                (deleteWall != null) && (setDefault != null);

        // Handle user interactions
        saveEdit.setOnClickListener(v -> {
            // Make sure name is not empty
            if (wallName.getText().toString().isEmpty()) {
                String error = "Your wall must have a name";
                wallName.setError(error);
                return;
            }

            // Update list adapter data
            item.setWall_name(wallName.getText().toString());
            wallInfo.set(pos, item);
            if (setDefault.isChecked()) { defaultPosition = pos; }
            notifyDataSetChanged();

            // Cancel dialog
            alertDialog.cancel();

            // Apply callback function
            onEdit.accept(item, setDefault.isChecked());
        });
        deleteWall.setOnClickListener(v -> {
            onDeleteClick(pos, item, alertDialog);
        });
        cancel.setOnClickListener(v -> alertDialog.cancel());
    }

    private void onDeleteClick(int pos, WallMetadata item, AlertDialog parent) {
        final AlertDialog alertDialog = AlertDialogManager.createDeleteWallDialog(item, context);
        alertDialog.show();

        // Get views in the alert dialog
        Button deleteWall = alertDialog.findViewById(R.id.deleteWall);
        Button deletePermanent = alertDialog.findViewById(R.id.deleteWallFromDatabase);
        Button cancel = alertDialog.findViewById(R.id.cancel);

        // Make sure views exist
        assert (deleteWall != null) && (deletePermanent != null) && (cancel != null);

        // Handle user interaction
        deleteWall.setOnClickListener(v -> {
            // Update list adapter
            wallInfo.remove(item);
            notifyDataSetChanged();

            // Close both dialogs
            alertDialog.cancel();
            parent.cancel();

            // Apply callback function
            onDelete.accept(item);
        });
        deletePermanent.setOnClickListener(v -> {
            // Update list adapter
            wallInfo.remove(item);
            notifyDataSetChanged();

            // Close both dialogs
            alertDialog.cancel();
            parent.cancel();

            // Apply callback function
            onDeletePermanently.accept(item);
        });
        cancel.setOnClickListener(v -> alertDialog.cancel());
    }
}
