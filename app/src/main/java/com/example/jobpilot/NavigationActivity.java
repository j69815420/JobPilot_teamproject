package com.example.jobpilot;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 초기 화면: MainFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_main, new MainFragment())
                .commit();

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_interview) {
                    selectedFragment = new MainFragment();
                } else if (itemId == R.id.nav_resume) {
                    selectedFragment = new ResumeFragment();
                } else if (itemId == R.id.nav_graph) {
                    selectedFragment = new ScoreAnalysisFragment();
                } else if (itemId == R.id.nav_mypage) {
                    selectedFragment = new MyPageFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container_main, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });
    }
}
