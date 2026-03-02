package com.example.jobpilot;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class ScoreAnalysisFragment extends Fragment {

    private View[] bars = new View[7];
    private TextView[] scores = new TextView[7];
    private TextView[] dates = new TextView[7];
    private MaterialCalendarView calendarView;

    private LocalDate centerDate;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_score_analysis, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        centerDate = LocalDate.now();

        // UI 요소 초기화
        initializeViews(view);

        // 초기 데이터 로드
        loadScoresForWeek(centerDate);

        // 달력 날짜 클릭 이벤트
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            java.util.Calendar calendar = date.getCalendar();
            centerDate = LocalDate.of(
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH) + 1,  // 0-11이므로 +1 필요
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );
            loadScoresForWeek(centerDate);
        });

        return view;
    }

    private void initializeViews(View view) {
        for (int i = 0; i < 7; i++) {
            int scoreId = getResources().getIdentifier("score" + (i + 1), "id",
                    getActivity().getPackageName());
            int barId = getResources().getIdentifier("bar" + (i + 1), "id",
                    getActivity().getPackageName());
            int dateId = getResources().getIdentifier("date" + (i + 1), "id",
                    getActivity().getPackageName());

            scores[i] = view.findViewById(scoreId);
            bars[i] = view.findViewById(barId);
            dates[i] = view.findViewById(dateId);
        }
    }

    private void loadScoresForWeek(LocalDate centerDate) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid == null) {
            return;
        }

        LocalDate startDate = centerDate.minusDays(3);
        LocalDate endDate = centerDate.plusDays(3);

        // Firestore에서 날짜 범위로 조회
        db.collection("live_interviews")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<LocalDate, Integer> scoreMap = new HashMap<>();
                    Map<LocalDate, Date> latestDateMap = new HashMap<>();  // 이 줄 추가!

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Date firebaseDate = doc.getDate("date");
                            if (firebaseDate != null) {
                                LocalDate date = firebaseDate.toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate();

                                // 날짜 범위 확인
                                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                                    int totalScore = doc.getLong("total_score") != null ?
                                            doc.getLong("total_score").intValue() : 0;

                                    // 같은 날짜가 이미 있는 경우, 더 최근 것으로 업데이트
                                    Date existingDate = latestDateMap.get(date);
                                    if (existingDate == null || firebaseDate.after(existingDate)) {
                                        scoreMap.put(date, totalScore);
                                        latestDateMap.put(date, firebaseDate);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    updateBarsWithData(scoreMap, startDate);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }
    private void updateBarsWithData(Map<LocalDate, Integer> scoreMap, LocalDate startDate) {
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            Integer score = scoreMap.getOrDefault(date, 0);

            scores[i].setText(score + "점");
            dates[i].setText(String.valueOf(date.getDayOfMonth()));

            updateBarHeight(i, score);
        }
    }

    private void updateBarHeight(int index, int score) {
        FrameLayout barFrame = (FrameLayout) bars[index].getParent();

        barFrame.post(() -> {
            int maxHeight = barFrame.getHeight();
            if (maxHeight == 0) {
                barFrame.post(() -> updateBarHeight(index, score));
                return;
            }

            int barHeight = (int) (score / 100.0 * maxHeight);
            if (barHeight < 4) barHeight = 4;

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bars[index].getLayoutParams();
            params.height = barHeight;
            bars[index].setLayoutParams(params);
        });
    }
}