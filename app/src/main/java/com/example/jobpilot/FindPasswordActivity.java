package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FindPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etUserId, etVerificationCode;
    private Button btnSendCode, btnVerify, btnNext;

    private String sentVerificationCode;
    private String userIdFromFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw1);

        etEmail = findViewById(R.id.et_email);
        etUserId = findViewById(R.id.et_id);
        etVerificationCode = findViewById(R.id.et_verification_code);

        btnSendCode = findViewById(R.id.btn_send_code);
        btnVerify = findViewById(R.id.btn_verify);
        btnNext = findViewById(R.id.btn_next);
        btnNext.setEnabled(false); // 인증 전에는 비활성화

        // 인증번호 발송
        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String userIdInput = etUserId.getText().toString().trim();

            if (email.isEmpty() || userIdInput.isEmpty()) {
                Toast.makeText(this, "아이디와 이메일을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("id", userIdInput)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            userIdFromFirestore = doc.getId();

                            // 6자리 인증코드 생성
                            sentVerificationCode = String.valueOf((int) (Math.random() * 899999) + 100000);

                            // 이메일 발송 (EmailSender 클래스 필요)
                            new Thread(() -> {
                                try {
                                    EmailSender sender = new EmailSender();
                                    sender.sendEmail(email, "비밀번호 변경 인증번호",
                                            "인증번호: " + sentVerificationCode);
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "인증번호가 이메일로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                    );
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "메일 전송 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                                }
                            }).start();

                        } else {
                            Toast.makeText(this, "아이디와 이메일이 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
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

        // 새 비밀번호 화면으로 이동
        btnNext.setOnClickListener(v -> {
            if (userIdFromFirestore != null) {
                Intent intent = new Intent(FindPasswordActivity.this, ChangePasswordActivity.class);
                intent.putExtra("userId", userIdFromFirestore);
                startActivity(intent);
                finish();
            }
        });
    }
}

