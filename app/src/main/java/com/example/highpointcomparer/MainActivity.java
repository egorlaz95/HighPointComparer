package com.example.highpointcomparer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {
    private static final String OPEN_ELEVATION_API_BASE_URL = "https://api.open-elevation.com/api/v1/";
    private EditText editTextCity;
    public Button buttonSearch;
    public Button buttonComparer;
    public LinearLayout topLayout;
//    Handler handler = new Handler();
    private CustomElevationInfoWindow elevationInfoWindow;
    private boolean isMark;
    private DatabaseHelper dbHelper;
    private Map<String, Double> historyItems = new HashMap<>();
    public MapView mapView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        topLayout = findViewById(R.id.topLayout);
        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonComparer = findViewById(R.id.buttonComparer);
        Button buttonShowHistory = findViewById(R.id.buttonShowHistory);
        mapView = findViewById(R.id.mapView);
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
            boolean heightInfo = true;
            @Override
            public void onClick(View view) {
                String cityName = editTextCity.getText().toString();
                performCitySearch(cityName, mapView, heightInfo);
            }
        });
        buttonComparer.setOnClickListener(new View.OnClickListener() {
            boolean isFirstCitySelected = false;
            private Address city1Location;
            private Address city2Location;
            boolean isMarker = true;
            @Override
            public void onClick(View view) {
                if (elevationInfoWindow != null) {
                    elevationInfoWindow.close();
                }
                if (isMark){
                    mapView.getOverlays().clear();
                    isMark = false;
                }
                String cityName = editTextCity.getText().toString();
                if (!isFirstCitySelected) {
                    city1Location = getAdress(cityName, mapView);
                    isFirstCitySelected = true;
                    editTextCity.setText("");
                } else {
                    city2Location = getAdress(cityName, mapView);
                    isFirstCitySelected = false;
                    editTextCity.setText("");
                    isMark = true;
                }
                compareTwoLocations(city1Location, city2Location);
                city2Location = null;
                if (!isMarker){
                    isMarker = true;
                }
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mapView.getOverlays().clear();
//                    }
//                }, 20000);
            }
        });
        buttonShowHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<CityElevation> historyItemsList = new ArrayList<>();
                for (Map.Entry<String, Double> entry : historyItems.entrySet()) {
                    historyItemsList.add(new CityElevation(entry.getKey(), entry.getValue()));
                }
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                intent.putExtra("historyItems", historyItemsList);
                startActivity(intent);
            }
        });
    }


    private void performCitySearch(String cityName, MapView mapView, boolean heightInfo) {
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
                if (elevationInfoWindow != null) {
                    elevationInfoWindow.close();
                }
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
                            historyItems.put(cityName, elevation);
                            elevationInfoWindow = new CustomElevationInfoWindow(R.layout.custom_info_window, mapView, elevation);
                            marker.setInfoWindow(elevationInfoWindow);
                            marker.showInfoWindow();
                            isMark = true;
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
            GeoPoint point1 = convertAddressToGeoPoint(city1Location);
            GeoPoint point2 = convertAddressToGeoPoint(city2Location);
            drawGreenLine(point1, point2);
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
    private void saveSearchHistory(String cityName, double elevation) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_CITY_NAME, cityName);
        values.put(DatabaseHelper.COLUMN_ELEVATION, elevation);
        db.insert(DatabaseHelper.TABLE_SEARCH_HISTORY, null, values);
        db.close();
        Toast.makeText(this, "Search history saved", Toast.LENGTH_SHORT).show();
        historyItems.put(cityName, elevation);
    }
    private GeoPoint convertAddressToGeoPoint(Address address) {
        double latitude = address.getLatitude();
        double longitude = address.getLongitude();
        return new GeoPoint(latitude, longitude);
    }
    private void drawGreenLine(GeoPoint startPoint, GeoPoint endPoint) {
        Polyline line = new Polyline();
        line.setColor(Color.GREEN);
        line.setWidth(5);
        line.setPoints(Arrays.asList(startPoint, endPoint));
        mapView.getOverlayManager().add(line);
        mapView.invalidate();
    }

}