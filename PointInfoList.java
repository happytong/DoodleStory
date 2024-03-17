package com.tongs.doodlestory;

import java.util.ArrayList;

public class PointInfoList {
    private ArrayList<PointInfo> pointArrayList;

    public PointInfoList() {
        pointArrayList = new ArrayList<>();
    }

    // Add a point to the list
    public void addPoint(int position, int color, int width) {
        PointInfo point = new PointInfo(position, color, width);
        pointArrayList.add(point);
    }

    // Reset (clear) the entire list
    public void resetPoints() {
        pointArrayList.clear();
    }

    // Get the size of the list
    public int getSize() {
        return pointArrayList.size();
    }

    // Get a point at a specific index
    public PointInfo getPoint(int index) {
        if (index >= 0 && index < pointArrayList.size()) {
            return pointArrayList.get(index);
        }
        return null;
    }
}
