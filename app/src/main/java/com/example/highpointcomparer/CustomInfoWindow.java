package com.example.highpointcomparer;

import android.view.View;
import android.widget.TextView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class CustomInfoWindow extends InfoWindow {
    private TextView elevationValue;

    public CustomInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
        elevationValue = mView.findViewById(R.id.elevationValue);
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onOpen(Object item) {
    }

    public void setElevation(String elevationText) {
        if (elevationValue != null) {
            elevationValue.setText(elevationText);
        }
    }
}
