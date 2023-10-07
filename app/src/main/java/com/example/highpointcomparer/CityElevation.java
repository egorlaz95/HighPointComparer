package com.example.highpointcomparer;

import java.io.Serializable;

public class CityElevation implements Serializable {
    private String cityName;
    private double elevation;

    public CityElevation(String cityName, double elevation) {
        this.cityName = cityName;
        this.elevation = elevation;
    }

    public String getCityName() {
        return cityName;
    }

    public double getElevation() {
        return elevation;
    }
}
