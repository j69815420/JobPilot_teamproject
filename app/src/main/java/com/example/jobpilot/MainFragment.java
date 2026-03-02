package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

    private Button btnLiveMode, btnPracticeMode, btnFeedBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_interview, container, false);

        btnLiveMode = view.findViewById(R.id.btnLiveMode);
        btnPracticeMode = view.findViewById(R.id.btnPracticeMode);
        btnFeedBack = view.findViewById(R.id.btnFeedBack);

        btnLiveMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LiveModeActivity.class);
            startActivity(intent);
        });

        btnPracticeMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            startActivity(intent);
        });

        btnFeedBack.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FeedBackActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
