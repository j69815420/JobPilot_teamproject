package com.example.jobpilot;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class LiveModeScoreActivityForFeedBack extends AppCompatActivity {

    private TextView tvScoreBig, tvScoreSmall, tvContent;
    private TextView btnAnswerContent, btnVoiceAnalysis, btnAiAnalysis, btnTotalReview;

    private ArrayList<String> answers;
    private String answerFeedback, aiFeedback, voiceFeedback, review;
    private int score, answerScore, aiScore, voiceScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livemodescore);

        // 뷰 연결
        tvScoreBig = findViewById(R.id.tv_score_big);
        tvScoreSmall = findViewById(R.id.tv_score_small);
        tvContent = findViewById(R.id.tv_content);

        btnAnswerContent = findViewById(R.id.btn_answer_content);
        btnVoiceAnalysis = findViewById(R.id.btn_voice_analysis);
        btnAiAnalysis = findViewById(R.id.btn_ai_analysis);
        btnTotalReview = findViewById(R.id.btn_total_review);

        // Intent 데이터
        score = getIntent().getIntExtra("score", 0);
        answerScore = getIntent().getIntExtra("answerScore", 0);
        aiScore = getIntent().getIntExtra("aiScore", 0);
        voiceScore = getIntent().getIntExtra("voiceScore", 0);
        answers = getIntent().getStringArrayListExtra("answers");
        answerFeedback = getIntent().getStringExtra("answer_feedback");
        aiFeedback = getIntent().getStringExtra("ai_feedback");
        voiceFeedback = getIntent().getStringExtra("voice_feedback");
        review = getIntent().getStringExtra("review");

        tvScoreBig.setText(String.valueOf(score));
        tvScoreSmall.setText("/100");

        // 기본 화면: 답변 내용
        displayAnswerContent();

        // 버튼 클릭 리스너
        btnAnswerContent.setOnClickListener(v -> displayAnswerContent());
        btnVoiceAnalysis.setOnClickListener(v -> displayVoiceAnalysis());
        btnAiAnalysis.setOnClickListener(v -> displayAiAnalysis());
        btnTotalReview.setOnClickListener(v -> displayTotalReview());
    }

    // 선택된 버튼 UI 적용
    private void updateButtonUI(TextView selectedButton) {
        // 모든 버튼 기본 상태로 초기화
        btnAnswerContent.setBackgroundResource(R.drawable.bg_button_unselected);
        btnVoiceAnalysis.setBackgroundResource(R.drawable.bg_button_unselected);
        btnAiAnalysis.setBackgroundResource(R.drawable.bg_button_unselected);
        btnTotalReview.setBackgroundResource(R.drawable.bg_button_unselected);

        btnAnswerContent.setTextColor(getColor(R.color.black));
        btnVoiceAnalysis.setTextColor(getColor(R.color.black));
        btnAiAnalysis.setTextColor(getColor(R.color.black));
        btnTotalReview.setTextColor(getColor(R.color.black));

        // 선택된 버튼만 활성화
        selectedButton.setBackgroundResource(R.drawable.bg_button_selected);
        selectedButton.setTextColor(getColor(R.color.white));
    }

    private void displayAnswerContent() {
        updateButtonUI(btnAnswerContent);
        tvScoreBig.setText(String.valueOf(answerScore));

        StringBuilder sb = new StringBuilder();
        sb.append("📝 답변 분석:\n");
        sb.append(answerFeedback != null && !answerFeedback.isEmpty() ? answerFeedback : "답변이 없습니다.");
        tvContent.setText(sb.toString());
    }

    private void displayVoiceAnalysis() {
        updateButtonUI(btnVoiceAnalysis);
        tvScoreBig.setText(String.valueOf(voiceScore));

        StringBuilder sb = new StringBuilder();
        sb.append("🎤 목소리 분석:\n");
        sb.append(voiceFeedback != null && !voiceFeedback.isEmpty() ? voiceFeedback : "피드백이 없습니다.");
        tvContent.setText(sb.toString());
    }

    private void displayAiAnalysis() {
        updateButtonUI(btnAiAnalysis);
        tvScoreBig.setText(String.valueOf(aiScore));

        StringBuilder sb = new StringBuilder();
        sb.append("🤖 AI 분석:\n");
        sb.append(aiFeedback != null && !aiFeedback.isEmpty() ? aiFeedback : "피드백이 없습니다.");
        tvContent.setText(sb.toString());
    }

    private void displayTotalReview() {
        updateButtonUI(btnTotalReview);

        StringBuilder sb = new StringBuilder();
        sb.append("📌 종합평가:\n");
        sb.append(review != null && !review.isEmpty() ? review : "총평이 없습니다.");
        tvContent.setText(sb.toString());
    }
}
