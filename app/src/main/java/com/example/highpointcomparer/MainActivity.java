package com.example.highpointcomparer;

import static android.app.PendingIntent.getActivity;
import static android.app.PendingIntent.readPendingIntentOrNullFromParcel;

import android.app.Dialog;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
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
import java.util.Locale;
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
    private CustomElevationInfoWindow elevationInfoWindow;
    private boolean isMark;
    private DatabaseHelper dbHelper;
    private Map<String, Double> historyItems = new HashMap<>();
    public MapView mapView;
    public Button buttonSelectLocation;
    private EditText editTextLocation;
    private Marker previousMarker = null;
    private Marker morePreviousMarker = null;
    public Address addressToCompare;
    public Button buttonShowHistory;
    public Button buttonSearcher;
    public String firstCityName;
    public String secondCityName;
    public boolean isFirstCitySelected = false;
    public Address city1Location;
    public Address city2Location;
    public String cityName;
    private Polyline line;
    public Address firstAddressToCompare;
    public Address secondAddressToCompare;
    public GeoPoint startPoint;
    public GeoPoint endPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        topLayout = findViewById(R.id.topLayout);
        editTextCity = findViewById(R.id.editTextCity);
        editTextLocation = findViewById(R.id.editTextLocation);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonComparer = findViewById(R.id.buttonComparer);
        buttonSelectLocation = findViewById(R.id.buttonSelectLocation);
        buttonShowHistory = findViewById(R.id.buttonShowHistory);
        buttonSearcher = findViewById(R.id.buttonSearcher);
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
        editTextCity.setVisibility(View.VISIBLE);
        buttonSearch.setVisibility(View.VISIBLE);
        editTextLocation.setVisibility(View.GONE);
        buttonSelectLocation.setVisibility(View.GONE);
        buttonComparer.setVisibility(View.VISIBLE);
        buttonSearcher.setVisibility(View.GONE);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)  {
                String cityName = editTextCity.getText().toString();
                performCitySearch(cityName);
                editTextCity.setText("");
                markerByTap();
            }
        });
        buttonComparer.setOnClickListener(new View.OnClickListener() {
            boolean isFirstCitySelected = false;
            private Address city1Location;
            private Address city2Location;
            boolean isMarker = true;
            public String firstCity;
            public String secondCity;
            private int markerCount;
            @Override
            public void onClick(View view) {
                buttonSearcher.setVisibility(View.VISIBLE);
                buttonComparer.setVisibility(View.GONE);
                if (elevationInfoWindow != null) {
                    elevationInfoWindow.close();
                }
                if (isMark) {
                    mapView.getOverlays().clear();
                    isMark = false;
                }
                updateToCompare();
                showDialogForLocationInput();
            }
        });

        buttonSearcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)  {
                buttonSearcher.setVisibility(View.GONE);
                buttonComparer.setVisibility(View.VISIBLE);
                updateToSearch();
                markerByTap();
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
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            private Marker previousMarker;

            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (previousMarker != null) {
                    mapView.getOverlays().remove(previousMarker);
                }
                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                mapView.getOverlays().add(marker);
                previousMarker = marker;
                Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        performCityTap(address);
                    } else {
                        Log.e("Tap Error", "Not Found");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }


    private void performCitySearch(String cityName) {
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
                previousMarker = marker;
                marker.setPosition(new GeoPoint(latitude, longitude));
                mapView.getOverlays().add(marker);
                mapView.getController().setCenter(new GeoPoint(latitude, longitude));
                mapView.invalidate();
                editTextCity.clearFocus();
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
    public void performCityTap(Address address){
        double latitude = address.getLatitude();
        double longitude = address.getLongitude();
        Log.d("latitude", String.valueOf(latitude));
        Log.d("longitude", String.valueOf(longitude));
        if (elevationInfoWindow != null) {
            elevationInfoWindow.close();
        }
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
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
                    String addressLine = address.getAddressLine(0);
                    historyItems.put(addressLine, elevation);
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
    }

//    public void performCityTapToCompare(Address address){
//        double latitude = address.getLatitude();
//        double longitude = address.getLongitude();
//        if (elevationInfoWindow != null) {
//            elevationInfoWindow.close();
//        }
//        Marker marker = new Marker(mapView);
//        marker.setPosition(new GeoPoint(latitude, longitude));
//        mapView.getController().setCenter(new GeoPoint(latitude, longitude));
//        mapView.invalidate();
//    }

    private Address getAdressForSearch(String cityName, MapView mapView) {
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
                morePreviousMarker = previousMarker;
                previousMarker = marker;
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

    private Address getAdressForTap(String cityName) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        Address address = null;
        try {
            addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0);
            } else {
                Toast.makeText(this, "Город не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }

    private void compareTwoLocations() {
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
                                    double elevationDifference = Math.abs(elevation2 - elevation1);
                                    setLineColor(elevationDifference);
                                    CompareInfoDialog compareInfoDialog = new CompareInfoDialog(elevationDifference);
                                    compareInfoDialog.show(getSupportFragmentManager(), compareInfoDialog.getTag());
                                    String cities = "Разница между " + firstCityName + " и " + secondCityName;
                                    historyItems.put(cities, elevationDifference);
                                    city1Location = null;
                                    city2Location = null;
                                    firstCityName = null;
                                    secondCityName = null;
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
    private void setLine(){
        line = new Polyline();
        line.setPoints(Arrays.asList(startPoint, endPoint));
    }
    private void setLineColor(Double elevationDifference){
        if (elevationDifference < 50) {
            line.setColor(Color.GREEN);
        } else if (elevationDifference < 100) {
            line.setColor(Color.YELLOW);
        } else {
            line.setColor(Color.RED);
        }
        line.setWidth(6);
        mapView.getOverlayManager().add(line);
        mapView.invalidate();
    }
    private void showDialogForLocationInput() {
        markerByTapToCompare(new GeocodeCallback() {
            @Override
            public void onGeocodeSuccess(Address address) {
                cityName = addressToCompare.getAddressLine(0);
                if (!isFirstCitySelected) {
                    firstCityName = cityName;
                    city1Location = addressToCompare;
                    isFirstCitySelected = true;
                    editTextLocation.setText("");
                } else {
                    secondCityName = cityName;
                    city2Location = addressToCompare;
                    isFirstCitySelected = false;
                    editTextLocation.setText("");
                    isMark = true;
                }
                if (startPoint != null && endPoint != null){
                    setLine();
                }
                compareTwoLocations();
            }

            @Override
            public void onGeocodeFailure(String errorMessage) {
                Log.e("Error:", errorMessage);
            }
        });
        buttonSelectLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cityName = editTextLocation.getText().toString();
                if (!isFirstCitySelected) {
                    if (firstCityName == null && secondCityName == null){
                        mapView.getOverlays().remove(previousMarker);
                        mapView.getOverlays().remove(morePreviousMarker);
                        if (line != null) {
                            mapView.getOverlays().remove(line);
                        }
                    }
                    firstCityName = cityName;
                    city1Location = getAdressForSearch(cityName, mapView);
                    startPoint = convertAddressToGeoPoint(city1Location);
                    isFirstCitySelected = true;
                    editTextLocation.setText("");
                } else {
                    secondCityName = cityName;
                    city2Location = getAdressForSearch(cityName, mapView);
                    endPoint = convertAddressToGeoPoint(city2Location);
                    isFirstCitySelected = false;
                    editTextLocation.setText("");
                    isMark = true;
                }
                if (startPoint != null && endPoint != null){
                    setLine();
                }
                compareTwoLocations();
            }
        });
    }

    private void updateToCompare() {
        editTextLocation.setVisibility(View.VISIBLE);
        buttonSelectLocation.setVisibility(View.VISIBLE);
        editTextCity.setVisibility(View.GONE);
        buttonSearch.setVisibility(View.GONE);
    }
    private void updateToSearch() {
        editTextLocation.setVisibility(View.GONE);
        buttonSelectLocation.setVisibility(View.GONE);
        editTextCity.setVisibility(View.VISIBLE);
        buttonSearch.setVisibility(View.VISIBLE);
        mapView.getOverlays().remove(previousMarker);
        mapView.getOverlays().remove(morePreviousMarker);
        if (line != null) {
            mapView.getOverlays().remove(line);
        }

        firstCityName = null;
        secondCityName = null;
        city1Location = null;
        city2Location = null;

        isFirstCitySelected = false;
    }

    private void markerByTap() {
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (previousMarker != null) {
                    mapView.getOverlays().remove(previousMarker);
                }
                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                mapView.getOverlays().add(marker);
                previousMarker = marker;
                Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        performCityTap(address);
                    } else {
                        Log.e("Tap Error", "Not Found");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }

    private void markerByTapToCompare(final GeocodeCallback callback) {
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                mapView.getController().setCenter(p);
                mapView.invalidate();
                mapView.getOverlays().add(marker);
                endPoint = startPoint;
                startPoint = p;
                if (firstCityName == null && secondCityName == null) {
                    mapView.getOverlays().remove(previousMarker);
                    mapView.getOverlays().remove(morePreviousMarker);
                    if (line != null) {
                        mapView.getOverlays().remove(line);
                    }
                }
                morePreviousMarker = previousMarker;
                previousMarker = marker;
                Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        addressToCompare = addresses.get(0);
                        callback.onGeocodeSuccess(addressToCompare);
                    } else {
                        Log.e("Tap Error", "Not Found");
                        callback.onGeocodeFailure("Адрес не найден");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onGeocodeFailure("Ошибка геокодирования: " + e.getMessage());
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }
}