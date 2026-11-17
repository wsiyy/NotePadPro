package com.example.notepadpro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** 笔记列表适配器：展示标题、摘要、分类、时间戳与置顶标记。 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    public interface OnNoteActionListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final List<Note> data = new ArrayList<>();
    private final OnNoteActionListener listener;

    public NoteAdapter(OnNoteActionListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        data.clear();
        data.addAll(notes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteHolder holder, int position) {
        holder.bind(data.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class NoteHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvCategory;
        private final TextView tvTime;
        private final ImageView ivPin;

        NoteHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivPin = itemView.findViewById(R.id.ivPin);
        }

        void bind(Note note, OnNoteActionListener listener) {
            tvTitle.setText(note.getDisplayTitle());

            String snippet = note.getSnippet();
            tvContent.setVisibility(snippet.isEmpty() ? View.GONE : View.VISIBLE);
            tvContent.setText(snippet);

            tvCategory.setText(note.getCategory());
            tvTime.setText(TimeUtils.format(note.getUpdatedAt()));
            ivPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onNoteClick(note));
            itemView.setOnLongClickListener(v -> {
                listener.onNoteLongClick(note);
                return true;
            });
        }
    }
}
