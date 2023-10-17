package com.example.highpointcomparer;

import android.location.Address;

public interface GeocodeCallback {
    void onGeocodeSuccess(Address address);
    void onGeocodeFailure(String errorMessage);
}
