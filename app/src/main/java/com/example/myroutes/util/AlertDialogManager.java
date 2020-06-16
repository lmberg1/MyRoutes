package com.example.myroutes.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.myroutes.R;
import com.example.myroutes.db.SharedViewModel;

public class AlertDialogManager {

    public static AlertDialog createAddSetDialog(Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_create_workout_set, null);

        // Create the dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Create Bouldering Set");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createAddWallDialog(Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_savewall, null);

        // Create the dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Save Wall");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createAddBoulderDialog(Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_save_boulder, null);

        // Setup spinner
        Spinner spinner = popupInputDialogView.findViewById(R.id.saveBoulderSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, SharedViewModel.BOULDER_GRADES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Create the dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Save Boulder");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createConnectToWallDialog(Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_enter_wall_id, null);

        // Set up AlertDialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Connect To Wall");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createShareWallDialog(WallMetadata item, Context context) {
        // Get layout inflater object.
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        // Inflate the popup dialog from a layout xml file.
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_show_wall_id, null);

        // Set the wall id
        TextView wallId = popupInputDialogView.findViewById(R.id.wallId);
        wallId.setText(item.getWall_id());

        // Set the inflated layout view object to the AlertDialog builder.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Share Wall");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createEditWallDialog(WallMetadata item, Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_edit_wall, null);

        // Set the wall id
        EditText wallName = popupInputDialogView.findViewById(R.id.wallName);
        wallName.setText(item.getWall_name());

        // Set the inflated layout view object to the AlertDialog builder.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Edit Wall Information");
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }

    public static AlertDialog createDeleteWallDialog(WallMetadata item, Context context) {
        // Inflate the layout
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupInputDialogView = layoutInflater.inflate(R.layout.alert_dialog_delete_confirmation, null);

        // Set up AlertDialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setTitle("Delete Confirmation");
        alertDialogBuilder.setMessage(String.format("Are you sure you want to delete %s?", item.getWall_name()));
        alertDialogBuilder.setView(popupInputDialogView);

        return alertDialogBuilder.create();
    }
}
