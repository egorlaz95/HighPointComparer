package com.example.highpointcomparer;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.osmdroid.views.overlay.Marker;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {
    private static final String OPEN_ELEVATION_API_BASE_URL = "https://api.open-elevation.com/api/v1/";
    private EditText editTextCity;
    private Button buttonSearch;
    private Button buttonComparer;
    private MapView mapView;
    private LinearLayout topLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);
        topLayout = findViewById(R.id.topLayout);
        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonComparer = findViewById(R.id.buttonComparer);
        MapView mapView = findViewById(R.id.mapView);
        mapView.setScrollableAreaLimitDouble(new BoundingBox(85, 180, -85, -180));
        mapView.setMaxZoomLevel(20.0);
        mapView.setMinZoomLevel(4.0);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setScrollableAreaLimitLatitude(MapView.getTileSystem().getMaxLatitude(), MapView.getTileSystem().getMinLatitude(), 0);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cityName = editTextCity.getText().toString();
                performCitySearch(cityName, mapView);
            }
        });
        buttonComparer.setOnClickListener(new View.OnClickListener() {
            boolean isFirstCitySelected = false;
            private Address city1Location;
            private Address city2Location;
            boolean isMarker = true;

            @Override
            public void onClick(View view) {
                String cityName = editTextCity.getText().toString();
                if (!isFirstCitySelected) {
                    city1Location = getAdress(cityName, mapView);
                    isFirstCitySelected = true;
                    editTextCity.setText("");
                } else {
                    city2Location = getAdress(cityName, mapView);
                    isFirstCitySelected = false;
                    editTextCity.setText("");
                    isMarker = false;
                }
                compareTwoLocations(city1Location, city2Location);
                city2Location = null;
                if (!isMarker){
                    mapView.getOverlays().clear();
                    isMarker = true;
                }
            }
        });
    }

    private void performCitySearch(String cityName, MapView mapView) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                Log.d("latitude", String.valueOf(latitude));
                Log.d("longitude", String.valueOf(longitude));
                mapView.getOverlays().clear();
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(latitude, longitude));
                mapView.getOverlays().add(marker);
                mapView.getController().setCenter(new GeoPoint(latitude, longitude));
                mapView.invalidate();
                String locations = String.valueOf(latitude) + " , " + String.valueOf(longitude);
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(OPEN_ELEVATION_API_BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                ElevationApi elevationApi = retrofit.create(ElevationApi.class);
                Call<ElevationResponse> call = elevationApi.getElevation(locations, "json");
                call.enqueue(new Callback<ElevationResponse>() {
                    @Override
                    public void onResponse(Call<ElevationResponse> call, Response<ElevationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ElevationResponse elevationResponse = response.body();
                            double elevation = elevationResponse.getResults()[0].getElevation();
                            CustomElevationInfoWindow elevationInfoWindow = new CustomElevationInfoWindow(R.layout.custom_info_window, mapView, elevation);
                            marker.setInfoWindow(elevationInfoWindow);
                            marker.showInfoWindow();
                        } else {
                            Log.e("Elevation", "Failed to retrieve elevation data.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ElevationResponse> call, Throwable t) {
                        Log.e("Elevation", "Error: " + t.getMessage());
                    }
                });
            } else {
                Toast.makeText(this, "Город не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Address getAdress(String cityName, MapView mapView) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        Address address = null;
        try {
            addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(latitude, longitude));
                mapView.getOverlays().add(marker);
                mapView.getController().setCenter(new GeoPoint(latitude, longitude));
                mapView.invalidate();
            } else {
                Toast.makeText(this, "Город не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }

    private void compareTwoLocations(Address city1Location, Address city2Location) {
        if (city1Location != null && city2Location != null) {
            String location1 = String.valueOf(city1Location.getLatitude()) + " , " + String.valueOf(city1Location.getLongitude());
            String location2 = String.valueOf(city2Location.getLatitude()) + " , " + String.valueOf(city2Location.getLongitude());
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(OPEN_ELEVATION_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            ElevationApi elevationApi = retrofit.create(ElevationApi.class);
            Call<ElevationResponse> call1 = elevationApi.getElevation(location1, "json");
            Call<ElevationResponse> call2 = elevationApi.getElevation(location2, "json");
            call1.enqueue(new Callback<ElevationResponse>() {
                @Override
                public void onResponse(Call<ElevationResponse> call, Response<ElevationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        double elevation1 = response.body().getResults()[0].getElevation();
                        call2.enqueue(new Callback<ElevationResponse>() {
                            @Override
                            public void onResponse(Call<ElevationResponse> call, Response<ElevationResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    double elevation2 = response.body().getResults()[0].getElevation();
                                    double elevationDifference = elevation2 - elevation1;
                                    Toast.makeText(getApplicationContext(),
                                            "Разница в высоте: " + elevationDifference + " м",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("Elevation", "Failed to retrieve elevation data for city 2.");
                                }
                            }

                            @Override
                            public void onFailure(Call<ElevationResponse> call, Throwable t) {
                                Log.e("Elevation", "Error: " + t.getMessage());
                            }
                        });
                    } else {
                        Log.e("Elevation", "Failed to retrieve elevation data for city 1.");
                    }
                }
                @Override
                public void onFailure(Call<ElevationResponse> call, Throwable t) {
                    Log.e("Elevation", "Error: " + t.getMessage());
                }
            });
        } else {
            Toast.makeText(this, "Выберите два города для сравнения.", Toast.LENGTH_SHORT).show();
        }
    }
}