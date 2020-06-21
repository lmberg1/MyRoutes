package com.example.myroutes.ui.addBoulder;

import androidx.lifecycle.ViewModel;

import com.example.myroutes.db.entities.BoulderItem;

import java.util.ArrayList;

public class AddBoulderModel extends ViewModel {

    // The indices of highlighted holds
    private ArrayList<Integer> highlightedHolds;
    // The boulder to edit (only set if we are in edit mode)
    private BoulderItem boulder;

    public AddBoulderModel() {
        highlightedHolds = new ArrayList<>();
    }

    public void setBoulder(BoulderItem boulder) {
        this.boulder = boulder;
    }

    public BoulderItem getBoulder() {
        return boulder;
    }

    public void setHighlightedHolds(ArrayList<Integer> highlightedHolds) {
        this.highlightedHolds = highlightedHolds;
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