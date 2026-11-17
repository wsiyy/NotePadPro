package com.example.notepadpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 数据访问：笔记表 notes 的增删改查。
 * 采用单例，避免重复打开数据库。
 */
public class NoteDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "notepad.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "notes";

    private static volatile NoteDbHelper instance;

    public static NoteDbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (NoteDbHelper.class) {
                if (instance == null) {
                    instance = new NoteDbHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private NoteDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL DEFAULT '', "
                + "content TEXT NOT NULL DEFAULT '', "
                + "created_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL, "
                + "category TEXT NOT NULL DEFAULT '其他', "
                + "pinned INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insert(Note note) {
        return getWritableDatabase().insert(TABLE, null, toValues(note));
    }

    public int update(Note note) {
        return getWritableDatabase().update(TABLE, toValues(note),
                "_id = ?", new String[]{String.valueOf(note.getId())});
    }

    public int delete(long id) {
        return getWritableDatabase().delete(TABLE, "_id = ?",
                new String[]{String.valueOf(id)});
    }

    public void setPinned(long id, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put("pinned", pinned ? 1 : 0);
        getWritableDatabase().update(TABLE, values, "_id = ?",
                new String[]{String.valueOf(id)});
    }

    public Note findById(long id) {
        try (Cursor cursor = getReadableDatabase().query(TABLE, null, "_id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return readNote(cursor);
            }
        }
        return null;
    }

    /**
     * 按关键字与分类查询笔记。
     * 关键字同时匹配标题和正文（LIKE），分类传“全部”表示不过滤。
     * 排序：置顶优先，其次按更新时间倒序。
     */
    public List<Note> query(String keyword, String category) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE + " WHERE 1 = 1");
        List<String> args = new ArrayList<>();

        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            sql.append(" AND category = ?");
            args.add(category);
        }

        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            sql.append(" AND (title LIKE ? OR content LIKE ?)");
            String like = "%" + kw + "%";
            args.add(like);
            args.add(like);
        }

        sql.append(" ORDER BY pinned DESC, updated_at DESC");

        List<Note> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase()
                .rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                result.add(readNote(cursor));
            }
        }
        return result;
    }

    private ContentValues toValues(Note note) {
        ContentValues values = new ContentValues();
        values.put("title", note.getTitle());
        values.put("content", note.getContent());
        values.put("created_at", note.getCreatedAt());
        values.put("updated_at", note.getUpdatedAt());
        values.put("category", note.getCategory());
        values.put("pinned", note.isPinned() ? 1 : 0);
        return values;
    }

    private Note readNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        note.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
        note.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow("pinned")) == 1);
        return note;
    }
}
