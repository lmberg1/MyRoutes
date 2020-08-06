package com.example.myroutes.ui.addBoulder;

import androidx.lifecycle.ViewModel;

import com.example.myroutes.db.entities.BoulderItem;

import java.util.ArrayList;
import java.util.List;

public class AddBoulderModel extends ViewModel {

    // The indices of highlighted holds
    private List<Integer> highlightedHolds;
    // The start hold/holds
    private List<Integer> startHolds;
    // The finish hold
    private int finishHold = -1;
    // The boulder to edit (only set if we are in edit mode)
    private BoulderItem boulder;

    public AddBoulderModel() {
        highlightedHolds = new ArrayList<>();
        startHolds = new ArrayList<>();
    }

    public void setBoulder(BoulderItem boulder) {
        this.boulder = boulder;
    }

    public BoulderItem getBoulder() {
        return boulder;
    }

    /*--------------------------------------Start Holds-------------------------------------------*/

    public void setStartHolds(List<Integer> startHolds) {
        this.startHolds = startHolds;
    }

    public List<Integer> getStartHolds() {
        return startHolds;
    }

    public void addStartHold(int i) { startHolds.add(i); }

    public void removeStartHold(int i) {
        startHolds.remove((Integer) i);
    }

    public void removeFirstStartHold() {
        if (startHolds.size() == 0) return;
        startHolds.remove(0);
    }

    public boolean hasStartHolds() {
        return startHolds.size() != 0;
    }

    /*--------------------------------------Finish Hold-------------------------------------------*/

    public boolean hasFinishHold() {
        return finishHold != -1;
    }

    public void clearFinishHold() {
        finishHold = -1;
    }

    public void setFinishHold(int finishHold) {
        this.finishHold = finishHold;
    }

    public int getFinishHold() {
        return finishHold;
    }

    /*----------------------------------Highlighted Holds-----------------------------------------*/

    public void setHighlightedHolds(List<Integer> highlightedHolds) {
        this.highlightedHolds = highlightedHolds;
    }

    public List<Integer> getHighlightedHolds() {
        return highlightedHolds;
    }

    public void addHighlightedHold(int i) {
        highlightedHolds.add(i);
    }

    public void removeHighlightedHold(int i) {
        highlightedHolds.remove((Integer) i);
        startHolds.remove((Integer) i);
        if (finishHold == i) finishHold = -1;
    }

    public boolean hasHighlighedHold(int i) {
        return highlightedHolds.contains(i);
    }

    public void clearHighlightedHolds() {
        highlightedHolds.clear();
        startHolds.clear();
        clearFinishHold();
    }
}