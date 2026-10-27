package com.kaam.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Kaam — native Android task tracker. Single screen, fully offline. */
public class MainActivity extends Activity {

    private List<Task> tasks = new ArrayList<>();
    private String filter = TaskMath.FILTER_TODAY;
    private String pendingDue = null;

    private TaskAdapter adapter;
    private ListView listView;
    private LinearLayout emptyView;
    private TextView emptyTitle;
    private TextView emptySub;
    private EditText input;
    private Spinner prioritySpinner;
    private Button dateBtn;

    private TextView statDone;
    private TextView statPending;
    private TextView statStreak;
    private TextView heroSub;
    private TextView pctText;
    private TextView todayLabel;
    private ProgressBar progress;

    private final TextView[] filterButtons = new TextView[3];
    private final String[] filterKeys = {
            TaskMath.FILTER_TODAY, TaskMath.FILTER_ALL, TaskMath.FILTER_DONE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        if (savedInstanceState != null) {
            filter = savedInstanceState.getString("filter", TaskMath.FILTER_TODAY);
            pendingDue = savedInstanceState.getString("pendingDue", null);
        }
        if (!TaskMath.FILTER_TODAY.equals(filter)
                && !TaskMath.FILTER_ALL.equals(filter)
                && !TaskMath.FILTER_DONE.equals(filter)) {
            filter = TaskMath.FILTER_TODAY;
        }
        if (!TaskMath.isValidDateKey(pendingDue)) pendingDue = null;

        tasks = TaskStore.load(this);
        buildUi();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("filter", filter);
        outState.putString("pendingDue", pendingDue);
    }

    /* ================= UI construction ================= */

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(dp(16), dp(14), dp(16), 0);

        root.addView(buildHeader());
        root.addView(buildHero());
        root.addView(buildStats());
        root.addView(buildAddCard());
        root.addView(buildFilters());
        root.addView(buildList());
        root.addView(buildEmpty());

        setContentView(root);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = Ui.text(this, "✓", 16, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.gradient(Ui.PRIMARY, Ui.PRIMARY2, this, 11));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.rightMargin = dp(10);
        header.addView(logo, logoLp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView appName = Ui.text(this, "Kaam", 22, Ui.TEXT, true);
        appName.setLetterSpacing(-0.02f);
        todayLabel = Ui.text(this, "", 11, Ui.MUTED, false);
        titles.addView(appName);
        titles.addView(todayLabel);
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView menu = Ui.text(this, "⋮", 24, Ui.MUTED, false);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Menu");
        menu.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });
        header.addView(menu);
        return header;
    }

    private View buildHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(Ui.gradient(0xFF4F46E5, 0xFF7C3AED, this, 22));

        TextView title = Ui.text(this, "Aaj ka kaam,\naaj hi.", 20, Color.WHITE, true);
        title.setLineSpacing(0, 1.05f);
        hero.addView(title);

        heroSub = Ui.text(this, "", 11, 0xFFD8D8F5, false);
        heroSub.setPadding(0, dp(6), 0, 0);
        hero.addView(heroSub);

        LinearLayout progressRow = new LinearLayout(this);
        progressRow.setOrientation(LinearLayout.HORIZONTAL);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        progressRow.setPadding(0, dp(14), 0, 0);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x40FFFFFF));
        progressRow.addView(progress, new LinearLayout.LayoutParams(0, dp(7), 1f));

        pctText = Ui.text(this, "0%", 12, Color.WHITE, true);
        LinearLayout.LayoutParams pctLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pctLp.leftMargin = dp(10);
        progressRow.addView(pctText, pctLp);
        hero.addView(progressRow);

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(16);
        hero.setLayoutParams(heroLp);
        return hero;
    }

    private View buildStats() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        statDone = addStat(row, "Aaj done");
        statPending = addStat(row, "Pending");
        statStreak = addStat(row, "Din streak");
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(12);
        row.setLayoutParams(rowLp);
        return row;
    }

    private TextView addStat(LinearLayout row, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(Ui.roundedStroke(Ui.SURFACE, this, 14, Ui.BORDER));

        TextView value = Ui.text(this, "0", 21, Ui.TEXT, true);
        TextView name = Ui.text(this, label, 10, Ui.FAINT, false);
        name.setPadding(0, dp(3), 0, 0);
        card.addView(value);
        card.addView(name);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        row.addView(card, lp);
        return value;
    }

    private View buildAddCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(Ui.roundedStroke(Ui.SURFACE, this, 18, Ui.BORDER));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(12);
        card.setLayoutParams(cardLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setHint("Naya kaam likho…");
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.FAINT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setBackground(Ui.roundedStroke(Ui.INPUT_BG, this, 12, Ui.BORDER));
        input.setPadding(dp(12), 0, dp(12), 0);
        row1.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button addBtn = new Button(this);
        addBtn.setText("Add");
        addBtn.setAllCaps(false);
        addBtn.setTextSize(14);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        addBtn.setBackgroundTintList(ColorStateList.valueOf(Ui.PRIMARY));
        addBtn.setStateListAnimator(null);
        addBtn.setMinWidth(0);
        addBtn.setMinimumWidth(0);
        addBtn.setPadding(dp(18), 0, dp(18), 0);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        addLp.leftMargin = dp(8);
        addBtn.setLayoutParams(addLp);
        final Runnable addAction = new Runnable() {
            @Override
            public void run() {
                addTask();
            }
        };
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAction.run();
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addAction.run();
                    return true;
                }
                return false;
            }
        });
        row1.addView(addBtn);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(8), 0, 0);

        prioritySpinner = makePrioritySpinner(1);
        row2.addView(prioritySpinner, new LinearLayout.LayoutParams(0, dp(40), 1f));

        dateBtn = new Button(this);
        styleDateButton(dateBtn);
        updateDateButton();
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(pendingDue, new DateCallback() {
                    @Override
                    public void onDate(String key) {
                        pendingDue = key;
                        updateDateButton();
                    }
                });
            }
        });
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, dp(40), 1.3f);
        dateLp.leftMargin = dp(8);
        row2.addView(dateBtn, dateLp);

        card.addView(row1);
        card.addView(row2);
        return card;
    }

    private View buildFilters() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Aaj", "Sab", "Done"};
        for (int i = 0; i < 3; i++) {
            final String key = filterKeys[i];
            TextView button = Ui.text(this, labels[i], 12, Ui.MUTED, false);
            button.setGravity(Gravity.CENTER);
            button.setPadding(0, dp(9), 0, dp(9));
            button.setBackground(Ui.roundedStroke(Ui.SURFACE, this, 11, Ui.BORDER));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filter = key;
                    refresh();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = dp(6);
            row.addView(button, lp);
            filterButtons[i] = button;
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(12);
        row.setLayoutParams(rowLp);
        return row;
    }

    private View buildList() {
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(dp(8));
        listView.setClipToPadding(false);
        listView.setPadding(0, dp(10), 0, dp(10));
        listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        listView.setBackgroundColor(Color.TRANSPARENT);

        adapter = new TaskAdapter(this, new TaskAdapter.Listener() {
            @Override
            public void onToggle(Task task) {
                toggleTask(task);
            }

            @Override
            public void onEdit(Task task) {
                showEditDialog(task);
            }

            @Override
            public void onDelete(Task task) {
                confirmDelete(task);
            }
        });
        listView.setAdapter(adapter);
        listView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return listView;
    }

    private View buildEmpty() {
        emptyView = new LinearLayout(this);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView icon = Ui.text(this, "✓", 26, Ui.PRIMARY, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.rounded(0x1A6366F1, this, 18));
        emptyView.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        emptyTitle = Ui.text(this, "Aaj clear hai 🎉", 15, Ui.TEXT, true);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, dp(12), 0, 0);
        emptyView.addView(emptyTitle);

        emptySub = Ui.text(this, "Naya kaam add karo ya aaram karo.", 12, Ui.MUTED, false);
        emptySub.setGravity(Gravity.CENTER);
        emptySub.setPadding(0, dp(5), 0, 0);
        emptyView.addView(emptySub);

        emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listView.setEmptyView(emptyView);
        return emptyView;
    }

    /* ================= state / refresh ================= */

    private void refresh() {
        String today = TaskMath.todayKey();
        adapter.setItems(TaskMath.sorted(TaskMath.filter(tasks, filter, today)));

        TaskMath.Stats stats = TaskMath.stats(tasks, today);
        int streak = TaskMath.streak(tasks, today);

        statDone.setText(String.valueOf(stats.doneToday));
        statPending.setText(String.valueOf(stats.pending));
        statStreak.setText(String.valueOf(streak));
        progress.setProgress(stats.pct);
        pctText.setText(stats.pct + "%");

        if (stats.pending == 0) {
            heroSub.setText("Sab clear! Naya target set karo.");
        } else {
            heroSub.setText(stats.pending + " kaam baaki · " + streak + " din streak");
        }

        try {
            todayLabel.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
                    .format(new Date()));
        } catch (Exception e) {
            todayLabel.setText(today);
        }

        updateTabs(today);
        updateEmptyState();
    }

    private void updateTabs(String today) {
        int doneCount = 0;
        for (Task t : tasks) if (t.done) doneCount++;
        int[] counts = {
                TaskMath.filter(tasks, TaskMath.FILTER_TODAY, today).size(),
                tasks.size(),
                doneCount
        };
        String[] labels = {"Aaj", "Sab", "Done"};
        for (int i = 0; i < 3; i++) {
            boolean active = filterKeys[i].equals(filter);
            filterButtons[i].setText(labels[i] + " · " + counts[i]);
            filterButtons[i].setTextColor(active ? Color.WHITE : Ui.MUTED);
            filterButtons[i].setBackground(active
                    ? Ui.gradient(Ui.PRIMARY, 0xFF8B5CF6, this, 11)
                    : Ui.roundedStroke(Ui.SURFACE, this, 11, Ui.BORDER));
        }
    }

    private void updateEmptyState() {
        if (TaskMath.FILTER_DONE.equals(filter)) {
            emptyTitle.setText("Abhi kuch done nahi");
            emptySub.setText("Task complete karo, yahan dikhega.");
        } else if (TaskMath.FILTER_ALL.equals(filter)) {
            emptyTitle.setText("List khali hai");
            emptySub.setText("Upar se pehla kaam add karo.");
        } else {
            emptyTitle.setText("Aaj clear hai 🎉");
            emptySub.setText("Naya kaam add karo ya aaram karo.");
        }
    }

    private void addTask() {
        String title = input.getText().toString().trim().replaceAll("\\s+", " ");
        if (title.length() == 0) {
            input.setError("Pehle task likho");
            input.requestFocus();
            return;
        }
        if (title.length() > 120) title = title.substring(0, 120);

        int priority = prioritySpinner.getSelectedItemPosition();
        if (priority < 0 || priority > 2) priority = 1;

        Task task = new Task(java.util.UUID.randomUUID().toString(), title, priority,
                pendingDue, System.currentTimeMillis());
        tasks.add(0, task);
        TaskStore.save(this, tasks);

        input.setText("");
        pendingDue = null;
        updateDateButton();
        hideKeyboard();
        refresh();
    }

    private void toggleTask(Task task) {
        task.done = !task.done;
        task.completedAt = task.done ? System.currentTimeMillis() : 0;
        TaskStore.save(this, tasks);
        refresh();
    }

    private void confirmDelete(final Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Task delete kare?")
                .setMessage(task.title)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeTask(task);
                        TaskStore.save(MainActivity.this, tasks);
                        refresh();
                        Toast.makeText(MainActivity.this, "Task delete hua", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeTask(Task task) {
        for (int i = 0; i < tasks.size(); i++) {
            Task current = tasks.get(i);
            boolean same = task.id == null ? current == task
                    : task.id.equals(current.id);
            if (same) {
                tasks.remove(i);
                return;
            }
        }
    }

    private void showEditDialog(final Task task) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), 0);

        box.addView(dialogLabel("Title"));
        final EditText titleInput = new EditText(this);
        styleDialogInput(titleInput);
        titleInput.setText(task.title);
        titleInput.setMaxLines(3);
        box.addView(titleInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        box.addView(dialogLabel("Priority"));
        final Spinner priority = makePrioritySpinner(task.priority);
        box.addView(priority, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        box.addView(dialogLabel("Due date"));
        final String[] due = {task.dueDate};
        final Button dueButton = new Button(this);
        styleDateButton(dueButton);
        setDateButtonText(dueButton, due[0]);
        dueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(due[0], new DateCallback() {
                    @Override
                    public void onDate(String key) {
                        due[0] = key;
                        setDateButtonText(dueButton, key);
                    }
                });
            }
        });
        box.addView(dueButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Task edit karo");
        builder.setView(box);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String title = titleInput.getText().toString()
                                        .trim().replaceAll("\\s+", " ");
                                if (title.length() == 0) {
                                    titleInput.setError("Title likho");
                                    return;
                                }
                                if (title.length() > 120) title = title.substring(0, 120);
                                task.title = title;
                                task.priority = priority.getSelectedItemPosition();
                                task.dueDate = due[0];
                                TaskStore.save(MainActivity.this, tasks);
                                dialog.dismiss();
                                refresh();
                            }
                        });
            }
        });
        dialog.show();
    }

    /* ================= small helpers ================= */

    private int dp(float value) {
        return Ui.dp(this, value);
    }

    private Button styleDateButton(Button button) {
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        button.setTextColor(Ui.TEXT);
        button.setBackground(Ui.roundedStroke(Ui.INPUT_BG, this, 10, Ui.BORDER));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(dp(40));
        button.setSingleLine(true);
        return button;
    }

    private void setDateButtonText(Button button, String due) {
        if (TaskMath.isValidDateKey(due)) {
            button.setText("Due · " + TaskMath.shortDate(due));
            button.setTextColor(Ui.TEXT);
        } else {
            button.setText("Due · none");
            button.setTextColor(Ui.FAINT);
        }
    }

    private void updateDateButton() {
        if (dateBtn != null) setDateButtonText(dateBtn, pendingDue);
    }

    private Spinner makePrioritySpinner(int selection) {
        final String[] labels = {"Low", "Medium", "High"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Ui.TEXT);
                view.setTextSize(12);
                view.setPadding(0, 0, 0, 0);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Ui.TEXT);
                view.setTextSize(13);
                view.setBackgroundColor(Ui.SURFACE2);
                view.setPadding(dp(14), dp(10), dp(14), dp(10));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(selection);
        spinner.setBackground(Ui.roundedStroke(Ui.INPUT_BG, this, 10, Ui.BORDER));
        spinner.setPadding(dp(6), 0, dp(6), 0);
        spinner.setMinimumHeight(dp(40));
        spinner.setPopupBackgroundDrawable(Ui.rounded(Ui.SURFACE2, this, 12));
        return spinner;
    }

    private TextView dialogLabel(String text) {
        TextView label = Ui.text(this, text, 10, Ui.FAINT, true);
        label.setLetterSpacing(0.08f);
        label.setPadding(0, dp(12), 0, dp(4));
        return label;
    }

    private void styleDialogInput(EditText edit) {
        edit.setTextSize(15);
        edit.setTextColor(Ui.TEXT);
        edit.setHintTextColor(Ui.FAINT);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setBackground(Ui.roundedStroke(Ui.INPUT_BG, this, 10, Ui.BORDER));
        edit.setPadding(dp(12), dp(10), dp(12), dp(10));
    }

    private interface DateCallback {
        void onDate(String key);
    }

    private void pickDate(String current, final DateCallback callback) {
        Calendar calendar = Calendar.getInstance();
        if (TaskMath.isValidDateKey(current)) {
            String[] parts = current.split("-");
            calendar.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]));
        }
        DatePickerDialog dialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        callback.onDate(String.format(Locale.US, "%04d-%02d-%02d",
                                year, month + 1, dayOfMonth));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        callback.onDate(null);
                    }
                });
        dialog.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    /* ================= menu ================= */

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Export / Share");
        popup.getMenu().add(0, 2, 1, "Delete completed");
        popup.getMenu().add(0, 3, 2, "About");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    shareBackup();
                } else if (item.getItemId() == 2) {
                    confirmDeleteCompleted();
                } else if (item.getItemId() == 3) {
                    showAbout();
                }
                return true;
            }
        });
        popup.show();
    }

    private void shareBackup() {
        if (tasks.isEmpty()) {
            Toast.makeText(this, "Abhi koi task nahi", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder("Kaam — task backup\n\n");
        for (Task t : tasks) {
            sb.append(t.done ? "[x] " : "[ ] ")
                    .append(TaskMath.labelForPriority(t.priority))
                    .append(" · ")
                    .append(t.title);
            if (t.dueDate != null) sb.append("  (due ").append(t.dueDate).append(")");
            sb.append('\n');
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Kaam backup");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "Share backup"));
    }

    private void confirmDeleteCompleted() {
        int count = 0;
        for (Task t : tasks) if (t.done) count++;
        if (count == 0) {
            Toast.makeText(this, "Koi completed task nahi", Toast.LENGTH_SHORT).show();
            return;
        }
        final int total = count;
        new AlertDialog.Builder(this)
                .setTitle("Delete completed?")
                .setMessage(total + " completed tasks delete honge.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Iterator<Task> it = tasks.iterator();
                        while (it.hasNext()) {
                            if (it.next().done) it.remove();
                        }
                        TaskStore.save(MainActivity.this, tasks);
                        refresh();
                        Toast.makeText(MainActivity.this,
                                total + " tasks delete hue", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Kaam v1.0")
                .setMessage("Offline task tracker\n\n"
                        + "• Data sirf is device me (SharedPreferences)\n"
                        + "• Koi internet permission nahi\n"
                        + "• Aaj ka kaam, aaj hi.\n\n"
                        + "Made with ❤️")
                .setPositiveButton("OK", null)
                .show();
    }
}
