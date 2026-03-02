package com.example.jobpilot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.user.UserApiClient;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.List;


public class MyPageFragment extends Fragment {

    private static final String TAG = "MyPageFragment";
    private static final String SERVER_URL = "http://192.168.219.102:5000"; // 실기기용

    private TextView tvUserInterests;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    public MyPageFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        mAuth = FirebaseAuth.getInstance();
        prefs = getActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);


        tvUserInterests = view.findViewById(R.id.tv_user_interests);
        TextView tvUserName = view.findViewById(R.id.tv_user_name);
        TextView tvUserEmail = view.findViewById(R.id.tv_user_email);
        TextView tvEditInfo = view.findViewById(R.id.tv_edit_info);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnInquiry = view.findViewById(R.id.btn_inquiry);
        Button btnDeleteAccount = view.findViewById(R.id.delete_account_button);
        Switch switchNotification = view.findViewById(R.id.switch_notification);


        tvEditInfo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyInfoEditActivity.class);
            startActivity(intent);
        });

        btnInquiry.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), InquiryActivity.class));
        });

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "알림이 켜졌습니다" : "알림이 꺼졌습니다";
            Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 50);
            toast.show();
        });

        //로그아웃
        btnLogout.setOnClickListener(v -> showBottomSheet(
                "로그아웃",
                "현재 세션에서 로그아웃됩니다. 계속하시겠습니까?",
                "로그아웃",
                this::performLogout
        ));


        //회원탈퇴
        btnDeleteAccount.setOnClickListener(v -> showBottomSheet(
                "회원탈퇴",
                "회원이 탈퇴됩니다. 계속하시겠습니까?",
                "회원탈퇴",
                this::deleteAccount
        ));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();

        // SharedPreferences 불러오기
        String kakaoUid = prefs.getString("uid", null);

        // Firebase Auth가 있으면 → 구글/일반 로그인
        if (firebaseUser != null) {
            String userIdForFirestore = firebaseUser.getUid();
            loadUserInfo(userIdForFirestore, tvUserName, tvUserEmail);
        }
        // 없으면 → 카카오 로그인
        else if (kakaoUid != null) {
            Log.d(TAG, "Firebase Auth 없음, Firestore에서 카카오 사용자 검색");
            loadUserInfo(kakaoUid, tvUserName, tvUserEmail);
        } else {
            //둘 다 없음 -> 로그인 필요
            Log.e(TAG, "로그인 정보 없음");
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        }

        return view;
    }

    //카카오 로그인용 Firestore 조회

    private void findAndLoadKakaoUser(TextView tvUserName, TextView tvUserEmail) {
        SharedPreferences userPrefs = getActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        String kakaoUid = userPrefs.getString("user_id", null);

        if (kakaoUid == null) {
            Log.e(TAG, "카카오 UID 없음, 로그인 필요");
            Toast.makeText(getContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(kakaoUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        loadUserInfo(kakaoUid, tvUserName, tvUserEmail);
                    } else {
                        // Firestore에 사용자 문서 없으면 Firebase Auth 임시 계정 생성
//                        firebaseLoginWithKakao(kakaoUid); // 여기서 아까 작성한 임시 로그인/생성 코드 호출
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    // --- 사용자 정보 불러오기 ---
    private void loadUserInfo(String userId, TextView tvUserName, TextView tvUserEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String id = documentSnapshot.getString("id");
                String email = documentSnapshot.getString("email");
                List<String> interestsList = (List<String>) documentSnapshot.get("interests");
                String interests = (interestsList != null && !interestsList.isEmpty())
                        ? String.join(", ", interestsList)
                        : null;

                tvUserName.setText(id != null ? id : "사용자");
                tvUserEmail.setText(email != null ? email : "이메일 없음");
                tvUserInterests.setText("관심 분야 : " + (interests != null ? interests : "선택된 관심분야 없음"));
            } else {
                Toast.makeText(getContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserInterests();
    }

    // --- 관심분야 새로고침 ---
    private void refreshUserInterests() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String userId = null;

        if (firebaseUser != null) {
            // 구글/일반 로그인
            userId = firebaseUser.getUid();
        } else {
            // uid로 가져오기
            userId = prefs.getString("uid", null);

            if (userId != null) {
                refreshInterestsByUserId(userId);   // 문서 ID로 직접 조회하도록 수정
            }
        }


    }


    private void refreshInterestsByUserId(String userId) {
        if (userId == null || tvUserInterests == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> interestsList = (List<String>) documentSnapshot.get("interests");
                        String interests = (interestsList != null && !interestsList.isEmpty())
                                ? String.join(", ", interestsList)
                                : null;

                        tvUserInterests.setText("관심 분야 : " + (interests != null ? interests : "선택된 관심분야 없음"));
                    }
                });
    }

    private void performLogout() {
        Log.d(TAG, "=== 로그아웃 시작 ===");

        mAuth.signOut();
        Log.d(TAG, "Firebase Auth 로그아웃 완료");

        UserApiClient.getInstance().logout(error -> {
            if (error != null) {
                Log.e(TAG, "카카오 로그아웃 실패 (무시 가능)", error);
            } else {
                Log.d(TAG, "카카오 로그아웃 성공");
            }
            return null;
        });

        prefs.edit().clear().apply();
        Log.d(TAG, "SharedPreferences 초기화 완료");

        Toast.makeText(getContext(), "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();

        Log.d(TAG, "=== 로그아웃 완료 ===");
    }

    // --- 회원 탈퇴 ---
    private void deleteAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String kakaoUid = prefs.getString("uid", null);

        // Firebase 로그인 사용자
        if (currentUser != null) {
            Log.d(TAG, "Firebase 사용자 탈퇴 - UID: " + currentUser.getUid());
            deleteFirebaseUser(currentUser);
        }
        // 카카오 로그인 사용자
        else if (kakaoUid != null) {
            Log.d(TAG, "카카오 사용자 탈퇴 - UID: " + kakaoUid);
            deleteKakaoUser(kakaoUid);
        }
        // 로그인 정보 없음
        else {
            Log.e(TAG, "로그인된 사용자 없음");
            Toast.makeText(getContext(), "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Firebase 사용자 탈퇴 (API 호출) ---
    private void deleteFirebaseUser(FirebaseUser currentUser) {
        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String idToken = result.getToken();
                    Log.d(TAG, "Firebase Token 획득 성공");

                    // API 서버에 요청
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(SERVER_URL + "/api/user/delete")
                            .delete()
                            .addHeader("Authorization", "Bearer " + idToken)
                            .build();

                    Log.d(TAG, "서버 탈퇴 요청 전송: " + SERVER_URL + "/api/user/delete");

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onResponse(Call call, Response response) {
                            try {
                                Log.d(TAG, "📡 서버 응답 코드: " + response.code());

                                if (response.isSuccessful()) {
                                    Log.d(TAG, "서버 탈퇴 성공");

                                    // 서버에서 Firestore + Firebase Auth 삭제 완료!
                                    // 이제 로컬 정리만 하면 됨
                                    getActivity().runOnUiThread(() -> completeAccountDeletion());
                                } else {
                                    String errorMsg = "탈퇴 처리 중 오류가 발생했습니다.";
                                    ResponseBody body = response.body();
                                    if (body != null) {
                                        try {
                                            String responseStr = body.string();
                                            Log.e(TAG, "서버 에러 응답: " + responseStr);
                                            JSONObject json = new JSONObject(responseStr);
                                            errorMsg = json.optString("message", errorMsg);
                                        } catch (Exception e) {
                                            Log.e(TAG, "에러 응답 파싱 실패", e);
                                        }
                                    }
                                    String finalErrorMsg = errorMsg;
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), finalErrorMsg, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            } finally {
                                response.close();
                            }
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "서버 연결 실패", e);
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(),
                                            "서버 연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Token 가져오기 실패", e);
                    Toast.makeText(getContext(), "인증 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // --- 카카오 사용자 탈퇴 (API 호출) ---
    private void deleteKakaoUser(String kakaoUid) {
        try {
            OkHttpClient client = new OkHttpClient();

            // JSON Body 생성
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("uid", kakaoUid);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonBody.toString(),
                    okhttp3.MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/api/user/delete/kakao")
                    .delete(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "카카오 탈퇴 요청 전송 - UID: " + kakaoUid);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        Log.d(TAG, "📡 서버 응답 코드: " + response.code());

                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                String responseStr = responseBody.string();
                                Log.d(TAG, "카카오 탈퇴 성공: " + responseStr);
                            }

                            // 서버에서 Firestore 삭제 완료!
                            // 이제 로컬 정리만 하면 됨
                            getActivity().runOnUiThread(() -> {
                                // 카카오 연결 끊기
                                unlinkKakaoAccount();
                                // 탈퇴 완료 처리
                                completeAccountDeletion();
                            });
                        } else {
                            String errorMsg = "탈퇴 처리 중 오류가 발생했습니다.";
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                try {
                                    String responseStr = responseBody.string();
                                    Log.e(TAG, "서버 에러: " + responseStr);
                                    JSONObject json = new JSONObject(responseStr);
                                    errorMsg = json.optString("message", errorMsg);
                                } catch (Exception e) {
                                    Log.e(TAG, "에러 응답 파싱 실패", e);
                                }
                            }
                            String finalErrorMsg = errorMsg;
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), finalErrorMsg, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "응답 처리 중 예외", e);
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    } finally {
                        response.close();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "카카오 탈퇴 서버 연결 실패", e);
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "서버 연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "카카오 탈퇴 요청 생성 실패", e);
            Toast.makeText(getContext(), "탈퇴 요청 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- 카카오 연결 끊기 (로컬만 처리) ---
    private void unlinkKakaoAccount() {
        UserApiClient.getInstance().unlink(error -> {
            if (error != null) {
                Log.e(TAG, "카카오 연결 끊기 실패 (무시 가능)", error);
            } else {
                Log.d(TAG, "카카오 연결 끊기 성공");
            }
            return null;
        });
    }

    // --- 탈퇴 완료 처리 (로컬 정리만) ---
    private void completeAccountDeletion() {
        Log.d(TAG, "=== 탈퇴 완료 처리 시작 ===");

        // Firebase Auth 로그아웃 (로컬만)
        mAuth.signOut();
        Log.d(TAG, "Firebase Auth 로그아웃 완료");

        // 카카오 로컬 로그아웃
        UserApiClient.getInstance().logout(error -> {
            if (error != null) {
                Log.e(TAG, "카카오 로컬 로그아웃 실패 (무시 가능)", error);
            } else {
                Log.d(TAG, "카카오 로컬 로그아웃 성공");
            }
            return null;
        });

        // SharedPreferences 초기화
        prefs.edit().clear().apply();
        Log.d(TAG, "SharedPreferences 초기화 완료");

        Toast.makeText(getContext(), "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_LONG).show();

        // LoginActivity로 이동
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();

        Log.d(TAG, "=== 탈퇴 완료 처리 종료 ===");
    }

    // --- BottomSheet ---
    private void showBottomSheet(String title, String message, String confirmText, Runnable confirmAction) {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogTheme);

        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_layout, null);
        dialog.setContentView(sheetView);

        dialog.getWindow().setDimAmount(0.3f);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = sheetView.findViewById(R.id.bottom_sheet_title);
        TextView tvMessage = sheetView.findViewById(R.id.bottom_sheet_message);
        Button btnCancel = sheetView.findViewById(R.id.bottom_sheet_cancel);
        Button btnConfirm = sheetView.findViewById(R.id.bottom_sheet_confirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            confirmAction.run();
            dialog.dismiss();
        });

        dialog.show();
    }
}