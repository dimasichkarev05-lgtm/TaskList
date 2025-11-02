package com.example.tasklist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBHelper";
    private static final String DB_NAME = "tasks.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE = "tasks";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESC = "description";
    public static final String COL_DONE = "done"; // 0 or 1
    public static final String COL_CREATED = "created_at";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String sql = "CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_DESC + " TEXT, "
                + COL_DONE + " INTEGER DEFAULT 0, "
                + COL_CREATED + " INTEGER"
                + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {

        if (oldV < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_CREATED + " INTEGER;");

                db.execSQL("UPDATE " + TABLE + " SET " + COL_CREATED + " = strftime('%s','now') WHERE " + COL_CREATED + " IS NULL;");
            } catch (Exception e) {
                Log.e(TAG, "onUpgrade migration error", e);
            }
        } else {

            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    public long addTask(Task task) {
        long id = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_TITLE, task.getTitle());
            cv.put(COL_DESC, task.getDescription());
            cv.put(COL_DONE, task.isDone() ? 1 : 0);
            long createdSec = (task.getCreatedAt() > 0) ? task.getCreatedAt() : (System.currentTimeMillis() / 1000L);
            cv.put(COL_CREATED, createdSec);
            id = db.insert(TABLE, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addTask error", e);
        }
        return id;
    }

    public List<Task> getTasks(String search, boolean completedFirst, boolean dateDesc) {
        List<Task> list = new ArrayList<>();
        Cursor c = null;
        try {
            SQLiteDatabase db = getReadableDatabase();

            String selection = null;
            String[] selectionArgs = null;
            if (search != null && !search.trim().isEmpty()) {
                selection = COL_TITLE + " LIKE ? OR " + COL_DESC + " LIKE ?";
                String q = "%" + search.trim() + "%";
                selectionArgs = new String[]{q, q};
            }

            // ORDER BY: сначала по done (в зависимости от completedFirst), затем по created_at
            String orderBy = (completedFirst ? (COL_DONE + " DESC") : (COL_DONE + " ASC"))
                    + ", " + COL_CREATED + (dateDesc ? " DESC" : " ASC");

            c = db.query(TABLE, null, selection, selectionArgs, null, null, orderBy);
            if (c != null && c.moveToFirst()) {
                do {
                    long id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                    String title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                    String desc = c.getString(c.getColumnIndexOrThrow(COL_DESC));
                    int doneInt = c.getInt(c.getColumnIndexOrThrow(COL_DONE));
                    boolean done = (doneInt == 1);
                    long created = 0;
                    try {
                        created = c.getLong(c.getColumnIndexOrThrow(COL_CREATED));
                    } catch (Exception ex) {
                        created = 0;
                    }
                    list.add(new Task(id, title, desc, done, created));
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getTasks error", e);
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // Получаем все задачи, по умолчанию новые сверху и не выполняемые первыми
    public List<Task> getAllTasks() {
        return getTasks(null, false, true);
    }

    public boolean updateTaskStatus(long id, boolean done) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_DONE, done ? 1 : 0);
            int rows = db.update(TABLE, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateTaskStatus error", e);
            return false;
        }
    }

    public boolean deleteTask(long id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            int rows = db.delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteTask error", e);
            return false;
        }
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    public Task getTaskById(long id) {
        Cursor c = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            c = db.query(TABLE, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                String desc = c.getString(c.getColumnIndexOrThrow(COL_DESC));
                int doneInt = c.getInt(c.getColumnIndexOrThrow(COL_DONE));
                boolean done = (doneInt == 1);
                long created = 0;
                try {
                    created = c.getLong(c.getColumnIndexOrThrow(COL_CREATED));
                } catch (Exception ex) {
                    created = 0;
                }
                return new Task(id, title, desc, done, created);
            }
        } catch (Exception e) {
            Log.e(TAG, "getTaskById error", e);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public boolean updateTask(Task task) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_TITLE, task.getTitle());
            cv.put(COL_DESC, task.getDescription());
            cv.put(COL_DONE, task.isDone() ? 1 : 0);
            // Не меняем created_at при обновлении
            int rows = db.update(TABLE, cv, COL_ID + "=?", new String[]{String.valueOf(task.getId())});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateTask error", e);
            return false;
        }
    }
}
