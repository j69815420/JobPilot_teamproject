package com.example.jobpilot;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FeedBackActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeedBackAdapter adapter;
    private List<FeedBackItem> feedbackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        recyclerView = findViewById(R.id.recyclerFeedback);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(false);
        manager.setStackFromEnd(false);
        recyclerView.setLayoutManager(manager);

        feedbackList = new ArrayList<>();
        adapter = new FeedBackAdapter(this, feedbackList);
        recyclerView.setAdapter(adapter);

        loadFeedbackFromDB();

        Button btnBack = findViewById(R.id.tvFeedbackTitle);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadFeedbackFromDB() {
        feedbackList.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

        // 실전 모드 가져오기
        db.collection("live_interviews")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Timestamp ts = doc.getTimestamp("date");
                        String dateStr = ts != null ? sdf.format(ts.toDate()) : "날짜 없음";

                        int totalScore = doc.getLong("total_score") != null ? doc.getLong("total_score").intValue() : 0;
                        int answerScore = doc.getLong("answer_score") != null ? doc.getLong("answer_score").intValue() : 0;
                        int aiScore = doc.getLong("ai_analysis_score") != null ? doc.getLong("ai_analysis_score").intValue() : 0;
                        int voiceScore = doc.getLong("voice_score") != null ? doc.getLong("voice_score").intValue() : 0;

                        FeedBackItem item = new FeedBackItem(
                                FeedBackItem.Type.REAL,
                                dateStr,
                                totalScore,
                                totalScore,
                                answerScore,
                                aiScore,
                                voiceScore,
                                "실전모드",
                                doc.getString("total_review_feedback") != null ? doc.getString("total_review_feedback") : "",
                                doc.getString("answer_feedback") != null ? doc.getString("answer_feedback") : "",
                                doc.getString("ai_analysis_feedback") != null ? doc.getString("ai_analysis_feedback") : "",
                                doc.getString("voice_feedback") != null ? doc.getString("voice_feedback") : ""
                        );

                        feedbackList.add(item);
                    }

                    // 날짜 헤더 추가
                    addDateHeaders();
                    adapter.notifyDataSetChanged();
                    });
                }
    private void addDateHeaders() {
        List<String> dates = new ArrayList<>();
        for (FeedBackItem item : feedbackList) {
            if (!dates.contains(item.date)) {
                dates.add(item.date);
            }
        }

        Collections.sort(dates);

        List<FeedBackItem> finalList = new ArrayList<>();
        for (String d : dates) {

            finalList.add(new FeedBackItem(FeedBackItem.Type.DATE, d));

            for (FeedBackItem f : feedbackList) {
                if (f.date.equals(d)) {
                    finalList.add(f);
                }
            }
        }

        feedbackList.clear();
        feedbackList.addAll(finalList);
    }
}
