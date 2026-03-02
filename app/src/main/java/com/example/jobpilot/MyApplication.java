package com.example.jobpilot;

import android.app.Application;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

    }
}
