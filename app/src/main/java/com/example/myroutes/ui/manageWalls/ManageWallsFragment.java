package com.example.myroutes.ui.manageWalls;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myroutes.util.AlertDialogManager;
import com.example.myroutes.R;
import com.example.myroutes.SharedViewModel;
import com.example.myroutes.util.WallMetadata;
import com.example.myroutes.ui.addWall.AddWallFragmentModel;

import java.util.ArrayList;
import java.util.Set;

import static com.example.myroutes.ui.manageWalls.SelectFromGalleryUtil.GALLERY_REQUEST_CODE;

public class ManageWallsFragment extends Fragment {
    private static final String TAG = "ManageWallsFragment";

    // View models
    private SharedViewModel model;
    private AddWallFragmentModel galleryModel;
    private FragmentActivity activity;
    private Context context;

    // Helper class to select image from gallery
    private SelectFromGalleryUtil galleryUtil;

    // Wall id of currently loaded wall
    private String currentWallId;

    // List view variables
    private ListView listView;
    private MySimpleArrayAdapter adapter;
    private View currentSelectedItem;

    private static final class MESSAGE {
        static final String ALREADY_SAVED = "You already have this wall saved";
        static final String WALL_ID_LENGTH = "Wall ids are 6 characters long";
        static final String DOWNLOAD_FAILURE = "Unable to add wall. Make sure you are connected to internet.";
        static final String DOWNLOAD_NOT_FOUND = "Unable to add wall. There is no wall with that wall id.";
        static final String DOWNLOAD_SUCCESS = "Saved new wall!";
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_walls, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = requireActivity();
        context = requireContext();

        // Initialize models
        model = new ViewModelProvider(activity).get(SharedViewModel.class);
        galleryModel = new ViewModelProvider(activity).get(AddWallFragmentModel.class);

        // Get the currentWallId and watch it for updates
        currentWallId = model.getCurrentWallId();
        model.getCurrentWallStatus().observe(getViewLifecycleOwner(), result -> {
            //if (result != SharedViewModel.Status.SUCCESS) { return; }
            currentWallId = model.getCurrentWallId();
            if (adapter != null) { adapter.setCurrentId(currentWallId); }
        });

        // Initialize class to help with selecting image from gallery
        galleryUtil = new SelectFromGalleryUtil(activity);

        // Allow users to add and connect to new walls
        Button addNewWall = view.findViewById(R.id.createNewWall);
        Button connectToWall = view.findViewById(R.id.connectWall);
        addNewWall.setOnClickListener(this::onClickAddWall);
        connectToWall.setOnClickListener(this::onClickConnectToWall);

        // Inflate list of walls user has access to
        listView = view.findViewById(R.id.wall_list);
        inflateWallList();
    }

    /*-------------------------------Handle Adding New Wall---------------------------------------*/

    private void onClickAddWall(View view) {
        galleryUtil.requestExternalStoragePermissions();
        Intent intent = galleryUtil.createSelectFromGalleryIntent();
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Called when user selects an image from their gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                Bitmap bitmap = galleryUtil.getBitmapFromIntent(data);

                // Something went wrong
                if (bitmap == null) { return; }

                // Navigate to AddWallFragment
                galleryModel.setImgBitmap(bitmap);
                NavHostFragment.findNavController(ManageWallsFragment.this)
                        .navigate(R.id.nav_addWall);
            }
        }
    }

    //TODO: handle case when user doesn't accept permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /*---------------------------Handle Connecting to New Wall------------------------------------*/

    private void onClickConnectToWall(View view) {
        // Create AlertDialog and show.
        final AlertDialog alertDialog = AlertDialogManager.createConnectToWallDialog(context);
        alertDialog.show();

        // Get views
        Button connectToWall = alertDialog.findViewById(R.id.connectToWall);
        EditText wallIdText = alertDialog.findViewById(R.id.wallId);
        Button cancel = alertDialog.findViewById(R.id.cancel);
        assert (connectToWall != null) && (wallIdText != null) && (cancel != null);

        // Let user cancel dialog
        cancel.setOnClickListener(v -> alertDialog.cancel());

        // Connect to wall
        connectToWall.setOnClickListener(v -> {
            // Handle errors
            if (checkForWallIdError(wallIdText)) return;

            // Close the alert dialog
            alertDialog.cancel();

            // Try to download the wall
            String wallId = wallIdText.getText().toString();
            model.downloadWall(wallId).observe(activity, item -> {
                if (item == null || item == SharedViewModel.Status.LOADING) { return; }
                // Display messages to notify is download was successful or not
                displayMessage(item, activity.getApplicationContext());

                // Update list of walls
                if (item == SharedViewModel.Status.SUCCESS) {
                    WallMetadata metadata = model.getWall_metadata(wallId);
                    if (metadata != null) {
                        adapter.add(metadata);
                    }
                }
            });
        });
    }

    private boolean checkForWallIdError(EditText editText) {
        String wallId = editText.getText().toString();
        if (wallId.length() != 6) {
            editText.setError(MESSAGE.WALL_ID_LENGTH);
            return true;
        }
        if (model.hasWall(wallId)) {
            editText.setError(MESSAGE.ALREADY_SAVED);
            return true;
        }
        return false;
    }

    private void displayMessage(SharedViewModel.Status status, Context context) {
        String message = null;
        switch (status) {
            case SUCCESS:
                message = MESSAGE.DOWNLOAD_SUCCESS;
                break;
            case NOT_FOUND:
                message = MESSAGE.DOWNLOAD_NOT_FOUND;
                break;
            case FAILURE:
                message = MESSAGE.DOWNLOAD_FAILURE;
        }
        if (message != null && context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /*-----------------------------------Handle List Of Walls-------------------------------------*/

    private ArrayList<WallMetadata> getWallMetadata() {
        ArrayList<WallMetadata> wall_info = new ArrayList<>();
        String default_id = model.getDefault_id();
        Set<String> wall_ids = model.getWall_ids();
        assert wall_ids != null;
        // Add default wall id first so it's at the start of the list
        if (default_id != null) {
            wall_info.add(model.getWall_metadata(default_id));
        }
        // Add metadata for the other walls
        for (String id : wall_ids) {
            if (id.equals(default_id)) continue;
            wall_info.add(model.getWall_metadata(id));
        }
        return wall_info;
    }

    private void inflateWallList() {
        // Initialize adapter and its callback functions
        ArrayList<WallMetadata> wall_info = getWallMetadata();
        adapter = new MySimpleArrayAdapter(getContext(), wall_info);
        adapter.setCurrentId(currentWallId);
        adapter.setOnDelete(this::onDeleteWall);
        adapter.setOnDeletePermanently(this::onDeleteWallPermanently);
        adapter.setOnEdit(this::onSaveWallEdits);
        listView.setAdapter(adapter);

        // Allow user to change current wall by clicking on list item
        listView.setOnItemClickListener(this::onClickListItem);
    }

    private void onClickListItem(AdapterView<?> parent, View view, int position, long id) {
        WallMetadata info = adapter.getItem(position);
        if (info == null) { return; }
        // This wall is already selected
        if (info.getWall_id().equals(currentWallId)) { return; }

        // Make sure to close previously selected view if it exists
        if ((currentSelectedItem != null) && (currentSelectedItem != view)) {
            currentSelectedItem.findViewById(R.id.goToWallButton).setVisibility(View.GONE);
        }
        currentSelectedItem = view;

        // Animate option to go to wall
        ImageView goToWall = view.findViewById(R.id.goToWallButton);
        goToWall.setVisibility((goToWall.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
        goToWall.setOnClickListener(v -> onUpdateWall(info));
    }

    /*--------------------------------Callbacks for list items------------------------------------*/

    private void onUpdateWall(WallMetadata item) {
        // Update current wall
        model.setCurrentWall(item.getWall_id());
        // Navigate to home fragment
        NavHostFragment.findNavController(ManageWallsFragment.this)
                .navigate(R.id.nav_home);
    }

    private void onDeleteWall(WallMetadata item) {
        model.deleteWallLocal(item.getWall_id());
    }

    private void onDeleteWallPermanently(WallMetadata item) {
        model.deleteWallPermanent(item.getWall_id()).observe(activity, result -> {
            if (result != SharedViewModel.Status.SUCCESS) { return; }
            Toast.makeText(context.getApplicationContext(),
                    "Deleted " + item.getWall_name(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void onSaveWallEdits(WallMetadata item, boolean isDefault) {
        // Update wall name
        model.updateWall(item);

        // Update default wall
        if (isDefault) {
            model.setDefault_id(item.getWall_id());
        }
    }

    /*-----------------------------------------Clean Up-------------------------------------------*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        listView = null;
    }
}
