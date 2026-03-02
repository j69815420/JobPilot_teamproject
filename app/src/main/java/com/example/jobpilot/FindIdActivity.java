package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FindIdActivity extends AppCompatActivity {

    EditText etEmail, etVerificationCode;
    Spinner spinnerDomain;
    Button btnSendCode, btnVerify, btnNext;

    String[] emailDomains = {"선택하세요", "naver.com", "gmail.com", "daum.net", "kakao.com"};
    String sentVerificationCode;
    String foundUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        etEmail = findViewById(R.id.et_email);
        etVerificationCode = findViewById(R.id.et_verification_code);
        spinnerDomain = findViewById(R.id.spinner_email_domain);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnVerify = findViewById(R.id.btn_verify);
        btnNext = findViewById(R.id.btn_next);

        // 스피너 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, emailDomains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDomain.setAdapter(adapter);

        //  인증번호 발송
        btnSendCode.setOnClickListener(v -> {
            String emailInput = etEmail.getText().toString().trim();
            String domain = spinnerDomain.getSelectedItem().toString();

            if (emailInput.isEmpty() || domain.equals("선택하세요")) {
                Toast.makeText(this, "이메일을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullEmail = emailInput + "@" + domain;

            // Firestore에서 사용자 조회 (이메일만)
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("email", fullEmail)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            foundUserId = doc.getString("id");

                            // 6자리 인증코드 생성
                            sentVerificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

                            // 이메일 발송 (스레드 사용)
                            new Thread(() -> {
                                try {
                                    EmailSender sender = new EmailSender();
                                    sender.sendEmail(fullEmail, "인증번호 발송", "인증번호: " + sentVerificationCode);

                                    runOnUiThread(() -> Toast.makeText(this, "인증번호가 이메일로 전송되었습니다.", Toast.LENGTH_SHORT).show());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    runOnUiThread(() -> Toast.makeText(this, "메일 전송 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            }).start();

                        } else {
                            Toast.makeText(this, "사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // 인증번호 확인
        btnVerify.setOnClickListener(v -> {
            String inputCode = etVerificationCode.getText().toString().trim();
            if (sentVerificationCode == null) {
                Toast.makeText(this, "먼저 인증번호를 발송하세요.", Toast.LENGTH_SHORT).show();
            } else if (inputCode.equals(sentVerificationCode)) {
                Toast.makeText(this, "인증 완료", Toast.LENGTH_SHORT).show();
                btnNext.setEnabled(true);
            } else {
                Toast.makeText(this, "인증번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //  인증 후 다음 화면으로 이동
        btnNext.setOnClickListener(v -> {
            if (foundUserId != null) {
                Intent intent = new Intent(FindIdActivity.this, ShowIdActivity.class);
                intent.putExtra("userId", foundUserId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "사용자 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setEnabled(false); // 인증 완료 후 활성화
    }
}
