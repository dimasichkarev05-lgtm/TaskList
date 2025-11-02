package com.example.tasklist;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

public class AddTaskActivity extends AppCompatActivity {
    public static final String EXTRA_TASK_ID = "extra_task_id";

    private EditText etTitle, etDesc;
    private CheckBox cbDoneInForm;
    private DBHelper dbHelper;
    private static final String KEY_TITLE = "key_title";
    private static final String KEY_DESC = "key_desc";
    private static final String KEY_DONE = "key_done";
    private static final String TAG = "AddTaskActivity";

    private long editingTaskId = -1; // -1 = новая задача

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        etTitle = findViewById(R.id.etTitle);
        etDesc = findViewById(R.id.etDesc);
        cbDoneInForm = findViewById(R.id.cbDoneInForm);
        Button btnSave = findViewById(R.id.btnSave);

        dbHelper = new DBHelper(this);

        // Если активность вызвана с id — загружаем задачу для редактирования
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TASK_ID)) {
            editingTaskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
            if (editingTaskId != -1) {
                loadTaskForEditing(editingTaskId);
            }
        }

        // Восстановление при повороте
        if (savedInstanceState != null) {
            etTitle.setText(savedInstanceState.getString(KEY_TITLE, ""));
            etDesc.setText(savedInstanceState.getString(KEY_DESC, ""));
            cbDoneInForm.setChecked(savedInstanceState.getBoolean(KEY_DONE, false));
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            boolean done = cbDoneInForm.isChecked();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Заголовок обязателен");
                return;
            }

            try {
                if (editingTaskId == -1) {
                    // добавление новой
                    Task t = new Task(title, desc, done);
                    long id = dbHelper.addTask(t);
                    if (id != -1) {
                        Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка при добавлении задачи", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // обновление существующей
                    Task t = new Task(editingTaskId, title, desc, done);
                    boolean ok = dbHelper.updateTask(t);
                    if (ok) {
                        Toast.makeText(this, "Задача обновлена", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка при обновлении задачи", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "save error", e);
                Toast.makeText(this, "Исключение при работе с БД", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTaskForEditing(long id) {
        try {
            Task t = dbHelper.getTaskById(id);
            if (t != null) {
                etTitle.setText(t.getTitle());
                etDesc.setText(t.getDescription());
                cbDoneInForm.setChecked(t.isDone());
            } else {
                Toast.makeText(this, "Задача не найдена", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "loadTaskForEditing error", e);
            Toast.makeText(this, "Ошибка при загрузке задачи", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_TITLE, etTitle.getText().toString());
        outState.putString(KEY_DESC, etDesc.getText().toString());
        outState.putBoolean(KEY_DONE, cbDoneInForm.isChecked());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
