package com.example.jobpilot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InquiryActivity extends AppCompatActivity {

    private TextView btnBack;
    private EditText etInquiry;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry);

        btnBack = findViewById(R.id.btn_back);
        etInquiry = findViewById(R.id.et_inquiry);
        btnSend = findViewById(R.id.btn_send);

        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // 보내기 버튼
        btnSend.setOnClickListener(v -> {
            String text = etInquiry.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "문의 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "문의가 접수되었습니다.", Toast.LENGTH_SHORT).show();
                etInquiry.setText("");
            }
        });
    }
}
