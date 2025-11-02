package com.example.tasklist;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Адаптер задач с отображением даты создания
 */
public class TaskAdapter extends ArrayAdapter<Task> {
    private DBHelper dbHelper;
    private List<Task> tasks;

    public TaskAdapter(Context context, List<Task> tasks, DBHelper dbHelper) {
        super(context, 0, tasks != null ? tasks : new ArrayList<>());
        this.dbHelper = dbHelper;
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        setHasStableIds(true);
    }

    private void setHasStableIds(boolean b) {
    }

    private static class ViewHolder {
        CheckBox cbDone;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvDate;
    }

    @Override
    public long getItemId(int position) {
        Task t = getItem(position);
        return (t != null) ? t.getId() : position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    private SimpleDateFormat getDateFormatter() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Task task = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_item, parent, false);
            holder = new ViewHolder();
            holder.cbDone = convertView.findViewById(R.id.cbDone);
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvDesc = convertView.findViewById(R.id.tvDesc);
            holder.tvDate = convertView.findViewById(R.id.tvDate);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        if (task == null) {
            holder.tvTitle.setText("");
            holder.tvDesc.setText("");
            holder.tvDate.setText("");
            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(false);
            return convertView;
        }

        // Текстовые поля
        holder.tvTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        holder.tvDesc.setText(task.getDescription() != null ? task.getDescription() : "");

        // Дата создания (если есть) — форматируем, иначе пустая строка
        long createdSec = task.getCreatedAt();
        if (createdSec > 0) {
            Date d = new Date(createdSec * 1000L);
            String dateStr = getDateFormatter().format(d);
            holder.tvDate.setText(dateStr);
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setText("");
            holder.tvDate.setVisibility(View.GONE);
        }

        // Внешний вид в зависимости от статуса
        if (task.isDone()) {
            holder.tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text_done));
            holder.tvDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text_done));
            holder.tvDate.setTextColor(ContextCompat.getColor(getContext(), R.color.task_date_color));
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text));
            holder.tvDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text));
            holder.tvDate.setTextColor(ContextCompat.getColor(getContext(), R.color.task_date_color));
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Настройка CheckBox — отключаем старый слушатель
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(task.isDone());
        holder.cbDone.setFocusable(false);
        holder.cbDone.setClickable(true);

        // Обработчик изменения статуса
        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                boolean ok = dbHelper.updateTaskStatus(task.getId(), isChecked);
                if (ok) {
                    task.setDone(isChecked);
                    // мгновенно обновляем вид строки
                    if (isChecked) {
                        holder.tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text_done));
                        holder.tvDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text_done));
                        holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        holder.tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text));
                        holder.tvDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.task_text));
                        holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }
                } else {
                    // откат UI при ошибке
                    buttonView.setChecked(!isChecked);
                    Toast.makeText(getContext(), "Ошибка при обновлении статуса задачи", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Исключение при обновлении статуса", Toast.LENGTH_SHORT).show();
                buttonView.setChecked(!isChecked);
            }
        });

        return convertView;
    }
}
