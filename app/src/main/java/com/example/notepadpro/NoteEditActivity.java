package com.example.notepadpro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 笔记编辑页：标题、分类、正文，支持分享与导出为 txt。
 * 返回/保存时自动入库，空笔记不落库。
 */
public class NoteEditActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static final String EXTRA_ID = "extra_note_id";

    private static final String[] CATEGORY_OPTIONS = {"工作", "学习", "生活", "灵感", "其他"};

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"),
                    uri -> {
                        if (uri != null) {
                            writeToUri(uri);
                        }
                    });

    private NoteDbHelper db;
    private Note note;
    private EditText etTitle;
    private EditText etContent;
    private TextView tvTime;
    private Spinner spCategory;

    private String category = "其他";
    private boolean dirty;
    private boolean deleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = NoteDbHelper.getInstance(this);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        tvTime = findViewById(R.id.tvTime);
        spCategory = findViewById(R.id.spCategory);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORY_OPTIONS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);
        spCategory.setOnItemSelectedListener(this);

        long id = getIntent().getLongExtra(EXTRA_ID, 0L);
        if (id > 0) {
            note = db.findById(id);
        }
        boolean isNew = note == null;
        if (isNew) {
            note = new Note();
            long now = System.currentTimeMillis();
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
        }

        etTitle.setText(note.getTitle());
        etContent.setText(note.getContent());
        category = note.getCategory();
        spCategory.setSelection(indexOfCategory(category), false);

        if (isNew) {
            setTitle(R.string.new_note);
            tvTime.setText(getString(R.string.created_at, TimeUtils.format(note.getCreatedAt())));
        } else {
            setTitle(R.string.edit_note);
            tvTime.setText(getString(R.string.time_line,
                    TimeUtils.format(note.getCreatedAt()),
                    TimeUtils.format(note.getUpdatedAt())));
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dirty = true;
            }
        };
        etTitle.addTextChangedListener(watcher);
        etContent.addTextChangedListener(watcher);

        dirty = false;

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithSave();
            }
        });
    }

    private int indexOfCategory(String value) {
        for (int i = 0; i < CATEGORY_OPTIONS.length; i++) {
            if (CATEGORY_OPTIONS[i].equals(value)) {
                return i;
            }
        }
        return CATEGORY_OPTIONS.length - 1;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selected = CATEGORY_OPTIONS[position];
        if (!selected.equals(category)) {
            category = selected;
            dirty = true;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private boolean persist() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if (title.isEmpty() && content.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        note.setTitle(title);
        note.setContent(content);
        note.setCategory(category);
        note.setUpdatedAt(now);

        if (note.getId() == 0L) {
            long newId = db.insert(note);
            if (newId <= 0) {
                return false;
            }
            note.setId(newId);
        } else {
            db.update(note);
        }

        dirty = false;
        tvTime.setText(getString(R.string.updated_at, TimeUtils.format(now)));
        return true;
    }

    private void finishWithSave() {
        if (!deleted) {
            persist();
        }
        finish();
    }

    private String buildShareText() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString();
        StringBuilder builder = new StringBuilder();
        if (!title.isEmpty()) {
            builder.append(title).append("\n\n");
        }
        builder.append(content.trim());
        builder.append("\n\n—— 来自「记事本」");
        return builder.toString();
    }

    private void shareNote() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, etTitle.getText().toString());
        send.putExtra(Intent.EXTRA_TEXT, buildShareText());
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    private void exportTxt() {
        String name = etTitle.getText().toString().trim();
        if (name.isEmpty()) {
            name = "未命名笔记";
        }
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        exportLauncher.launch(name + ".txt");
    }

    private void writeToUri(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) {
                throw new IllegalStateException("无法写入文件");
            }
            os.write(buildShareText().getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.export_ok, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.export_fail, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_confirm_edit)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (note.getId() > 0) {
                        db.delete(note.getId());
                    }
                    deleted = true;
                    finish();
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            if (persist()) {
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            }
            finish();
            return true;
        }
        if (id == R.id.action_share) {
            shareNote();
            return true;
        }
        if (id == R.id.action_export) {
            exportTxt();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishWithSave();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!deleted && dirty) {
            persist();
        }
    }
}
