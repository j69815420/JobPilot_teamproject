package com.example.jobpilot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class InterestActivity extends AppCompatActivity {

    private static final String TAG = "InterestActivity";
    private final int MAX_SELECTION = 3;
    private List<AppCompatButton> selectedButtons = new ArrayList<>();
    private List<AppCompatButton> allButtons = new ArrayList<>();
    private String userId;  // UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interest);

        // 1. UID 가져오기
        userId = getIntent().getStringExtra("uid");
        if (userId == null || userId.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
            userId = prefs.getString("uid", null);
        }
        if (userId == null || userId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) userId = currentUser.getUid();
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "회원 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. 관심 분야 버튼 등록
        int[] buttonIds = {
                R.id.btn_frontend, R.id.btn_backend, R.id.btn_fullstack, R.id.btn_android,
                R.id.btn_ios, R.id.btn_cross, R.id.btn_data, R.id.btn_ml,
                R.id.btn_cloud, R.id.btn_ai, R.id.btn_devops, R.id.btn_security,
                R.id.btn_game_client, R.id.btn_game_server, R.id.btn_arvr,
                R.id.btn_fw, R.id.btn_iot, R.id.btn_blockchain,
                R.id.btn_robotics, R.id.btn_opensource
        };

        for (int id : buttonIds) {
            AppCompatButton btn = findViewById(id);
            allButtons.add(btn);
            btn.setOnClickListener(buttonClickListener);
        }

        // 3. 완료 버튼 클릭
        AppCompatButton btnComplete = findViewById(R.id.btn_complete);
        btnComplete.setOnClickListener(v -> {
            if (selectedButtons.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> selectedInterests = new ArrayList<>();
            for (AppCompatButton b : selectedButtons) {
                selectedInterests.add(b.getText().toString());
            }

            Log.d(TAG, "선택된 관심분야: " + selectedInterests);

            // 4. NavigationActivity로 전달
            Intent intent = new Intent(InterestActivity.this, NavigationActivity.class);
            intent.putStringArrayListExtra("selected_interests", selectedInterests);
            startActivity(intent);
            finish();
        });
    }

    private final AppCompatButton.OnClickListener buttonClickListener = view -> {
        AppCompatButton button = (AppCompatButton) view;

        if (selectedButtons.contains(button)) {
            button.setSelected(false);
            selectedButtons.remove(button);
        } else {
            if (selectedButtons.size() >= MAX_SELECTION) {
                Toast.makeText(this, "최대 3개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            button.setSelected(true);
            selectedButtons.add(button);
        }
    };
}
