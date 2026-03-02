package com.example.jobpilot;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DeleteAccountActivity extends AppCompatActivity {

    private static final String TAG = "DeleteAccountActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String kakaoAccessToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button deleteAccountBtn = findViewById(R.id.delete_account_button);
        deleteAccountBtn.setOnClickListener(v -> deleteAccount());
    }

    private void deleteAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "로그인된 사용자 없음");
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.d(TAG, "Firestore 사용자 문서 없음");
                        return;
                    }

                    String loginProvider = document.getString("loginProvider");
                    if ("kakao".equals(loginProvider)) {
                        // 카카오 탈퇴
                        sendKakaoUnlinkToServer(kakaoAccessToken);
                        db.collection("users").document(currentUser.getUid()).delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore 삭제 완료"))
                                .addOnFailureListener(e -> Log.e(TAG, "Firestore 삭제 실패", e));
                    } else if ("google".equals(loginProvider) || "email".equals(loginProvider)) {
                        // 구글/이메일 탈퇴
                        db.collection("users").document(currentUser.getUid()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Firestore 삭제 완료");
                                    currentUser.delete()
                                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Firebase 계정 삭제 완료"))
                                            .addOnFailureListener(e -> {
                                                if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                                    Toast.makeText(this, "최근 로그인 후 탈퇴 가능합니다.", Toast.LENGTH_SHORT).show();
                                                }
                                                Log.e(TAG, "Firebase 계정 삭제 실패", e);
                                            });
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Firestore 삭제 실패", e));
                    }

                    // 탈퇴 후 로그아웃
                    mAuth.signOut();
                    kakaoAccessToken = null;
                    Log.d(TAG, "앱 로컬 로그아웃 완료");
                    Toast.makeText(this, "회원 탈퇴 완료", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore 사용자 조회 실패", e));
    }

    private void sendKakaoUnlinkToServer(String accessToken){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://192.168.219.103:5000/api/kakao/unlink")
                .delete()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) Log.d(TAG, "카카오 탈퇴 서버 응답: " + body.string());
                Log.d(TAG, "응답 코드: " + response.code());
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "카카오 탈퇴 서버 전송 실패", e);
            }
        });
    }
}

