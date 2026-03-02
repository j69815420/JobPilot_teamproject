package com.example.jobpilot;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.CompoundButtonCompat;

public class TermsActivity extends AppCompatActivity {

    private CheckBox cbAll, cbTerms, cbPrivacy, cbMarketing;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        cbAll = findViewById(R.id.cbAll);
        cbTerms = findViewById(R.id.cbTerms);
        cbPrivacy = findViewById(R.id.cbPrivacy);
        cbMarketing = findViewById(R.id.cbMarketing);
        btnNext = findViewById(R.id.btnNext);

        // 체크박스 색상 상태 정의
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // 체크된 상태
                new int[] { -android.R.attr.state_checked } // 체크 안 된 상태
        };
        int[] colors = new int[] {
                Color.DKGRAY, // 체크 시 진한 회색
                Color.LTGRAY  // 체크 안 하면 연한 기본색
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);

        // 모든 체크박스에 적용
        CompoundButtonCompat.setButtonTintList(cbAll, colorStateList);
        CompoundButtonCompat.setButtonTintList(cbTerms, colorStateList);
        CompoundButtonCompat.setButtonTintList(cbPrivacy, colorStateList);
        CompoundButtonCompat.setButtonTintList(cbMarketing, colorStateList);

        // 전체 약관 동의 클릭 시 모든 체크박스 상태 변경
        cbAll.setOnClickListener(v -> {
            boolean isChecked = cbAll.isChecked();
            cbTerms.setChecked(isChecked);
            cbPrivacy.setChecked(isChecked);
            cbMarketing.setChecked(isChecked);
        });

        // 개별 체크박스 클릭 시 전체 약관 동의 체크상태 갱신
        CheckBox.OnClickListener individualListener = v -> {
            boolean allChecked = cbTerms.isChecked() && cbPrivacy.isChecked() && cbMarketing.isChecked();
            cbAll.setChecked(allChecked);
        };

        cbTerms.setOnClickListener(individualListener);
        cbPrivacy.setOnClickListener(individualListener);
        cbMarketing.setOnClickListener(individualListener);

        // 다음 버튼 클릭 시
        btnNext.setOnClickListener(v -> {
            // 필수 항목 확인
            if (!cbTerms.isChecked() || !cbPrivacy.isChecked()) {
                Toast.makeText(this, "필수 약관에 동의해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 회원가입 화면으로 이동
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });
    }
}
