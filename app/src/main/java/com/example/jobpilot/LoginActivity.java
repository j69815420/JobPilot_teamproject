package com.example.jobpilot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etId, etPassword;
    private TextView tvSignup, tvFindId, tvFindPw;
    private Button btnLogin, btnKakaoLogin, btnGoogleLogin;
    private LinearLayout loginContent;
    private ImageView loadingImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI 초기화
        etId = findViewById(R.id.et_id);
        etPassword = findViewById(R.id.et_password);
        tvSignup = findViewById(R.id.tv_signup);
        tvFindId = findViewById(R.id.tv_find_id);
        tvFindPw = findViewById(R.id.tv_find_pw);
        btnLogin = findViewById(R.id.btn_login);
        btnKakaoLogin = findViewById(R.id.btn_kakao_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        loginContent = findViewById(R.id.loginContent);
        loadingImage = findViewById(R.id.iv_loading);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 카카오 초기화
        KakaoSdk.init(this, "d38b869f4df76106f82925eaaf871ffa");

        // Google 로그인 초기화
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 회원가입 이동
        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, TermsActivity.class))
        );

        // 아이디 찾기
        tvFindId.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindIdActivity.class))
        );

        // 비밀번호 찾기
        tvFindPw.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindPasswordActivity.class))
        );

        // 일반 로그인 버튼 클릭
        btnLogin.setOnClickListener(v -> {
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firestore 기반 로그인 (원래 방식대로)
            db.collection("users")
                    .whereEqualTo("id", id)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                            String email = userDoc.getString("email");
                            String storedPw = userDoc.getString("password");
                            String docId = userDoc.getId();

                            if (storedPw != null && storedPw.equals(password)) {
                                Log.d(TAG, "비밀번호 일치");

                                // SharedPreferences 저장
                                SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                                prefs.edit()
                                        .putString("uid", docId)
                                        .putString("user_id", id)
                                        .putString("user_email", email)
                                        .putString("login_type", "normal")
                                        .apply();

                                Log.d(TAG, "SharedPreferences 저장 완료 - uid: " + docId);

                                // Firebase Auth 로그인 시도 (선택사항)
                                if (email != null && !email.isEmpty()) {
                                    mAuth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(authTask -> {
                                                if (authTask.isSuccessful()) {
                                                    Log.d(TAG, "Firebase Auth 로그인 성공");
                                                } else {
                                                    Log.w(TAG, "Firebase Auth 로그인 실패, Firestore만 사용", authTask.getException());
                                                }
                                                // 성공/실패 관계없이 관심분야 확인
                                                checkInterestsAndNavigate(docId);
                                            });
                                } else {
                                    checkInterestsAndNavigate(docId);
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "존재하지 않는 아이디입니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "로그인 실패", e);
                        Toast.makeText(LoginActivity.this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Google 로그인
        btnGoogleLogin.setOnClickListener(v -> {
            Log.d(TAG, "Google 로그인 버튼 클릭");
            signInWithGoogle();
        });

        // Kakao 로그인
        btnKakaoLogin.setOnClickListener(v -> {
            Log.d(TAG, "Kakao 로그인 버튼 클릭");

            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
                UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                    if (error != null) {
                        Log.e(TAG, "카카오톡 로그인 실패", error);
                        UserApiClient.getInstance().loginWithKakaoAccount(this, (token2, error2) -> {
                            if (error2 != null) {
                                Log.e(TAG, "카카오 계정 로그인 실패", error2);
                                Toast.makeText(this, "로그인 실패: " + error2.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "카카오 계정 로그인 성공");
                                UserApiClient.getInstance().me((user, meError) -> {
                                    if (user != null) {
                                        firebaseLoginWithKakao(String.valueOf(user.getId()));
                                    }
                                    return null;
                                });
                            }
                            return null;
                        });
                    } else {
                        Log.d(TAG, "카카오톡 로그인 성공");
                        UserApiClient.getInstance().me((user, meError) -> {
                            if (user != null) {
                                firebaseLoginWithKakao(String.valueOf(user.getId()));
                            }
                            return null;
                        });
                    }
                    return null;
                });
            } else {
                UserApiClient.getInstance().loginWithKakaoAccount(this, (token, error) -> {
                    if (error != null) {
                        Log.e(TAG, "카카오 계정 로그인 실패", error);
                        Toast.makeText(this, "로그인 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "카카오 계정 로그인 성공");
                        UserApiClient.getInstance().me((user, meError) -> {
                            if (user != null) {
                                firebaseLoginWithKakao(String.valueOf(user.getId()));
                            }
                            return null;
                        });
                    }
                    return null;
                });
            }
        });
    }

    // SharedPreferences 저장
    private void saveToSharedPreferences(String uid, String userId, String email) {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        prefs.edit()
                .putString("uid", uid)
                .putString("user_id", userId)
                .putString("user_email", email)
                .putString("login_type", "normal")
                .apply();
        Log.d(TAG, "SharedPreferences 저장 완료 - uid: " + uid);
    }

    // 관심분야 확인 후 화면 이동
    private void checkInterestsAndNavigate(String docId) {
        Log.d(TAG, "관심분야 확인 시작 - docId: " + docId);

        db.collection("users").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (doc.contains("interests")) {
                            List<String> interests = (List<String>) doc.get("interests");
                            if (interests != null && !interests.isEmpty()) {
                                Log.d(TAG, "관심분야 있음 (" + interests.size() + "개) → 홈으로 이동");
                                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                                navigateToHome();
                                return;
                            }
                        }
                        // 관심분야 없음
                        Log.d(TAG, "관심분야 없음 → InterestActivity로 이동");
                        Intent intent = new Intent(LoginActivity.this, InterestActivity.class);
                        intent.putExtra("uid", docId);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Firestore 문서 없음");
                        Toast.makeText(LoginActivity.this, "계정 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "관심분야 확인 실패", e);
                    Toast.makeText(LoginActivity.this, "정보 조회 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                });
    }

    // Google 로그인 실행
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Google 인증
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user, "google");
                        }
                    } else {
                        Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Firestore 저장 (Google/Kakao 공통)
    private void saveUserToFirestore(FirebaseUser firebaseUser, String provider) {
        String uid = firebaseUser.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("id", firebaseUser.getDisplayName());
        userData.put("email", firebaseUser.getEmail());
        userData.put("loginProvider", provider);
        userData.put("lastLogin", FieldValue.serverTimestamp());

        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // 기존 사용자
                if (documentSnapshot.contains("interests")) {
                    List<String> interests = (List<String>) documentSnapshot.get("interests");
                    if (interests != null && !interests.isEmpty()) {
                        Log.d(TAG, "기존 " + provider + " 사용자, 관심분야 있음 → 홈으로");
                        userRef.set(userData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                                    navigateToHome();
                                });
                        return;
                    }
                }
                // 기존 사용자지만 관심분야 없음
                Log.d(TAG, "기존 " + provider + " 사용자, 관심분야 없음 → InterestActivity로");
                userRef.set(userData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(LoginActivity.this, InterestActivity.class);
                            intent.putExtra("uid", uid);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
            } else {
                // 신규 사용자
                Log.d(TAG, "신규 " + provider + " 사용자 → InterestActivity로");
                userRef.set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(LoginActivity.this, InterestActivity.class);
                            intent.putExtra("uid", uid);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore 등록 실패", e);
                            Toast.makeText(this, "회원 정보 저장 실패", Toast.LENGTH_SHORT).show();
                        });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore 조회 실패", e);
            Toast.makeText(this, "회원 정보 확인 실패", Toast.LENGTH_SHORT).show();
        });
    }

    // 카카오 로그인 Firestore 저장
    private void saveKakaoUserToFirestore() {
        UserApiClient.getInstance().me((user, meError) -> {
            if (user != null) {
                String kakaoId = String.valueOf(user.getId());
                String email = user.getKakaoAccount().getEmail() != null ?
                        user.getKakaoAccount().getEmail() : "no_email";
                String nickname = user.getKakaoAccount().getProfile() != null ?
                        user.getKakaoAccount().getProfile().getNickname() : "no_name";

                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser == null) {
                    Log.e(TAG, "Firebase 사용자 로그인 정보 없음");
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    return null;
                }
                String firebaseUid = firebaseUser.getUid();

                SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
                prefs.edit()
                        .putString("uid", firebaseUid)
                        .putString("kakaoId", kakaoId)
                        .putString("user_id", nickname)
                        .putString("user_email", email)
                        .putString("loginProvider", "kakao")
                        .apply();

                Map<String, Object> userData = new HashMap<>();
                userData.put("id", nickname);
                userData.put("email", email);
                userData.put("loginProvider", "kakao");
                userData.put("kakaoId", kakaoId);
                userData.put("lastLogin", FieldValue.serverTimestamp());

                DocumentReference userRef = db.collection("users").document(firebaseUid);

                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("interests")) {
                            List<String> interests = (List<String>) documentSnapshot.get("interests");
                            if (interests != null && !interests.isEmpty()) {
                                Log.d(TAG, "기존 카카오 사용자, 관심분야 있음 → 홈으로");
                                userRef.set(userData, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                                            navigateToHome();
                                        });
                                return;
                            }
                        }
                        Log.d(TAG, "기존 카카오 사용자, 관심분야 없음 → InterestActivity로");
                        userRef.set(userData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Intent intent = new Intent(LoginActivity.this, InterestActivity.class);
                                    intent.putExtra("uid", firebaseUid);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        Log.d(TAG, "신규 카카오 사용자 → InterestActivity로");
                        userRef.set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Intent intent = new Intent(LoginActivity.this, InterestActivity.class);
                                    intent.putExtra("uid", firebaseUid);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore 등록 실패", e);
                                    Toast.makeText(this, "회원 정보 저장 실패", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
            }
            return null;
        });
    }

    private void firebaseLoginWithKakao(String kakaoId) {
        String fakeEmail = "kakao_" + kakaoId + "@fake.com";
        String fakePassword = "kakao_default_pw";

        mAuth.signInWithEmailAndPassword(fakeEmail, fakePassword)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase 로그인 성공");
                    saveKakaoUserToFirestore();
                })
                .addOnFailureListener(e -> {
                    mAuth.createUserWithEmailAndPassword(fakeEmail, fakePassword)
                            .addOnSuccessListener(result -> {
                                Log.d(TAG, "Firebase 계정 생성 성공");
                                saveKakaoUserToFirestore();
                            })
                            .addOnFailureListener(err -> {
                                Log.e(TAG, "Firebase 생성 실패", err);
                                Toast.makeText(this, "Firebase 로그인 실패", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.e(TAG, "Google 로그인 실패: " + e.getStatusCode(), e);
            }
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, NavigationActivity.class);
        startActivity(intent);
        finish();
    }
}