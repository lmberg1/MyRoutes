package com.example.myroutes.ui.workouts;

import androidx.lifecycle.ViewModel;

import com.example.myroutes.db.SharedViewModel;

public class WorkoutModel extends ViewModel {

    private String workout_id;
    private String wall_id;
    private int setIdx;
    private int boulderIdx;

    public WorkoutModel() {
        boulderIdx = 0;
        setIdx = 0;
    }

    public String getWorkout_id() {
        return workout_id;
    }

    public String getWall_id() {
        return wall_id;
    }

    public int getSetIdx() {
        return setIdx;
    }

    public int getBoulderIdx() {
        return boulderIdx;
    }

    public void setWorkout_id(String workout_id) {
        this.workout_id = workout_id;
    }

    public void setWall_id(String wall_id) {
        this.wall_id = wall_id;
    }

    public void setSetIdx(int setIdx) {
        this.setIdx = setIdx;
    }

    public void setBoulderIdx(int boulderIdx) {
        this.boulderIdx = boulderIdx;
    }
}