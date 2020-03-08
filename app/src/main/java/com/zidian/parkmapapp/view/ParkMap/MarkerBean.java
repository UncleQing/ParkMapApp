package com.zidian.parkmapapp.view.ParkMap;

public class MarkerBean {
    public static final int TYPE_CAR = 1;
    public static final int TYPE_Other = 2;

    private Coordinate coordinate; //相对于地图位置0f~1.0f
    private Coordinate canvas_coordinate; //画布坐标 单位px
    private float rotation;
    private int id;
    private int type;
    private float width;
    private float height;
    private float rippleWidth;
    private float rippleHeight;

    public Coordinate getCanvas_coordinate() {
        return canvas_coordinate;
    }

    public void setCanvas_coordinate(Coordinate canvas_coordinate) {
        this.canvas_coordinate = canvas_coordinate;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getRippleWidth() {
        return rippleWidth;
    }

    public void setRippleWidth(float rippleWidth) {
        this.rippleWidth = rippleWidth;
    }

    public float getRippleHeight() {
        return rippleHeight;
    }

    public void setRippleHeight(float rippleHeight) {
        this.rippleHeight = rippleHeight;
    }
}
