package com.alexeyum.soundfinder;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class InfoFragment extends Fragment {
    TextView tvInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        tvInfo = view.findViewById(R.id.tvInfo);

        return view;
    }

    public void updateInfoText(String newText) {
        if (tvInfo != null) {
            tvInfo.setText(newText);
        }
    }
}