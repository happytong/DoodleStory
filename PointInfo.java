package com.tongs.doodlestory;

public class PointInfo {
    private int index;
    private int color;
    private int width;

    public PointInfo(int index, int color, int width) {
        this.index = index;
        this.color = color;
        this.width = width;
    }

    public int getIndex() {
        return index;
    }

    public void setPosition(int index) {
        this.index = index;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
