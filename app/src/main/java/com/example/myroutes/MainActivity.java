package com.example.myroutes;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import org.opencv.android.OpenCVLoader;

import com.example.myroutes.db.SharedViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // View model
    private SharedViewModel model;

    // Bottom navigation controller
    private NavController navController;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.e(TAG, "OpenCV Initialization Error!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation(findViewById(R.id.nav_view));

        // Don't reset app if it has a saved instance state
        if (savedInstanceState != null) { return; }

        // Initialize view model for handling wall data
        model = new ViewModelProvider(this).get(SharedViewModel.class);

        // Initialize username if first time logging in
        String username = model.getUsername();
        if (username == null) {
            username = UUID.randomUUID().toString().substring(0,8);
            model.setUsername(username);
        }

        // Try to log user in with their username
        model.setStitchUser(username).observe(this, user -> {
            if (user == null) return;

            // User has not added any walls so navigate them to fragment to add one
            if (model.getDefault_id() == null) {
                navController.navigate(R.id.nav_manage_walls);
                return;
            }
            // Try to load default wall
            model.setCurrentWall(model.getDefault_id());
        });
    }

    private void setupNavigation(BottomNavigationView navigationView) {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        /*navigationView.setOnNavigationItemSelectedListener(item -> {
            currentFragmentId.setValue(item.getItemId());
            switch (item.getItemId()) {
                case R.id.nav_home:
                    navController.navigate(R.id.nav_home);
                    return true;
                case R.id.nav_addBoulder:
                    Log.e(TAG, item.getTitle().toString());
                    navController.navigate(R.id.nav_addBoulder);
                    return true;
                case R.id.nav_manage_walls:
                    navController.navigate(R.id.nav_manage_walls);
                    return true;
            }
            return false;
        });*/

        //NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        navigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }
}
