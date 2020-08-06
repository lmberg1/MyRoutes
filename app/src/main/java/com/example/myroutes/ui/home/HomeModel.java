package com.example.myroutes.ui.home;

import androidx.lifecycle.ViewModel;

import com.example.myroutes.SharedViewModel;

public class HomeModel extends ViewModel {

    private String grade;
    private int boulderIdx;

    public HomeModel() {
        grade = SharedViewModel.BOULDER_GRADES[0];
        boulderIdx = 0;
    }

    public String getGrade() {
        return grade;
    }

    public int getBoulderIdx() {
        return boulderIdx;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setBoulderIdx(int boulderIdx) {
        this.boulderIdx = boulderIdx;
    }
}