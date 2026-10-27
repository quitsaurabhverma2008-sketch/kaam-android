package com.kaam.app;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** ListView adapter — builds each task row in code (no XML layouts). */
public class TaskAdapter extends BaseAdapter {

    public interface Listener {
        void onToggle(Task task);
        void onEdit(Task task);
        void onDelete(Task task);
    }

    private final Context context;
    private final Listener listener;
    private List<Task> items = new ArrayList<>();

    public TaskAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Task> newItems) {
        items = newItems == null ? new ArrayList<Task>() : newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        String id = items.get(position).id;
        return id == null ? position : id.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Task task = items.get(position);
        final String today = TaskMath.todayKey();
        int pad = Ui.dp(context, 12);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(pad, pad, Ui.dp(context, 8), pad);
        row.setBackground(Ui.roundedStroke(Ui.SURFACE, context, 15, Ui.BORDER));
        if (task.done) row.setAlpha(0.62f);

        // ---- check circle ----
        TextView check = Ui.text(context, task.done ? "✓" : "", 13, 0xFF06261B, true);
        check.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        if (task.done) {
            circle.setColor(Ui.SUCCESS);
        } else {
            circle.setColor(0x00000000);
            circle.setStroke(Ui.dp(context, 2), 0xFF4A4A5E);
        }
        check.setBackground(circle);
        check.setContentDescription(task.done ? "Task pending karo" : "Task complete karo");
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(
                Ui.dp(context, 24), Ui.dp(context, 24));
        checkLp.topMargin = Ui.dp(context, 1);
        checkLp.rightMargin = Ui.dp(context, 12);
        check.setLayoutParams(checkLp);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onToggle(task);
            }
        });
        row.addView(check);

        // ---- title + meta ----
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEdit(task);
            }
        });

        TextView title = Ui.text(context, task.title, 15,
                task.done ? Ui.FAINT : Ui.TEXT, false);
        title.setMaxLines(3);
        if (task.done) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        body.addView(title);

        String meta = TaskMath.labelForPriority(task.priority);
        int metaColor = task.priority == TaskMath.HIGH ? Ui.DANGER
                : task.priority == TaskMath.MED ? Ui.WARNING : Ui.SUCCESS;
        if (task.dueDate != null) {
            String due = TaskMath.dueLabel(task.dueDate, today);
            meta = meta + "  ·  " + due;
            if (!task.done && task.dueDate.compareTo(today) < 0) metaColor = Ui.DANGER;
        }
        TextView metaView = Ui.text(context, meta, 11, metaColor, false);
        metaView.setPadding(0, Ui.dp(context, 4), 0, 0);
        body.addView(metaView);
        row.addView(body);

        // ---- edit / delete ----
        TextView edit = Ui.text(context, "✎", 16, Ui.MUTED, false);
        styleMini(edit, "Task edit karo");
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEdit(task);
            }
        });
        row.addView(edit);

        TextView delete = Ui.text(context, "✕", 16, Ui.MUTED, false);
        styleMini(delete, "Task delete karo");
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDelete(task);
            }
        });
        row.addView(delete);

        row.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void styleMini(TextView view, String description) {
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                Ui.dp(context, 32), Ui.dp(context, 32)));
        android.util.TypedValue out = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, out, true)) {
            view.setBackgroundResource(out.resourceId);
        }
    }
}
