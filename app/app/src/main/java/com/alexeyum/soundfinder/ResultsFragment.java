package com.alexeyum.soundfinder;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ResultsFragment extends Fragment {
    TextView tvResults;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        tvResults = view.findViewById(R.id.tvResults);

        return view;
    }

    public void updateResultsText(String newText) {
        if (tvResults != null) {
            tvResults.setText(newText);
        }
    }
}