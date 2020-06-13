package com.example.myroutes.ui.addBoulder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class AddBoulderModel extends ViewModel {

    private ArrayList<Integer> highlightedHolds;

    public AddBoulderModel() {
        highlightedHolds = new ArrayList<>();
    }

    public ArrayList<Integer> getHighlightedHolds() {
        return highlightedHolds;
    }

    public void addHighlightedHold(int i) {
        highlightedHolds.add(i);
    }

    public void removeHighlightedHold(int i) {
        highlightedHolds.remove((Integer) i);
    }

    public void clearHighlightedHolds() { highlightedHolds = new ArrayList<>(); }

    public boolean hasHighlighedHold(int i) {
        return highlightedHolds.contains(i);
    }
}