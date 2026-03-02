package com.example.jobpilot;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import java.util.ArrayList;

public class ChatScoreActivity extends AppCompatActivity {

    private AppCompatButton btnBack, btnAnswer, btnReview;
    private TextView tvScoreBig, tvScoreSmall, tvContent;

    private ArrayList<String> questions;
    private ArrayList<String> answers;
    private String aiFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practicemodescore);

        // 뷰 연결
        btnBack = findViewById(R.id.btn_back);
        btnAnswer = findViewById(R.id.btn_answer);
        btnReview = findViewById(R.id.btn_review);
        tvScoreBig = findViewById(R.id.tv_score_big);
        tvScoreSmall = findViewById(R.id.tv_score_small);
        tvContent = findViewById(R.id.tv_content);

        // Intent로 데이터 받기
        questions = getIntent().getStringArrayListExtra("questions");
        answers = getIntent().getStringArrayListExtra("answers");
        int score = getIntent().getIntExtra("score", 0);
        aiFeedback = getIntent().getStringExtra("feedback");  // "ai_feedback" → "feedback"

        // 점수 표시
        if (tvScoreBig != null) tvScoreBig.setText(String.valueOf(score));
        if (tvScoreSmall != null) tvScoreSmall.setText("/100");

        // 기본 상태: 답변 버튼 선택
        btnAnswer.setBackgroundResource(R.drawable.bg_button_selected);
        btnReview.setBackgroundResource(R.drawable.bg_button_unselected);
        displayAnswers();

        // 뒤로가기 버튼 클릭
        btnBack.setOnClickListener(v -> finish());

        // 답변 버튼 클릭
        btnAnswer.setOnClickListener(v -> {
            btnAnswer.setBackgroundResource(R.drawable.bg_button_selected);
            btnReview.setBackgroundResource(R.drawable.bg_button_unselected);
            displayAnswers();
        });

        // 총평 버튼 클릭
        btnReview.setOnClickListener(v -> {
            btnReview.setBackgroundResource(R.drawable.bg_button_selected);
            btnAnswer.setBackgroundResource(R.drawable.bg_button_unselected);
            displayReview();
        });
    }

    // 답변 내용 표시
    private void displayAnswers() {
        StringBuilder sb = new StringBuilder();
        if (questions != null && answers != null) {
            int count = Math.min(questions.size(), answers.size());
            for (int i = 0; i < count; i++) {
                sb.append("🤖 질문: ").append(questions.get(i)).append("\n");
                sb.append("📝 답변: ").append(answers.get(i)).append("\n\n");
            }
        }
        tvContent.setText(sb.toString());
    }

    // 총평 내용 표시 (AI 피드백 사용)
    private void displayReview() {
        if (aiFeedback != null && !aiFeedback.isEmpty()) {
            tvContent.setText("AI 분석 총평:\n\n" + aiFeedback);
        } else {
            tvContent.setText("AI 피드백을 불러올 수 없습니다.");
        }
    }
}