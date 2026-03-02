package com.example.jobpilot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResumeWriteFragment extends Fragment {

    private EditText etTitle, etGrowth, etStrength, etMotivation, etFuture;
    private Button btnComplete;

    // 데이터 전달 인터페이스
    public interface OnResumeWriteCompleteListener {
        void onResumeWriteComplete(ResumeItem item);
    }

    private OnResumeWriteCompleteListener listener;

    public void setOnResumeWriteCompleteListener(OnResumeWriteCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_resume_write, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etGrowth = view.findViewById(R.id.etGrowth);
        etStrength = view.findViewById(R.id.etStrength);
        etMotivation = view.findViewById(R.id.etMotivation);
        etFuture = view.findViewById(R.id.etFuture);
        btnComplete = view.findViewById(R.id.btnComplete);

        btnComplete.setOnClickListener(v -> {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            ResumeItem item = new ResumeItem(
                    "", // content
                    date,
                    etTitle.getText().toString(),
                    etGrowth.getText().toString(),
                    etStrength.getText().toString(),
                    etMotivation.getText().toString(),
                    etFuture.getText().toString()
            );

            if (listener != null) {
                listener.onResumeWriteComplete(item);
            }

            // 작성 후 Fragment 닫기
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        return view;
    }
}
