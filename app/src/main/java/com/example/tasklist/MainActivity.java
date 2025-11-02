package com.example.tasklist;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private TaskAdapter adapter;
    private ListView listView;
    private CheckBox cbFilter;
    private EditText etSearch;
    private Button btnSearch;
    private CheckBox cbDateDesc;

    private static final int REQ_ADD = 1001;
    private static final String TAG = "MainActivity";
    private static final String KEY_FILTER = "key_filter_checked";
    private static final String KEY_SEARCH = "key_search";
    private static final String KEY_DATE_DESC = "key_date_desc";

    // Состояния UI
    private boolean filterCompletedFirst = false; // cbFilter
    private boolean dateDesc = true; // cbDateDesc (true = newest first)
    private String searchQuery = null; // etSearch

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        listView = findViewById(R.id.listView);
        Button btnAdd = findViewById(R.id.btnAdd);
        cbFilter = findViewById(R.id.cbFilter);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        cbDateDesc = findViewById(R.id.cbDateDesc);

        // Восстанавливаем состояние при пересоздании
        if (savedInstanceState != null) {
            filterCompletedFirst = savedInstanceState.getBoolean(KEY_FILTER, false);
            searchQuery = savedInstanceState.getString(KEY_SEARCH, null);
            dateDesc = savedInstanceState.getBoolean(KEY_DATE_DESC, true);

            cbFilter.setChecked(filterCompletedFirst);
            if (searchQuery != null) etSearch.setText(searchQuery);
            cbDateDesc.setChecked(dateDesc);
        } else {
            filterCompletedFirst = cbFilter.isChecked();
            dateDesc = cbDateDesc.isChecked();
        }

        btnAdd.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivityForResult(i, REQ_ADD);
        });

        cbFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterCompletedFirst = isChecked;
            loadTasks();
        });

        cbDateDesc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dateDesc = isChecked;
            loadTasks();
        });

        btnSearch.setOnClickListener(v -> {
            searchQuery = etSearch.getText().toString().trim();
            if (searchQuery != null && searchQuery.isEmpty()) searchQuery = null;
            loadTasks();
        });

        // Открыть задачу для редактирования при коротком нажатии
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object obj = parent.getItemAtPosition(position);
            if (!(obj instanceof Task)) {
                Toast.makeText(MainActivity.this, "Не удалось определить задачу", Toast.LENGTH_SHORT).show();
                return;
            }
            Task task = (Task) obj;
            Intent i = new Intent(MainActivity.this, AddTaskActivity.class);
            i.putExtra(AddTaskActivity.EXTRA_TASK_ID, task.getId());
            startActivityForResult(i, REQ_ADD);
        });

        // Удаление при долгом нажатии
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Object obj = parent.getItemAtPosition(position);
            if (!(obj instanceof Task)) {
                Toast.makeText(MainActivity.this, "Невозможно определить задачу", Toast.LENGTH_SHORT).show();
                return true;
            }
            Task task = (Task) obj;
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Удалить задачу")
                    .setMessage("\"" + task.getTitle() + "\"?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        try {
                            boolean ok = dbHelper.deleteTask(task.getId());
                            if (ok) {
                                if (adapter != null) {
                                    adapter.remove(task);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    loadTasks();
                                }
                                Toast.makeText(MainActivity.this, "Задача удалена", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при удалении задачи", e);
                            Toast.makeText(MainActivity.this, "Исключение при удалении", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return true; // событие обработано
        });

        loadTasks();
    }

    /**
     * Загружает задачи с учётом состояния фильтра/поиска/сортировки и обновляет адаптер.
     */
    private void loadTasks() {
        try {
            List<Task> tasks = dbHelper.getTasks(searchQuery, filterCompletedFirst, dateDesc);
            if (tasks == null) tasks = new ArrayList<>();

            if (adapter == null) {
                adapter = new TaskAdapter(this, tasks, dbHelper);
                listView.setAdapter(adapter);
            } else {
                adapter.clear();
                adapter.addAll(tasks);
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "loadTasks error", e);
            Toast.makeText(this, "Ошибка при загрузке задач", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks(); // обновляем список
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_FILTER, filterCompletedFirst);
        outState.putString(KEY_SEARCH, searchQuery);
        outState.putBoolean(KEY_DATE_DESC, dateDesc);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
