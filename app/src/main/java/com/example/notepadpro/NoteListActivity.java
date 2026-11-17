package com.example.notepadpro;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * 笔记列表页：
 * 1) 每条笔记显示标题、内容摘要、分类和时间戳；
 * 2) 顶部搜索框按标题/正文实时过滤；
 * 3) 分类 Chip 过滤、长按置顶/分享/删除、右下角新建。
 */
public class NoteListActivity extends AppCompatActivity implements NoteAdapter.OnNoteActionListener {

    private static final String[] CATEGORIES = {"全部", "工作", "学习", "生活", "灵感", "其他"};

    private NoteDbHelper db;
    private NoteAdapter adapter;
    private RecyclerView rvNotes;
    private TextView tvEmpty;

    private String keyword = "";
    private String category = "全部";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = NoteDbHelper.getInstance(this);
        rvNotes = findViewById(R.id.rvNotes);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new NoteAdapter(this);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(adapter);

        buildCategoryChips();

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> openEditor(0L));
    }

    private void buildCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroup);
        for (String item : CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chipGroup.addView(chip);
            chip.setChecked(item.equals(category));
            chip.setOnClickListener(v -> {
                category = item;
                loadNotes();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        List<Note> notes = db.query(keyword, category);
        adapter.setNotes(notes);
        boolean empty = notes.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openEditor(long id) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        if (id > 0) {
            intent.putExtra(NoteEditActivity.EXTRA_ID, id);
        }
        startActivity(intent);
    }

    @Override
    public void onNoteClick(Note note) {
        openEditor(note.getId());
    }

    @Override
    public void onNoteLongClick(Note note) {
        String pinAction = note.isPinned() ? getString(R.string.unpin) : getString(R.string.pin);
        String[] actions = {pinAction, getString(R.string.share), getString(R.string.delete)};
        new AlertDialog.Builder(this)
                .setTitle(note.getDisplayTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        db.setPinned(note.getId(), !note.isPinned());
                        loadNotes();
                    } else if (which == 1) {
                        shareNote(note);
                    } else {
                        confirmDelete(note);
                    }
                })
                .show();
    }

    private void confirmDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_confirm, note.getDisplayTitle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.delete(note.getId());
                    loadNotes();
                })
                .show();
    }

    private void shareNote(Note note) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, note.getDisplayTitle());
        send.putExtra(Intent.EXTRA_TEXT, note.getDisplayTitle() + "\n\n" + note.getContent());
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_list, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    keyword = query;
                    loadNotes();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    keyword = newText;
                    loadNotes();
                    return true;
                }
            });
        }

        MenuItem darkItem = menu.findItem(R.id.action_dark);
        if (darkItem != null) {
            darkItem.setChecked(Prefs.isDark(this));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dark) {
            boolean dark = !Prefs.isDark(this);
            Prefs.setDark(this, dark);
            item.setChecked(dark);
            AppCompatDelegate.setDefaultNightMode(dark
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        }
        if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
