package com.example.jobpilot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyInfoEditActivity extends AppCompatActivity {

    private static final String TAG = "MyInfoEditActivity";
    private static final String SERVER_URL = "http://192.168.219.102:5000";  // 실제 기기용

    private EditText editId, editEmail, editBirth;
    private boolean isSocialLogin = false;  // 소셜 로그인 여부
    private LinearLayout selectedFieldsContainer;
    private LinearLayout linearInterests;
    private Button btnUpdateInfo, btnBack;

    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private final List<String> selectedInterests = new ArrayList<>();
    private final int MAX_SELECTION = 3;  // 관심분야 최대 선택 수

    private FirebaseUser currentUser;
    private EditText editCurrentPassword;

    private boolean isInterestButtonsSetup = false; // 중복 설정 방지 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_edit);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("user_info", MODE_PRIVATE);

        editId = findViewById(R.id.editId);
        editEmail = findViewById(R.id.editEmail);
        editBirth = findViewById(R.id.editBirth);
        selectedFieldsContainer = findViewById(R.id.selected_fields_container);
        linearInterests = findViewById(R.id.linearInterests);
        btnUpdateInfo = findViewById(R.id.btnUpdateInfo);
        btnBack = findViewById(R.id.btnBack);
        editCurrentPassword = findViewById(R.id.editCurrentPassword);

        currentUser = mAuth.getCurrentUser();

        // Firebase에서 현재 사용자 정보 불러오기
        loadUserInfoFromFirebase();

        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());

        // 정보 수정 버튼 클릭
        btnUpdateInfo.setOnClickListener(v -> {
            String currentPassword = editCurrentPassword.getText().toString().trim();
            updateUserInfo(currentPassword);  // 입력한 비밀번호 포함하여 업데이트
        });
    }

    // Firebase Auth에서 ID Token을 가져와 서버로부터 사용자 정보 불러오기
    private void loadUserInfoFromFirebase() {
        String userId = null;

        if (currentUser != null) {
            userId = currentUser.getUid();

            // 소셜 로그인인지 체크 (Google/Kakao 등)
            isSocialLogin = false;
            for (int i = 0; i < currentUser.getProviderData().size(); i++) {
                String providerId = currentUser.getProviderData().get(i).getProviderId();
                Log.d(TAG, "Provider ID: " + providerId);
                if (providerId.equals("google.com") || providerId.equals("kakao.com")) {
                    isSocialLogin = true;
                    break;
                }
            }
        } else {
            // SharedPreferences에서 UID 가져오기 (카카오 로그인 등)
            userId = prefs.getString("uid", null);
            isSocialLogin = true; // SharedPreferences UID만 있는 경우 소셜 로그인 아님 처리
            if (userId != null) {
                Log.d(TAG, "SharedPreferences UID 사용자: " + userId);
            }
        }

        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isSocialLogin) {
            // 소셜 로그인 시 ID, 이메일, 비밀번호 입력 비활성화
            editEmail.setEnabled(false);
            editId.setEnabled(false);
            editCurrentPassword.setEnabled(false);
            editCurrentPassword.setVisibility(View.GONE);
            Toast.makeText(this, "소셜 로그인 계정은 ID와 이메일을 변경할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        String finalUserId = userId;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Firestore에서 필드 가져오기
                        String username = documentSnapshot.getString("id");
                        String email = documentSnapshot.getString("email");
                        String birth = documentSnapshot.getString("birth");

                        // EditText에 표시
                        editId.setText(username != null ? username : "");
                        editEmail.setText(email != null ? email : "");
                        editBirth.setText(birth != null ? birth : "");

                        // 관심분야도 배열로 저장돼 있다면 selectedInterests에 저장
                        List<String> interests = (List<String>) documentSnapshot.get("interests");
                        selectedInterests.clear(); // 기존 리스트 비우기
                        if (interests != null && !interests.isEmpty()) {
                            selectedInterests.addAll(interests);
                            Log.d(TAG, "Loaded interests count: " + selectedInterests.size() + ", List: " + selectedInterests.toString());
                        } else {
                            Log.d(TAG, "No interests found in Firestore");
                        }

                        // 관심분야 버튼 UI 초기화 (한 번만 실행)
                        if (!isInterestButtonsSetup) {
                            setupInterestButtons();
                            isInterestButtonsSetup = true;
                        } else {
                            // 이미 설정된 경우 상태만 업데이트
                            updateInterestButtonStates();
                        }

                    } else {
                        Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore load failed", e);
                });
    }

    // Firestore 업데이트
    private void saveToFirestore(String id, String email, String birth) {
        String userId = null;
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            userId = prefs.getString("uid", null);
        }
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 관심분야 최소 1개 선택 필수
        if (selectedInterests.isEmpty()) {
            Toast.makeText(this, "관심분야를 최소 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("id", id);  // 아이디 (Firestore만)
        updates.put("email", email);  // 이메일 동기화
        updates.put("birth", birth);  // 생일
        updates.put("interests", new ArrayList<>(selectedInterests)); // 선택된 관심분야 저장

        Log.d(TAG, "Saving to Firestore - interests: " + selectedInterests.toString());

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore 업데이트 완료");
                    runOnUiThread(() -> {
                        Toast.makeText(MyInfoEditActivity.this, "정보가 변경되었습니다.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore 업데이트 실패", e);
                    runOnUiThread(() -> Toast.makeText(this, "정보 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    // 사용자 정보 업데이트 (이메일 변경 시 재인증 필요)
    private void updateUserInfo(String currentPasswordForAuth) {
        String newId = editId.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();
        String birth = editBirth.getText().toString().trim();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSocialLogin) {
            // 현재 표시된 값 그대로 저장 (수정 불가이므로)
            String currentId = editId.getText().toString().trim();
            String currentEmail = editEmail.getText().toString().trim();

            saveToFirestore(currentId, currentEmail, birth);  //  EditText에서 가져옴
            return;
        }

        // 일반 로그인: ID/이메일/생일 업데이트 가능
        if (newId.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "아이디와 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "올바른 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPasswordForAuth.isEmpty()) {
            Toast.makeText(this, "정보 수정 시 현재 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이메일 변경 요청
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPasswordForAuth);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> currentUser.updateEmail(newEmail)
                        .addOnSuccessListener(unused -> saveToFirestore(newId, newEmail, birth))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "이메일 변경 실패", e);
                            Toast.makeText(this, "이메일 변경 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "재인증 실패", e);
                    Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // 관심분야 버튼 세팅 (한 번만 실행)
    private void setupInterestButtons() {
        Log.d(TAG, "=== setupInterestButtons START ===");
        int count = linearInterests.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = linearInterests.getChildAt(i);
            if (view instanceof Button) {
                Button btn = (Button) view;
                String text = btn.getText().toString();
                updateButtonState(btn, text);
                // 클릭 리스너 설정
                btn.setOnClickListener(v -> {
                    if (selectedInterests.contains(text)) selectedInterests.remove(text);
                    else if (selectedInterests.size() < MAX_SELECTION) selectedInterests.add(text);
                    else {
                        Toast.makeText(this, "최대 3개까지만 선택 가능합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateButtonState(btn, text);
                    updateSelectedFieldsViews();
                });
            }
        }
        updateSelectedFieldsViews();
        Log.d(TAG, "=== setupInterestButtons END ===");
    }

    // 버튼 상태만 업데이트 (리스너 재등록 없이)
    private void updateInterestButtonStates() {
        int count = linearInterests.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = linearInterests.getChildAt(i);
            if (view instanceof Button) {
                Button btn = (Button) view;
                String text = btn.getText().toString();
                updateButtonState(btn, text);
            }
        }
        updateSelectedFieldsViews();
    }

    // 개별 버튼 상태 업데이트
    private void updateButtonState(Button btn, String text) {
        boolean isSelected = selectedInterests.contains(text);
        if (isSelected) {
            btn.setBackground(ContextCompat.getDrawable(this, R.drawable.interest_selected));
            btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_purple, 0);
        } else {
            btn.setBackground(ContextCompat.getDrawable(this, R.drawable.interest_unselected));
            btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    // 선택된 관심분야 TextView 업데이트
    private void updateSelectedFieldsViews() {
        selectedFieldsContainer.removeAllViews();
        if (selectedInterests.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("1개 이상 분야를 선택해 주세요");
            tv.setTextColor(0xFF888888);
            selectedFieldsContainer.addView(tv);
        } else {
            for (String interest : selectedInterests) {
                TextView tv = new TextView(this);
                tv.setText(interest);
                tv.setTextColor(0xFF7B68EE);
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.selected_fields_border));
                tv.setPadding(20, 10, 20, 10);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 8, 8, 8);
                tv.setLayoutParams(params);
                selectedFieldsContainer.addView(tv);
            }
        }
    }
}
