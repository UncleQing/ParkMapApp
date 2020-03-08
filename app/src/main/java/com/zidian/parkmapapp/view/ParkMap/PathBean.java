package com.zidian.parkmapapp.view.ParkMap;

public class PathBean {
    private Coordinate coordinate; //相对于地图位置0f~1.0f
    private Coordinate canvas_coordinate; //画布坐标
    private int index;

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public Coordinate getCanvas_coordinate() {
        return canvas_coordinate;
    }

    public void setCanvas_coordinate(Coordinate canvas_coordinate) {
        this.canvas_coordinate = canvas_coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
