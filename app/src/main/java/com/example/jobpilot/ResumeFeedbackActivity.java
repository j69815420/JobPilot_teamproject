package com.example.jobpilot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResumeFeedbackActivity extends AppCompatActivity {

    private TextView tvResumeContent, tvAIResult;
    private Button btnDeleteResume, btnEditResume;

    private String documentId;

    private static final String OPENAI_API_KEY = "API키 보안상 제거";

    // 데이터 저장용 변수
    private String title, date, growth, strength, motivation, future, content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_feedback);

        tvResumeContent = findViewById(R.id.tvResumeContent);
        tvAIResult = findViewById(R.id.tvAIResult);
        btnDeleteResume = findViewById(R.id.btnDeleteResume);
        btnEditResume = findViewById(R.id.btnEditResume);

        // ResumeFragment에서 전달받은 인자들
        Intent intent = getIntent();
        documentId = intent.getStringExtra("documentId");
        content = intent.getStringExtra("content");
        title = intent.getStringExtra("title");
        date = intent.getStringExtra("date");
        growth = intent.getStringExtra("growth");
        strength = intent.getStringExtra("strength");
        motivation = intent.getStringExtra("motivation");
        future = intent.getStringExtra("future");

        displayResumeData();
        tvAIResult.setText("AI 분석 중...");

        String resumeText = (content != null && !content.isEmpty())
                ? content
                : "제목: " + title + "\n" +
                "성장 경험: " + growth + "\n" +
                "강점: " + strength + "\n" +
                "동기: " + motivation + "\n" +
                "입사 후 포부: " + future;

        requestAIFeedback(resumeText);

        btnEditResume.setOnClickListener(v -> showEditDialog());
        btnDeleteResume.setOnClickListener(v -> showDeleteDialog());
    }

    private void displayResumeData() {
        if (content != null && !content.isEmpty()) {
            tvResumeContent.setText(
                    "날짜: " + date + "\n" +
                            "제목: " + title + "\n\n" +
                            "내용:\n" + content
            );
        } else {
            tvResumeContent.setText(
                    "날짜: " + date + "\n" +
                            "제목: " + title + "\n" +
                            "성장 경험: " + growth + "\n" +
                            "강점: " + strength + "\n" +
                            "동기: " + motivation + "\n" +
                            "입사 후 포부: " + future
            );
        }
    }

    // 수정 다이얼로그: 파일 업로드 + 직접 작성 모두 대응
    private void showEditDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        scrollView.addView(layout);

        TextView t1 = new TextView(this);
        t1.setText("제목");
        layout.addView(t1);

        EditText etTitle = new EditText(this);
        etTitle.setText(title);
        layout.addView(etTitle);

        TextView tContent = new TextView(this);
        tContent.setText("전체 내용");
        layout.addView(tContent);

        EditText etContent = new EditText(this);
        etContent.setMinLines(10);
        etContent.setText(content);
        layout.addView(etContent);

        new AlertDialog.Builder(this)
                .setTitle("자기소개서 수정")
                .setView(scrollView)
                .setPositiveButton("저장", (dialog, which) -> {
                    title = etTitle.getText().toString();
                    content = etContent.getText().toString();

                    saveToFirestore();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveToFirestore() {
        if (documentId == null) {
            Toast.makeText(this, "문서 ID 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인 필요", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("date", date);
        map.put("growth", growth);
        map.put("strength", strength);
        map.put("motivation", motivation);
        map.put("future", future);
        map.put("content", content);

        db.collection("resumes")
                .document(uid)
                .collection("items")
                .document(documentId)
                .update(map)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "수정 완료", Toast.LENGTH_SHORT).show();
                    displayResumeData();

                    // 수정 후 재분석
                    String updatedText = content;
                    tvAIResult.setText("AI 재분석 중...");
                    requestAIFeedback(updatedText);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (d, w) -> deleteResume())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteResume() {
        if (documentId == null) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("resumes")
                .document(uid)
                .collection("items")
                .document(documentId)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void requestAIFeedback(String resumeText) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                JSONObject json = new JSONObject();
                json.put("model", "gpt-4o-mini");

                JSONArray messages = new JSONArray();

                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "너는 전문 채용 담당자야. 아래 자기소개서를 보고 장점, 개선점, 구조적 문제를 분석해줘.");
                messages.put(system);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", resumeText);
                messages.put(userMsg);

                json.put("messages", messages);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json")
                );

                Request req = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                        .post(body)
                        .build();

                Response res = client.newCall(req).execute();

                if (res.isSuccessful() && res.body() != null) {
                    String result = res.body().string();
                    JSONObject resJson = new JSONObject(result);

                    String feedback =
                            resJson.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                    runOnUiThread(() -> tvAIResult.setText(feedback));
                } else {
                    runOnUiThread(() ->
                            tvAIResult.setText("AI 오류: " + res.code() + "\n" + res.message()));
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        tvAIResult.setText("오류: " + e.getMessage()));
            }
        }).start();
    }

    public static void start(Context context, ResumeItem item, String documentId) {
        Intent intent = new Intent(context, ResumeFeedbackActivity.class);
        intent.putExtra("documentId", documentId);
        intent.putExtra("content", item.content);
        intent.putExtra("title", item.title);
        intent.putExtra("date", item.date);
        intent.putExtra("growth", item.growth);
        intent.putExtra("strength", item.strength);
        intent.putExtra("motivation", item.motivation);
        intent.putExtra("future", item.future);

        context.startActivity(intent);
    }
}
