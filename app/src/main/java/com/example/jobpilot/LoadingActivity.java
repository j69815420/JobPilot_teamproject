package com.example.jobpilot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class LoadingActivity extends Activity {

    private ImageView imgLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading);

        imgLoading = findViewById(R.id.loading_icon);

        // PNG 회전 애니메이션
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(1000);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(this, android.R.interpolator.linear);
        imgLoading.startAnimation(rotate);

        // 2초 후 LiveModeScoreActivity로 이동
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(LoadingActivity.this, LiveModeScoreActivity.class);
            intent.putExtra("audioFilePath", getIntent().getStringExtra("audioFilePath"));
            startActivity(intent);
            finish();
        }, 2000);
    }
}
