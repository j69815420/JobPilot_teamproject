package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Handler;

public class LiveModeScoreActivity extends AppCompatActivity {

    private TextView tvScoreBig, tvScoreSmall, tvContent;
    private Button btnAnswerContent, btnVoiceAnalysis, btnAIAnalysis, btnTotalReview;

    private String audioFilePath;
    private String transcript = "";
    private final String OPENAI_API_KEY = "API키 보안상 제거";
    private String voiceFeedback = "";
    private String answerFeedback = "";
    private String aiAnalysisFeedback = "";
    private String totalReviewFeedback = "";
    private int voiceScore = 0;
    private int answerScore = 0;
    private int aiScore = 0;
    private int totalScore = 0;

    private boolean answerCompleted = false;
    private boolean voiceCompleted = false;
    private boolean aiCompleted = false;
    private boolean totalCompleted = false;

    private boolean feedbackRequested = false;
    private boolean isSaved = false;
    private Handler saveHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livemodescore);

        tvScoreBig = findViewById(R.id.tv_score_big);
        tvScoreSmall = findViewById(R.id.tv_score_small);
        tvContent = findViewById(R.id.tv_content);

        btnAnswerContent = findViewById(R.id.btn_answer_content);
        btnVoiceAnalysis = findViewById(R.id.btn_voice_analysis);
        btnAIAnalysis = findViewById(R.id.btn_ai_analysis);
        btnTotalReview = findViewById(R.id.btn_total_review);

        boolean fromHistory = getIntent().getBooleanExtra("fromHistory", false);
        if (fromHistory) {
            int score = getIntent().getIntExtra("score", 0);
            String feedback = getIntent().getStringExtra("feedback");

            tvScoreBig.setText(String.valueOf(score));
            tvContent.setText(feedback);

            btnAnswerContent.setVisibility(Button.GONE);
            btnVoiceAnalysis.setVisibility(Button.GONE);
            btnAIAnalysis.setVisibility(Button.GONE);
            btnTotalReview.setVisibility(Button.GONE);
            return;
        }

        audioFilePath = getIntent().getStringExtra("audioFilePath");
        if (audioFilePath != null && new File(audioFilePath).exists()) {
            transcribeAudio(audioFilePath);
        } else {
            tvContent.setText("녹음 파일이 존재하지 않습니다.");
            System.out.println("DEBUG: 오디오 파일 없음: " + audioFilePath);
        }

        btnAnswerContent.setOnClickListener(v -> { selectButton(btnAnswerContent); displayFeedback("답변"); });
        btnVoiceAnalysis.setOnClickListener(v -> { selectButton(btnVoiceAnalysis); displayFeedback("목소리"); });
        btnAIAnalysis.setOnClickListener(v -> { selectButton(btnAIAnalysis); displayFeedback("AI 분석"); });
        btnTotalReview.setOnClickListener(v -> { selectButton(btnTotalReview); displayFeedback("종합평가"); });
    }

    // ---------------- 음성 변환 ----------------
    private void transcribeAudio(String audioPath) {
        OkHttpClient client = new OkHttpClient();
        File file = new File(audioPath);

        if (!file.exists()) {
            runOnUiThread(() -> tvContent.setText("오디오 파일이 존재하지 않습니다: " + audioPath));
            System.out.println("DEBUG: 파일 존재하지 않음: " + audioPath);
            return;
        }

        // MIME 타입 자동 결정
        String mimeType = "audio/mpeg"; // 기본 mp3
        if (audioPath.endsWith(".wav")) mimeType = "audio/wav";
        else if (audioPath.endsWith(".m4a")) mimeType = "audio/mp4";
        else if (audioPath.endsWith(".mp3")) mimeType = "audio/mpeg";

        RequestBody fileBody = RequestBody.create(file, MediaType.parse(mimeType));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .addFormDataPart("model", "whisper-1")
                .build();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvContent.setText("음성 변환 실패: " + e.getMessage()));
                System.out.println("DEBUG: Whisper 호출 실패 - " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> tvContent.setText("음성 변환 실패: " + response.message()));
                    System.out.println("DEBUG: Whisper 응답 실패 - " + response.message());
                    return;
                }
                try {
                    String resStr = response.body().string();
                    JSONObject json = new JSONObject(resStr);
                    transcript = json.optString("text", "");
                    System.out.println("DEBUG: transcript=" + transcript);

                    runOnUiThread(() -> {
                        if (!transcript.isEmpty()) {
                            tvContent.setText("음성 변환 완료");
                            requestAllFeedback(); // 변환 완료 후 피드백 요청
                        } else {
                            tvContent.setText("음성 변환 후 transcript가 비어있습니다.");
                            System.out.println("DEBUG: transcript 비어있음");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> tvContent.setText("JSON 파싱 실패: " + e.getMessage()));
                    System.out.println("DEBUG: JSON 파싱 실패 - " + e.getMessage());
                }
            }
        });
    }

    // ---------------- 피드백 요청 ----------------
    private void requestAllFeedback() {
        if (feedbackRequested) return;
        feedbackRequested = true;

        requestCategoryFeedback("답변");
        requestCategoryFeedback("목소리");
        requestCategoryFeedback("AI 분석");
        requestCategoryFeedback("종합평가");
    }

    private void requestCategoryFeedback(String category) {
        if (transcript.isEmpty()) return;

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");

            String question = getIntent().getStringExtra("question");
            if (question == null || question.trim().isEmpty()) {
                question = "질문이 없습니다.";
            }
            System.out.println("DEBUG: question=" + question);

            msg.put("content", "질문: " + question + "\n사용자 답변: " + transcript +
                    "\n0~100 점수와 피드백을 JSON으로 {\"feedback\":\"...\",\"score\":숫자} 형식으로 반환해주세요. 다른 텍스트는 포함하지 마세요.");
            messages.put(msg);
            jsonBody.put("messages", messages);

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> tvContent.setText(category + " 요청 실패"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> tvContent.setText(category + " 응답 실패"));
                        return;
                    }

                    try {
                        String resStr = response.body().string();
                        System.out.println("DEBUG OpenAI Response [" + category + "]: " + resStr);

                        JSONObject resJson = new JSONObject(resStr);
                        String content = resJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content").trim();

                        content = content.replaceAll("```", "").trim();

                        int start = content.indexOf("{");
                        int end = content.lastIndexOf("}");
                        if (start != -1 && end != -1 && start < end) {
                            String jsonPart = content.substring(start, end + 1);
                            JSONObject feedbackJson = new JSONObject(jsonPart);

                            String feedback = feedbackJson.optString("feedback", "");
                            int score = feedbackJson.optInt("score", 0);

                            if (!feedback.isEmpty()) {
                                switch (category) {
                                    case "답변":
                                        answerFeedback = feedback;
                                        answerScore = score;
                                        answerCompleted = true;
                                        break;
                                    case "목소리":
                                        voiceFeedback = feedback;
                                        voiceScore = score;
                                        voiceCompleted = true;
                                        break;
                                    case "AI 분석":
                                        aiAnalysisFeedback = feedback;
                                        aiScore = score;
                                        aiCompleted = true;
                                        break;
                                    case "종합평가":
                                        totalReviewFeedback = feedback;
                                        totalScore = score;
                                        totalCompleted = true;
                                        break;
                                }

                                runOnUiThread(() -> displayFeedback(category));

                                if (answerCompleted && voiceCompleted && aiCompleted && totalCompleted && !isSaved) {
                                    isSaved = true;
                                    saveHandler.postDelayed(() -> saveToFirestore(), 2000);
                                }
                            } else {
                                runOnUiThread(() -> tvContent.setText(category + " 피드백이 비어있습니다."));
                            }
                        } else {
                            runOnUiThread(() -> tvContent.setText(category + " JSON 파싱 실패"));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> tvContent.setText(category + " 피드백 처리 중 오류"));
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> tvContent.setText(category + " 요청 중 오류 발생"));
        }
    }

    // ---------------- UI 업데이트 ----------------
    // ---------------- UI 업데이트 ----------------
    private void displayFeedback(String category) {
        String displayFeedback = "";

        switch (category) {
            case "답변": displayFeedback = answerFeedback.isEmpty() ? "대기중..." : answerFeedback; break;
            case "목소리": displayFeedback = voiceFeedback.isEmpty() ? "대기중..." : voiceFeedback; break;
            case "AI 분석": displayFeedback = aiAnalysisFeedback.isEmpty() ? "대기중..." : aiAnalysisFeedback; break;
            case "종합평가": displayFeedback = totalReviewFeedback.isEmpty() ? "대기중..." : totalReviewFeedback; break;
        }

        // 통합 점수 계산
        int sum = 0, count = 0;
        if (answerCompleted) { sum += answerScore; count++; }
        if (voiceCompleted) { sum += voiceScore; count++; }
        if (aiCompleted) { sum += aiScore; count++; }
        if (totalCompleted) { sum += totalScore; count++; }

        int integratedScore = count > 0 ? sum / count : 0;

        tvContent.setText(category + " 피드백:\n" + displayFeedback);
        tvScoreBig.setText(count == 0 ? "-" : String.valueOf(integratedScore));
        tvScoreSmall.setText("/100");
    }


    // ---------------- Firestore 저장 ----------------
    private void saveToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        String interviewId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("date", new Date());
        data.put("mode", "live");
        data.put("transcript", transcript);

        data.put("answer_feedback", answerFeedback);
        data.put("answer_score", answerScore);
        data.put("voice_feedback", voiceFeedback);
        data.put("voice_score", voiceScore);
        data.put("ai_analysis_feedback", aiAnalysisFeedback);
        data.put("ai_analysis_score", aiScore);
        data.put("total_review_feedback", totalReviewFeedback);
        data.put("total_score", totalScore);

        int averageScore = (answerScore + voiceScore + aiScore + totalScore) / 4;
        data.put("average_score", averageScore);

        db.collection("live_interviews")
                .document(interviewId)
                .set(data)
                .addOnSuccessListener(aVoid -> runOnUiThread(() ->
                        Toast.makeText(LiveModeScoreActivity.this, "결과가 저장되었습니다.", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(LiveModeScoreActivity.this, "저장 실패", Toast.LENGTH_SHORT).show()));
    }

    private void selectButton(Button selectedBtn) {
        Button[] buttons = {btnAnswerContent, btnVoiceAnalysis, btnAIAnalysis, btnTotalReview};
        for (Button btn : buttons) {
            btn.setBackgroundResource(R.drawable.bg_button_unselected);
            btn.setTextColor(0xFF000000);
        }
        selectedBtn.setBackgroundResource(R.drawable.bg_button_selected);
        selectedBtn.setTextColor(0xFFFFFFFF);
    }
}
