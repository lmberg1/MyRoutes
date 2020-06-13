package com.example.myroutes.util;

import com.example.myroutes.util.PreferenceManager;

public class WallMetadata {
    private String name;
    private String id;
    private PreferenceManager.Role role;

    public WallMetadata() {
        name = null; id = null; role = null;
    }

    public WallMetadata(String wall_id, String wall_name, PreferenceManager.Role role) {
        this.name = wall_name;
        this.id = wall_id;
        this.role = role;
    }

    public String getWall_id() {
        return id;
    }

    public String getWall_name() {
        return name;
    }

    public PreferenceManager.Role getRole() {
        return role;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setWall_name(String name) {
        this.name = name;
    }

    public void setRole(PreferenceManager.Role role) {
        this.role = role;
    }
}
