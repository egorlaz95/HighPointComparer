package com.example.highpointcomparer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class CustomElevationInfoWindow extends InfoWindow {
    private Context mContext;
    private double elevation;

    public CustomElevationInfoWindow(Context context, int layoutResId, MapView mapView, double elevation) {
        super(layoutResId, mapView);
        this.mContext = context;
        this.elevation = elevation;
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onOpen(Object item) {
        if (mView != null) {
            TextView elevationTextView = mView.findViewById(R.id.textViewElevation);
            if (elevationTextView != null) {
                String elevationText = "Высота: " + elevation + " м";
                elevationTextView.setText(elevationText);
            }
        }
    }
}
