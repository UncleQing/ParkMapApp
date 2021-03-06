package com.zidian.parkmapapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zidian.parkmapapp.view.ParkMap.ParkMapView;

public class MainActivity extends AppCompatActivity {

    private ParkMapView parkMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parkMapView = findViewById(R.id.pmv);

        testMap();

    }


    private void testMap() {
        parkMapView.testMarker();
        parkMapView.setPaths(null);
        parkMapView.updateLocalPosition(-1, -1, -1);
        parkMapView.setSpaceClickListner(new ParkMapView.OnSpaceClickListenner() {
            @Override
            public void onClick(int id) {

            }
        });
        parkMapView.showObstacle();
        parkMapView.hideObstacle();

    }
}
