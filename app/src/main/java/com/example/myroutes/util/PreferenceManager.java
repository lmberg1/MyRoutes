package com.example.myroutes.util;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PreferenceManager {
    private static final String TAG = "PreferenceManager";
    public enum Role {OWNER, NON_OWNER};

    public static String getUser_id(SharedPreferences preferences) {
        return preferences.getString(Fields.USER_ID, null);
    }

    public static String getDefault_wall(SharedPreferences preferences) {
        return preferences.getString(Fields.DEFAULT_WALL, null);
    }

    public static Set<String> getWall_ids(SharedPreferences preferences) {
        return preferences.getStringSet(Fields.WALL_IDS, new HashSet<>());
    }

    public static WallMetadata getWall_metadata(String wall_id, SharedPreferences preferences) {
        Set<String> data = preferences.getStringSet(wall_id, null);
        if (data == null) { return null; }

        // Create a new metadata object
        WallMetadata wallMetadata = new WallMetadata();
        wallMetadata.setId(wall_id);
        for (String s : data) {
            if (s.equals(Fields.OWNER)) { wallMetadata.setRole(Role.OWNER); }
            else if (s.equals(Fields.NON_OWNER)) { wallMetadata.setRole(Role.NON_OWNER); }
            else { wallMetadata.setWall_name(s); }
        }
        return wallMetadata;
    }

    public static void setUser_id(String user_id, SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Fields.USER_ID, user_id);
        editor.apply();
    }

    public static void setDefault_wall(String default_wall, SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Fields.DEFAULT_WALL, default_wall);
        editor.apply();
    }

    public static void setWall_metadata(WallMetadata wallInfo, SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        String roleStr = (wallInfo.getRole() == Role.OWNER) ? Fields.OWNER : Fields.NON_OWNER;
        editor.putStringSet(wallInfo.getWall_id(), new HashSet<>(Arrays.asList(wallInfo.getWall_name(), roleStr)));
        editor.apply();
    }

    public static void addWall_id(WallMetadata wallInfo, SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        // Update default wall if necessary
        Set<String> wall_ids = getWall_ids(preferences);
        if (wall_ids.isEmpty()) {
            editor.putString(Fields.DEFAULT_WALL, wallInfo.getWall_id());
        }
        // Update list of wall ids user has access to
        wall_ids.add(wallInfo.getWall_id());
        editor.putStringSet(Fields.WALL_IDS, wall_ids);
        // Update metadata associated with this wall id
        String roleStr = (wallInfo.getRole() == Role.OWNER) ? Fields.OWNER : Fields.NON_OWNER;
        editor.putStringSet(wallInfo.getWall_id(), new HashSet<>(Arrays.asList(wallInfo.getWall_name(), roleStr)));
        Log.e(TAG, wallInfo.getWall_name());
        editor.apply();
    }

    public static void removeWall_id(String wall_id, SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> wall_ids = getWall_ids(preferences);
        // No walls to remove
        if (wall_ids.isEmpty()) return;
        // Update default wall if necessary
        if (getDefault_wall(preferences).equals(wall_id)) {
            if (wall_ids.size() > 0) {
                editor.putString(Fields.DEFAULT_WALL, wall_ids.iterator().next());
            }
            else {
                editor.remove(Fields.DEFAULT_WALL);
            }
        }
        // Update set of available wall ids
        wall_ids.remove(wall_id);
        if (wall_ids.isEmpty()) { editor.remove(Fields.WALL_IDS); }
        else { editor.putStringSet(Fields.WALL_IDS, wall_ids); }
        // Remove wall metadata
        editor.remove(wall_id);
        editor.apply();
    }

    public static void clear(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    static final class Fields {
        static final String USER_ID = "user_id";
        static final String DEFAULT_WALL = "default_wall";
        static final String WALL_IDS = "wall_ids";
        static final String OWNER = "owner";
        static final String NON_OWNER = "non-owner";
    }

}
