package com.example.highpointcomparer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CompareInfoDialog extends BottomSheetDialogFragment {
    private double elevationDifference;

    public CompareInfoDialog(double elevationDifference) {
        this.elevationDifference = elevationDifference;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compare_info, container, false);
        TextView textView = view.findViewById(R.id.textViewHeightDifference);
        textView.setText("Разница в высоте: " + elevationDifference + " м");
        return view;
    }
}
