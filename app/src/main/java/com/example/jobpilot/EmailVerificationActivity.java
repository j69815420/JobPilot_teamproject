package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmailVerificationActivity extends AppCompatActivity {
    private EditText etId, etEmail, etVerificationCode;
    private Spinner spinnerEmailDomain;
    private Button btnSendCode, btnVerify, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw1);

        // View 연결
        etId = findViewById(R.id.et_id);
        etEmail = findViewById(R.id.et_email);
        spinnerEmailDomain = findViewById(R.id.spinner_email_domain);
        etVerificationCode = findViewById(R.id.et_verification_code);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnVerify = findViewById(R.id.btn_verify);
        btnNext = findViewById(R.id.btn_next);

        // 이메일 도메인 선택
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.email_domains, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmailDomain.setAdapter(adapter);

        // 인증번호 발송 버튼
        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String domain = spinnerEmailDomain.getSelectedItem().toString();
            String fullEmail = email + "@" + domain;

            // 여기에 이메일 유효성 검사 및 인증번호 발송 로직 추가
            Toast.makeText(this, "인증번호가 " + fullEmail + " 으로 전송되었습니다.", Toast.LENGTH_SHORT).show();
        });

        // 인증 버튼
        btnVerify.setOnClickListener(v -> {
            String code = etVerificationCode.getText().toString().trim();

            // 여기에 실제 인증번호 확인 로직 추가
            if (code.equals("123456")) { // 예시
                Toast.makeText(this, "인증 성공!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "인증 실패. 올바른 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 다음 버튼
        btnNext.setOnClickListener(v -> {
            // 다음 화면으로 이동 (예: 비밀번호 변경 화면)
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }
}

