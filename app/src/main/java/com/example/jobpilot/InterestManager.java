package com.example.jobpilot;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterestManager {
    private static final String TAG = "InterestManager";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public InterestManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    // uid 조회 (Firebase Auth만 사용)
    private void getUserUid(OnSuccessListener<String> callback) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            // 구글, 이메일 로그인
            Log.d(TAG, " Firebase Auth UID: " + firebaseUser.getUid());
            callback.onSuccess(firebaseUser.getUid());
        } else {
            //  Firebase Auth 없음
            Log.e(TAG, " Firebase Auth 사용자 없음");
            callback.onSuccess(null);
        }
    }

    // 1. 유저관심분야 저장 (콜백 있음)
    public void saveUserInterests(List<String> interests, Runnable onSuccess) {
        getUserUid(uid -> {
            if (uid == null) {
                Log.e(TAG, " UID가 없어서 저장 불가");
                return;
            }

            Log.d(TAG, " 관심분야 저장 시작: " + uid);
            Log.d(TAG, " 저장할 데이터: " + interests.toString());

            Map<String, Object> data = new HashMap<>();
            data.put("interests", interests);

            db.collection("users").document(uid)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, " 관심분야 update 성공");
                        onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, " update 실패, set으로 재시도", e);
                        db.collection("users").document(uid)
                                .set(data)
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.d(TAG, " 관심분야 set 성공");
                                    onSuccess.run();
                                })
                                .addOnFailureListener(e1 -> {
                                    Log.e(TAG, " 관심분야 저장 완전 실패", e1);
                                });
                    });
        });
    }

    // 2. 유저관심분야 저장 (콜백 없음)
    public void saveUserInterests(List<String> interests) {
        saveUserInterests(interests, () -> {
            Log.d(TAG, "관심분야 저장 완료 (콜백 없음)");
        });
    }

    // 3. 유저 관심분야 불러오기
    public void fetchUserInterests(OnSuccessListener<List<String>> onSuccess, OnFailureListener onFailure) {
        getUserUid(uid -> {
            if (uid == null) {
                Log.e(TAG, "로그인된 사용자가 없습니다.");
                onFailure.onFailure(new Exception("로그인된 사용자 없음"));
                return;
            }

            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            List<String> interests = (List<String>) document.get("interests");
                            onSuccess.onSuccess(interests);
                        } else {
                            Log.e(TAG, "사용자 문서가 존재하지 않음");
                            onFailure.onFailure(new Exception("사용자 문서 없음"));
                        }
                    })
                    .addOnFailureListener(onFailure);
        });
    }

    // 4. 관심분야 기반 질문 조회
    public void fetchQuestionsByUserInterests(OnSuccessListener<List<String>> onSuccess, OnFailureListener onFailure) {
        getUserUid(uid -> {
            if (uid == null) {
                Log.e(TAG, "로그인된 사용자가 없습니다.");
                onFailure.onFailure(new Exception("로그인된 사용자 없음"));
                return;
            }

            // 사용자 관심분야 불러오기
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            List<String> interests = (List<String>) document.get("interests");

                            if (interests == null || interests.isEmpty()) {
                                onFailure.onFailure(new Exception("관심분야가 없습니다."));
                                return;
                            }

                            // 관심분야 기반 질문 검색
                            db.collection("questions")
                                    .whereIn("interests", interests)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        List<String> questions = new ArrayList<>();
                                        for (DocumentSnapshot doc : querySnapshot) {
                                            String q = doc.getString("question");
                                            if (q != null) questions.add(q);
                                        }
                                        onSuccess.onSuccess(questions);
                                    })
                                    .addOnFailureListener(onFailure);

                        } else {
                            onFailure.onFailure(new Exception("사용자 문서 없음"));
                        }
                    })
                    .addOnFailureListener(onFailure);
        });
    }
}
