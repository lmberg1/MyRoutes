package com.example.myroutes.util;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.myroutes.SharedViewModel.Status;

import java.util.HashMap;

public class WallLoadingErrorHandler {
    private static final String TAG = "WallLoadingErrorHandler";

    private static HashMap<Status, String> errorTable = setHashMap();

    private static HashMap<Status, String> setHashMap() {
        HashMap<Status, String> errorTable = new HashMap<>();
        errorTable.put(Status.NOT_FOUND, "Oh no. The wall you tried to go to no longer exists.");
        errorTable.put(Status.FAILURE, "Oh no. Something went wrong. Make sure you are connected to the internet and try again.");
        errorTable.put(null, "You have no walls. Go to the Manage Walls panel to add one.");
        return errorTable;
    }


    public static boolean handleError(Status result, final ProgressBar progressBar, final TextView messageText) {
        if (result == Status.SUCCESS) {
            progressBar.setVisibility(View.GONE);
            messageText.setVisibility(View.GONE);
            return true;
        }
        else if (result == Status.LOADING) {
            progressBar.setVisibility(View.VISIBLE);
            messageText.setVisibility(View.GONE);
        }
        else {
            String error = errorTable.get(result);
            progressBar.setVisibility(View.GONE);
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(error);
        }
        return false;
    }

}
