package com.example.jobpilot;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedBackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_DATE = 0;
    private final int TYPE_REAL = 1;
    private final int TYPE_PRACTICE = 2;

    private List<FeedBackItem> itemList;
    private Context context;

    public FeedBackAdapter(Context context, List<FeedBackItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        FeedBackItem.Type type = itemList.get(position).type;
        if (type == FeedBackItem.Type.DATE) return TYPE_DATE;
        else if (type == FeedBackItem.Type.REAL) return TYPE_REAL;
        else return TYPE_PRACTICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_DATE) {
            View v = inflater.inflate(R.layout.item_date, parent, false);
            return new DateViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_feedback, parent, false);
            return new FeedbackViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        FeedBackItem item = itemList.get(position);

        if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).tvDate.setText(item.date);
            return;
        }

        FeedbackViewHolder h = (FeedbackViewHolder) holder;

        // 리스트에 표시되는 텍스트
        h.tvContent.setText(item.mode + " - 점수: " + item.total_score);

        // 클릭 → 상세 페이지 이동
        h.itemView.setOnClickListener(v -> {

            Intent intent;

            if (item.type == FeedBackItem.Type.REAL) {
                intent = new Intent(context, LiveModeScoreActivityForFeedBack.class);

                intent.putExtra("total_score", item.total_score);
                intent.putExtra("answerScore", item.answerScore);
                intent.putExtra("aiScore", item.aiScore);
                intent.putExtra("voiceScore", item.voiceScore);
                intent.putExtra("answer_feedback", item.answerReview);
                intent.putExtra("ai_feedback", item.aiReview);
                intent.putExtra("voice_feedback", item.voiceReview);
                intent.putExtra("review", item.review);

                context.startActivity(intent);
                intent.putExtra("date", item.date);

            }
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // 날짜 헤더
    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }

    // 모드 + 점수
    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;

        FeedbackViewHolder(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tvFeedbackContent);
        }
    }
}
