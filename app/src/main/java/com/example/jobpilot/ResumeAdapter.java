package com.example.jobpilot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResumeAdapter extends RecyclerView.Adapter<ResumeAdapter.ViewHolder> {

    private Context context;
    private List<ResumeItem> items;
    private OnItemClickListener listener;

    // ResumeItem만 전달하도록 수정
    public interface OnItemClickListener {
        void onItemClick(ResumeItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ResumeAdapter(Context context, List<ResumeItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_resume_purplebox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResumeItem item = items.get(position);
        holder.tvDate.setText(item.date);
        holder.tvTitle.setText(item.title);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item); // Context 제거
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle;
        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvResumeDate);
            tvTitle = itemView.findViewById(R.id.tvResumeTitle);
        }
    }
}
