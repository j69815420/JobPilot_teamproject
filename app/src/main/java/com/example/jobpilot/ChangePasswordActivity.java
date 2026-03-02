package com.example.jobpilot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etPw1, etPw2;
    private Button btnChangePassword;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pw2);

        etPw1 = findViewById(R.id.et_pw1);
        etPw2 = findViewById(R.id.et_pw2);
        btnChangePassword = findViewById(R.id.btn_next);

        // 이전 화면에서 전달된 userId
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnChangePassword.setOnClickListener(v -> {
            String pw1 = etPw1.getText().toString().trim();
            String pw2 = etPw2.getText().toString().trim();

            if (!pw1.equals(pw2)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(pw1)) {
                Toast.makeText(this, "비밀번호는 8자 이상, 영어+숫자를 포함해야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firestore에 비밀번호 바로 업데이트
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .update("password", pw1)
                    .addOnSuccessListener(aVoid -> {
                        // SharedPreferences에 로그인 정보 저장 (자동 로그인용)
                        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", userId)
                                .putString("user_password", pw1)
                                .apply();

                        Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();

                        // 로그인 화면으로 이동
                        Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "비밀번호 변경 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[a-zA-Z].*") &&
                password.matches(".*[0-9].*");
    }
}
