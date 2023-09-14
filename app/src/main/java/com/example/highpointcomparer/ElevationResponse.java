package com.example.highpointcomparer;

import com.google.gson.annotations.SerializedName;

public class ElevationResponse {
    @SerializedName("results")
    private ElevationResult[] results;

    public ElevationResult[] getResults() {
        return results;
    }

    public static class ElevationResult {
        @SerializedName("elevation")
        private double elevation;

        public double getElevation() {
            return elevation;
        }
    }
}
