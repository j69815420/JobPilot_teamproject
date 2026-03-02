package com.example.jobpilot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private final String OPENAI_API_KEY = "API키 보안상 제거";

    private List<String> fetchedQuestions = new ArrayList<>();
    private List<String> askedQuestions = new ArrayList<>();
    private List<String> userAnswersForDB = new ArrayList<>();
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messageList;
    private EditText messageEditText;
    private ImageButton sendButton;

    private int questionIndex = 0;
    private boolean waitingForStart = true;
    private boolean waitingForNext = false;
    private String lastUserAnswer = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practicemode);

        recyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        InterestManager interestManager = new InterestManager();

        interestManager.fetchQuestionsByUserInterests(
                questions -> {
                    fetchedQuestions.addAll(questions);
                    Collections.shuffle(fetchedQuestions);
                    addMessage(new Message("안녕하세요! 질문을 시작하시겠습니까? (예/아니오)", false));
                },
                e -> Log.e(TAG, "질문 불러오기 실패", e)
        );

        sendButton.setOnClickListener(v -> {
            String userInput = messageEditText.getText().toString().trim();
            if (userInput.isEmpty()) return;

            addMessage(new Message(userInput, true));
            messageEditText.setText("");

            if (waitingForStart) {
                handleStartResponse(userInput);
            } else if (waitingForNext) {
                handleNextResponse(userInput);
            } else {
                handleAnswer(userInput);
            }
        });
    }

    private void handleStartResponse(String input) {
        waitingForStart = false;
        if (input.equalsIgnoreCase("예")) {
            showNextQuestion();
        } else {
            addMessage(new Message("알겠습니다. 다음에 또 만나요!", false));
            finish();
        }
    }

    private void handleAnswer(String input) {
        lastUserAnswer = input;

        if (!waitingForStart && !waitingForNext) {
            userAnswersForDB.add(input);
        }

        addMessage(new Message("좋아요! 답변 감사합니다.", false));
        addMessage(new Message("다음 질문으로 넘어가시겠습니까? (예/아니오)", false));
        waitingForNext = true;
    }

    private void handleNextResponse(String input) {
        waitingForNext = false;
        if (input.equalsIgnoreCase("예")) {
            showNextQuestion();
        } else {
            addMessage(new Message("수고하셨습니다! AI가 분석 중입니다...", false));
            goToResult();
        }
    }

    private void showNextQuestion() {
        if (questionIndex < fetchedQuestions.size()) {
            String currentQuestion = fetchedQuestions.get(questionIndex);
            addMessage(new Message(fetchedQuestions.get(questionIndex), false));
            askedQuestions.add(currentQuestion);
            questionIndex++;
        } else {
            addMessage(new Message("모든 질문이 끝났습니다!", false));
            goToResult();
        }
    }

    private void addMessage(Message message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void goToResult() {
        recyclerView.postDelayed(() -> {
            // AI 피드백 요청 후 저장 및 화면 이동
            requestAIFeedbackAndSave();
        }, 500);
    }

    private void requestAIFeedbackAndSave() {
        // 질문-답변 텍스트 생성
        StringBuilder interviewText = new StringBuilder();
        for (int i = 0; i < askedQuestions.size(); i++) {
            interviewText.append("질문 ").append(i + 1).append(": ")
                    .append(askedQuestions.get(i)).append("\n");
            interviewText.append("답변: ").append(userAnswersForDB.get(i)).append("\n\n");
        }

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", "다음은 텍스트 기반 면접 내용입니다:\n" + interviewText.toString() +
                    "\n답변 내용을 종합적으로 평가하여 0~100 점수와 상세한 피드백을 JSON으로 {\"feedback\":\"...\",\"score\":숫자} 형식으로 반환해주세요. 다른 텍스트는 포함하지 마세요.");
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
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "AI 분석 실패", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "AI 분석 실패", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    try {
                        String resStr = response.body().string();
                        JSONObject resJson = new JSONObject(resStr);
                        String content = resJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content").trim();

                        if (content.startsWith("```json")) content = content.substring(7).trim();
                        else if (content.startsWith("```")) content = content.substring(3).trim();
                        if (content.endsWith("```")) content = content.substring(0, content.length() - 3).trim();

                        JSONObject feedbackJson = new JSONObject(content);
                        String aiFeedback = feedbackJson.getString("feedback");
                        int aiScore = feedbackJson.getInt("score");

                        // Firestore에 저장
                        saveInterviewToFirestore(aiScore, aiFeedback);

                        // ChatScoreActivity로 이동
                        runOnUiThread(() -> {
                            Intent intent = new Intent(ChatActivity.this, ChatScoreActivity.class);
                            intent.putStringArrayListExtra("questions", new ArrayList<>(askedQuestions));
                            intent.putStringArrayListExtra("answers", new ArrayList<>(userAnswersForDB));
                            intent.putExtra("score", aiScore);
                            intent.putExtra("feedback", aiFeedback);
                            startActivity(intent);
                            finish();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "결과 처리 실패", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveInterviewToFirestore(int score, String feedback) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        String interviewId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> interviewData = new HashMap<>();
        interviewData.put("uid", uid);
        interviewData.put("mode", "text");
        interviewData.put("date", new Date());
        interviewData.put("questions", askedQuestions);
        interviewData.put("answers", userAnswersForDB);
        interviewData.put("score", score);
        interviewData.put("ai_feedback", feedback);

        db.collection("chat_interviews")
                .document(interviewId)
                .set(interviewData)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "면접 데이터 저장 완료"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "면접 데이터 저장 실패", e));
    }
}