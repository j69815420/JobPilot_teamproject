package com.example.jobpilot;

import android.app.Application;
import com.kakao.sdk.common.KakaoSdk;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

// Kakao SDK를 초기화하기 위해 필요한 클래스
public class KakaoLogin extends Application {
    public void onCreate() {
        super.onCreate();
        KakaoSdk.init(this, "fcb137cc3f4c089c47b2f24471cd88ee");

        // PDFBox 초기화 추가
        PDFBoxResourceLoader.init(getApplicationContext());
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

}
