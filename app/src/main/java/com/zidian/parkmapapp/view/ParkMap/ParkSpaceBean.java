package com.zidian.parkmapapp.view.ParkMap;

public class ParkSpaceBean {
    private int id; //车位id
    private Coordinate ltCoordinateScale;   //车位左上坐标比例：0 ~ 1.0f
    private Coordinate rbCoordinateScale;   //车位右下坐标比例
    private Coordinate ltCoordinate;   //车位左上坐标 px
    private Coordinate rbCoordinate;   //车位右下坐标



    public ParkSpaceBean(int id, float ltxS, float ltyS, float rbxS , float rbyS) {
        this.id = id;
        this.ltCoordinateScale = new Coordinate(ltxS, ltyS);
        this.rbCoordinateScale = new Coordinate(rbxS, rbyS);
        this.ltCoordinate = new Coordinate(0, 0);
        this.rbCoordinate = new Coordinate(0, 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Coordinate getLtCoordinateScale() {
        return ltCoordinateScale;
    }

    public void setLtCoordinateScale(Coordinate ltCoordinateScale) {
        this.ltCoordinateScale = ltCoordinateScale;
    }

    public Coordinate getRbCoordinateScale() {
        return rbCoordinateScale;
    }

    public void setRbCoordinateScale(Coordinate rbCoordinateScale) {
        this.rbCoordinateScale = rbCoordinateScale;
    }

    public Coordinate getLtCoordinate() {
        return ltCoordinate;
    }

    public void setLtCoordinate(Coordinate ltCoordinate) {
        this.ltCoordinate = ltCoordinate;
    }

    public Coordinate getRbCoordinate() {
        return rbCoordinate;
    }

    public void setRbCoordinate(Coordinate rbCoordinate) {
        this.rbCoordinate = rbCoordinate;
    }
}
