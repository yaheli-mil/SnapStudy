package com.yalig.snapstudy.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yalig.snapstudy.R;
import com.yalig.snapstudy.models.StudySet;

import java.util.ArrayList;

/**
 * Adapter שמציג את רשימת ערכות הלימוד ב-RecyclerView במסך הראשי.
 * כל שורה בנויה מהקובץ item_study_set.xml.
 */
public class StudySetAdapter extends RecyclerView.Adapter<StudySetAdapter.SetViewHolder> {

    // ממשק ללחיצה על ערכה ברשימה
    public interface OnSetClickListener {
        void onSetClick(StudySet set);
    }

    private ArrayList<StudySet> sets;
    private final OnSetClickListener listener;

    public StudySetAdapter(ArrayList<StudySet> sets, OnSetClickListener listener) {
        this.sets = sets;
        this.listener = listener;
    }

    // עדכון הרשימה (למשל אחרי חיפוש) וציור מחדש
    public void setSets(ArrayList<StudySet> sets) {
        this.sets = sets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_set, parent, false);
        return new SetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        StudySet set = sets.get(position);
        holder.tvTitle.setText(set.getTitle());
        holder.tvCategory.setText(set.getCategory());
        holder.tvSubtitle.setText(set.getCardCount() + " כרטיסיות • " + set.getUpdatedInfo());
        holder.tvMastery.setText(set.getMasteryPercentage() + "% שליטה");
        holder.pbMastery.setProgress(set.getMasteryPercentage());
        holder.itemView.setOnClickListener(v -> listener.onSetClick(set));
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    // מחזיק את ה-Views של שורה אחת ברשימה
    public static class SetViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvSubtitle, tvMastery;
        ProgressBar pbMastery;

        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSetTitle);
            tvCategory = itemView.findViewById(R.id.tvCategoryBadge);
            tvSubtitle = itemView.findViewById(R.id.tvSetSubtitle);
            tvMastery = itemView.findViewById(R.id.tvMastery);
            pbMastery = itemView.findViewById(R.id.pbMastery);
        }
    }
}
