package com.example.jobpilot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LiveModeActivity extends AppCompatActivity {

    private static final int REQUEST_MIC_PERMISSION = 1001;
    private static final String API_BASE_URL = "https://api.openai.com/";
    private static final String OPENAI_API_KEY = "Bearer API키 보안상 제거"; 
    private TextView tvQuestion, tvAnswerStatus;
    private Button btnStart, btnNext, btnFinish, btnRecord;
    private LinearLayout layoutStopFinish;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;

    private String[] aiQuestions = null;
    private int currentQuestionIndex = 0;

    private OpenAIApi openAIApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livemode1);

        tvQuestion = findViewById(R.id.tv_question);
        btnStart = findViewById(R.id.btn_start);
        btnNext = findViewById(R.id.btn_next);
        btnFinish = findViewById(R.id.btn_finish);
        btnRecord = findViewById(R.id.btn_record);
        layoutStopFinish = findViewById(R.id.layout_stop_finish);

        // 답변 상태 표시
        tvAnswerStatus = new TextView(this);
        tvAnswerStatus.setText("답변 인식중...");
        tvAnswerStatus.setTextSize(16f);
        tvAnswerStatus.setTextColor(0xFF000000);
        tvAnswerStatus.setPadding(20, 20, 20, 20);
        tvAnswerStatus.setVisibility(TextView.GONE);

        LinearLayout parent = (LinearLayout) tvQuestion.getParent();
        parent.addView(tvAnswerStatus, parent.indexOfChild(tvQuestion) + 1);

        // OpenAI 클라이언트 생성 및 질문 생성
        buildOpenAIClient();
        requestAIInterviewQuestions();

        // START 버튼
        btnStart.setOnClickListener(v -> {
            if (aiQuestions == null) {
                Toast.makeText(this, "AI 질문 생성 중입니다...", Toast.LENGTH_SHORT).show();
                return;
            }

            layoutStopFinish.setVisibility(LinearLayout.VISIBLE);
            btnStart.setVisibility(Button.GONE);
            tvQuestion.setText(aiQuestions[currentQuestionIndex]);

            if (!hasMicPermission()) requestMicPermission();
        });

        // NEXT 버튼
        btnNext.setOnClickListener(v -> {
            if (aiQuestions == null) return;

            currentQuestionIndex++;
            if (currentQuestionIndex >= aiQuestions.length) {
                Toast.makeText(this, "마지막 질문입니다.", Toast.LENGTH_SHORT).show();
                currentQuestionIndex = aiQuestions.length - 1;
                return;
            }
            tvQuestion.setText(aiQuestions[currentQuestionIndex]);
        });

        // RECORD 버튼
        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                if (!hasMicPermission()) requestMicPermission();
                else startRecordingWithUI();
            } else stopRecordingWithUI();
        });

        // FINISH 버튼 → 녹음 완료 후 LiveModeScoreActivity로 이동, 질문 전달
        btnFinish.setOnClickListener(v -> {
            if (isRecording) stopRecordingWithUI();

            if (audioFilePath == null) {
                Toast.makeText(this, "녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Toast.makeText(this, "녹음 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 현재 질문도 함께 전달
            String currentQuestion = aiQuestions != null && aiQuestions.length > 0
                    ? aiQuestions[currentQuestionIndex]
                    : "질문 없음";

            Intent intent = new Intent(LiveModeActivity.this, LiveModeScoreActivity.class);
            intent.putExtra("audioFilePath", audioFilePath);
            intent.putExtra("question", currentQuestion);
            startActivity(intent);
        });
    }

    // ---------------- 권한 ----------------
    private boolean hasMicPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "권한 승인 완료. 녹음을 시작할 수 있습니다.", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // ---------------- 녹음 ----------------
    private void startRecordingWithUI() {
        startRecording();
        isRecording = true;

        GradientDrawable bg = (GradientDrawable) btnRecord.getBackground();
        bg.setColor(Color.parseColor("#FF7979"));
        btnRecord.setTextColor(Color.WHITE);
        btnRecord.setText("RECORDING...");

        tvAnswerStatus.setVisibility(TextView.VISIBLE);
    }

    private void stopRecordingWithUI() {
        stopRecordingIfNeeded();
        isRecording = false;

        GradientDrawable bg = (GradientDrawable) btnRecord.getBackground();
        bg.setColor(Color.parseColor("#E0E0E0"));
        btnRecord.setTextColor(Color.BLACK);
        btnRecord.setText("RECORD");

        tvAnswerStatus.setVisibility(TextView.GONE);
    }

    private void startRecording() {
        audioFilePath = getExternalFilesDir(null).getAbsolutePath()
                + "/interview_" + System.currentTimeMillis() + ".mp4";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e("LiveMode", "녹음 시작 실패", e);
            Toast.makeText(this, "녹음 시작 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingIfNeeded() {
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (RuntimeException ignored) { }
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    // ---------------- OpenAI 질문 생성 ----------------
    private void buildOpenAIClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        openAIApi = retrofit.create(OpenAIApi.class);
    }

    private void requestAIInterviewQuestions() {
        List<ChatRequest.Message> msgs = new ArrayList<>();
        msgs.add(new ChatRequest.Message(
                "user",
                "면접 서술형 질문 5개 생성해줘. 한 문장으로 구성하고 전문적인 질문으로 해줘."
        ));

        ChatRequest req = new ChatRequest("gpt-3.5-turbo", msgs);

        openAIApi.createCompletion(OPENAI_API_KEY, req)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> res) {
                        if (!res.isSuccessful() || res.body() == null) {
                            tvQuestion.setText("질문 생성 실패");
                            return;
                        }

                        String output = res.body().choices.get(0).message.content;
                        aiQuestions = output.split("\n");

                        for (int i = 0; i < aiQuestions.length; i++)
                            aiQuestions[i] = aiQuestions[i].replaceAll("^[0-9]+\\. ", "").trim();

                        tvQuestion.setText("AI 질문 생성 완료. START 버튼을 누르세요.");
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        tvQuestion.setText("AI 오류: " + t.getMessage());
                    }
                });
    }
}
