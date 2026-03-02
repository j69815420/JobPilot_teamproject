package com.example.jobpilot;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ShowIdActivity extends AppCompatActivity {

    TextView tvResultId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_id); // 네 레이아웃 이름

        tvResultId = findViewById(R.id.tv_result_id);

        String userId = getIntent().getStringExtra("userId");

        // XXXXXXX 부분에 아이디만 넣기
        tvResultId.setText(userId + " 입니다");
    }
}

