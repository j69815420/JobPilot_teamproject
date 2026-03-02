package com.example.jobpilot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private EditText etSignupId, etSignupPassword, etSignupPasswordConfirm, etSignupEmail;
    private Spinner spinnerEmailDomain;
    private Button btnCheckDuplicate, btnSignup;
    private boolean isIdChecked = false;
    private String checkedId = "";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etSignupId = findViewById(R.id.et_signup_id);
        etSignupPassword = findViewById(R.id.et_signup_password);
        etSignupPasswordConfirm = findViewById(R.id.et_signup_password_confirm);
        etSignupEmail = findViewById(R.id.et_signup_email);
        spinnerEmailDomain = findViewById(R.id.spinner_email_domain);
        btnCheckDuplicate = findViewById(R.id.btn_check_duplicate);
        btnSignup = findViewById(R.id.btn_signup);

        prefs = getSharedPreferences("user_info", MODE_PRIVATE);

        // 이메일 도메인 스피너 세팅
        String[] emailDomains = {"선택하세요", "naver.com", "gmail.com", "daum.net"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, R.id.spinner_text, emailDomains);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerEmailDomain.setAdapter(adapter);

        // 중복확인 버튼 클릭
        btnCheckDuplicate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = etSignupId.getText().toString().trim();
                if (TextUtils.isEmpty(id)) {
                    Toast.makeText(SignupActivity.this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("id", id)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult() != null && !task.getResult().isEmpty()) {
                                    Toast.makeText(SignupActivity.this, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show();
                                    isIdChecked = false;
                                    checkedId = "";
                                } else {
                                    Toast.makeText(SignupActivity.this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show();
                                    isIdChecked = true;
                                    checkedId = id;
                                }
                            } else {
                                Toast.makeText(SignupActivity.this, "아이디 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "아이디 중복 확인 실패", task.getException());
                                isIdChecked = false;
                                checkedId = "";
                            }
                        });
            }
        });

        // 회원가입 버튼 클릭
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = etSignupId.getText().toString().trim();
                String password = etSignupPassword.getText().toString();
                String passwordConfirm = etSignupPasswordConfirm.getText().toString();
                String email = etSignupEmail.getText().toString().trim();
                String emailDomain = spinnerEmailDomain.getSelectedItem().toString();

                // 입력값 검증
                if (TextUtils.isEmpty(id) || TextUtils.isEmpty(password) || TextUtils.isEmpty(passwordConfirm)
                        || TextUtils.isEmpty(email) || emailDomain.equals("선택하세요")) {
                    Toast.makeText(SignupActivity.this, "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 비밀번호 일치 확인
                if (!password.equals(passwordConfirm)) {
                    Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 비밀번호 최소 6자리 체크
                if (password.length() < 6) {
                    Toast.makeText(SignupActivity.this, "비밀번호는 6자리 이상이어야 합니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 아이디 중복확인
                if (!isIdChecked) {
                    Toast.makeText(SignupActivity.this, "아이디 중복확인을 해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 이메일 완성
                String fullEmail = email + "@" + emailDomain;

                AuthHelper authHelper = new AuthHelper(SignupActivity.this);

                // Firestore 저장
                authHelper.signUpWithEmail(id, password, fullEmail,
                        () -> { // 성공
                            Toast.makeText(SignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        () -> { // 실패
                            Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                        }
                );
            }
        });
    }

    // Firebase 인증 + Firestore 연동을 도와주는 Helper 클래스
    public class AuthHelper {
        private FirebaseAuth mAuth;
        private FirebaseFirestore db;
        private Context context;

        public AuthHelper(Context context) {
            this.context = context;
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        }

        public void signUpWithEmail(String id, String password, String fullEmail, Runnable onSuccess, Runnable onFailure) {
            Log.d(TAG, " 회원가입 시작: id=" + id + ", email=" + fullEmail);

            mAuth.createUserWithEmailAndPassword(fullEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                Log.d(TAG, " Firebase Auth 회원가입 성공: uid=" + uid);

                                Map<String, Object> userData = new HashMap<>();
                                userData.put("uid", uid);
                                userData.put("id", id);
                                userData.put("email", fullEmail);
                                userData.put("password", password);  // 비밀번호 저장 추가
                                userData.put("loginProvider", "ID/PW");
                                userData.put("createdAt", FieldValue.serverTimestamp());
                                userData.put("updatedAt", FieldValue.serverTimestamp());

                                db.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, " Firestore 저장 성공");

                                            // SharedPreferences 저장
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString("uid", uid);
                                            editor.putString("user_id", id);
                                            editor.putString("user_email", fullEmail);
                                            editor.putString("login_type", "normal");
                                            editor.apply();

                                            Log.d(TAG, " SharedPreferences 저장 완료");

                                            // InterestActivity로 이동 (UID 전달)
                                            Intent intent = new Intent(SignupActivity.this, InterestActivity.class);
                                            intent.putExtra("uid", uid);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);

                                            if (onSuccess != null) onSuccess.run();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, " Firestore 저장 실패", e);
                                            user.delete();
                                            Toast.makeText(context, "회원 정보 저장 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                            if (onFailure != null) onFailure.run();
                                        });
                            }
                        } else {
                            Log.e(TAG, " Firebase Auth 회원가입 실패", task.getException());
                            if (onFailure != null) onFailure.run();
                        }
                    });
        }
    }
}