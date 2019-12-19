package com.zidian.parkmapapp.view;


public class Marker {
    public static final int MARK_TYPE_CAR = 1;
    public static final int MARK_TYPE_OHTER = 2;

    private float percentX;
    private float percentY;
    private int drawX;
    private int drawY;
    private int width;
    private int height;
    private int imgRes;
    private int markerType;

    public Marker() {

    }

    public int getMarkerType() {
        return markerType;
    }

    public int getDrawX() {
        return drawX;
    }

    public void setDrawX(int drawX) {
        this.drawX = drawX;
    }

    public int getDrawY() {
        return drawY;
    }

    public void setDrawY(int drawY) {
        this.drawY = drawY;
    }

    public void setMarkerType(int markerType) {
        this.markerType = markerType;
    }

    public float getPercentX() {
        return percentX;
    }

    public void setPercentX(float percentX) {
        this.percentX = percentX;
    }

    public float getPercentY() {
        return percentY;
    }

    public void setPercentY(float percentY) {
        this.percentY = percentY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getImgRes() {
        return imgRes;
    }

    public void setImgRes(int imgRes) {
        this.imgRes = imgRes;
    }
}
