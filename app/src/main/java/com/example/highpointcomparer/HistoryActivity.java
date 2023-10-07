package com.example.highpointcomparer;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.highpointcomparer.CityElevation;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private ArrayList<CityElevation> historyItemsList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        historyListView = findViewById(R.id.historyListView);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("historyItems")) {
            historyItemsList = (ArrayList<CityElevation>) intent.getSerializableExtra("historyItems");
            showSearchHistory();
        }
    }

    private void showSearchHistory() {
        List<String> cityNames = new ArrayList<>();
        for (CityElevation cityElevation : historyItemsList) {
            cityNames.add(cityElevation.getCityName() + " - " + cityElevation.getElevation() + " м");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cityNames);
        historyListView.setAdapter(adapter);
    }
}
