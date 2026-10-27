package com.kaam.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure date/stats/filter logic. No Android imports — JVM-testable. */
public final class TaskMath {

    public static final int LOW = 0;
    public static final int MED = 1;
    public static final int HIGH = 2;

    public static final String FILTER_TODAY = "today";
    public static final String FILTER_ALL = "all";
    public static final String FILTER_DONE = "done";

    private TaskMath() {
    }

    public static String labelForPriority(int priority) {
        if (priority == HIGH) return "High";
        if (priority == LOW) return "Low";
        return "Medium";
    }

    public static SimpleDateFormat formatter() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setLenient(false);
        return f;
    }

    public static String dateKey(long millis) {
        return formatter().format(new Date(millis));
    }

    public static String todayKey() {
        return dateKey(System.currentTimeMillis());
    }

    public static boolean isValidDateKey(String key) {
        if (key == null || !key.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try {
            formatter().parse(key);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static String shiftDays(String key, int days) {
        if (!isValidDateKey(key)) return key;
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(formatter().parse(key));
            c.add(Calendar.DAY_OF_MONTH, days);
            return dateKey(c.getTimeInMillis());
        } catch (ParseException e) {
            return key;
        }
    }

    public static int diffDays(String a, String b) {
        if (!isValidDateKey(a) || !isValidDateKey(b)) return 0;
        try {
            Date da = formatter().parse(a);
            Date db = formatter().parse(b);
            return (int) Math.round((da.getTime() - db.getTime()) / 86400000.0);
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String dueLabel(String due, String today) {
        if (!isValidDateKey(due)) return "";
        int delta = diffDays(due, today);
        if (delta == 0) return "Aaj";
        if (delta == 1) return "Kal";
        if (delta == -1) return "1 din late";
        if (delta < 0) return (-delta) + " din late";
        return delta + " din baaki";
    }

    public static String shortDate(String due) {
        if (!isValidDateKey(due)) return "No date";
        try {
            Date d = formatter().parse(due);
            return new SimpleDateFormat("d MMM", Locale.ENGLISH).format(d);
        } catch (ParseException e) {
            return due;
        }
    }

    public static List<Task> filter(List<Task> tasks, String filter, String today) {
        List<Task> out = new ArrayList<>();
        if (tasks == null) return out;
        for (Task t : tasks) {
            if (t == null) continue;
            if (FILTER_ALL.equals(filter)) {
                out.add(t);
            } else if (FILTER_DONE.equals(filter)) {
                if (t.done) out.add(t);
            } else {
                if (t.done) {
                    if (t.completedAt > 0 && dateKey(t.completedAt).equals(today)) {
                        out.add(t);
                    }
                } else if (t.dueDate == null || t.dueDate.compareTo(today) <= 0) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    public static List<Task> sorted(List<Task> tasks) {
        List<Task> out = new ArrayList<>(tasks == null ? new ArrayList<Task>() : tasks);
        Collections.sort(out, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                if (a.done != b.done) return a.done ? 1 : -1;
                if (a.done) {
                    if (a.completedAt == b.completedAt) return 0;
                    return a.completedAt > b.completedAt ? -1 : 1;
                }
                int pa = priorityWeight(a.priority);
                int pb = priorityWeight(b.priority);
                if (pa != pb) return pa < pb ? -1 : 1;
                if (a.dueDate != null && b.dueDate != null) {
                    int d = a.dueDate.compareTo(b.dueDate);
                    if (d != 0) return d;
                } else if (a.dueDate != null) {
                    return -1;
                } else if (b.dueDate != null) {
                    return 1;
                }
                if (a.createdAt == b.createdAt) return 0;
                return a.createdAt > b.createdAt ? -1 : 1;
            }
        });
        return out;
    }

    private static int priorityWeight(int priority) {
        if (priority == HIGH) return 0;
        if (priority == MED) return 1;
        return 2;
    }

    public static Stats stats(List<Task> tasks, String today) {
        int doneToday = 0;
        int pending = 0;
        int todayTotal = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                if (t == null) continue;
                if (!t.done) pending++;
                if (t.done && t.completedAt > 0 && dateKey(t.completedAt).equals(today)) doneToday++;
                boolean todayTask = (t.dueDate != null && t.dueDate.equals(today))
                        || (t.createdAt > 0 && dateKey(t.createdAt).equals(today))
                        || (t.done && t.completedAt > 0 && dateKey(t.completedAt).equals(today));
                if (todayTask) todayTotal++;
            }
        }
        int pct = todayTotal > 0 ? Math.round(doneToday * 100f / todayTotal) : 0;
        return new Stats(doneToday, pending, todayTotal, pct);
    }

    public static int streak(List<Task> tasks, String today) {
        Set<String> days = new HashSet<>();
        if (tasks != null) {
            for (Task t : tasks) {
                if (t != null && t.done && t.completedAt > 0) {
                    days.add(dateKey(t.completedAt));
                }
            }
        }
        String cursor = today;
        if (!days.contains(cursor)) cursor = shiftDays(cursor, -1);
        int count = 0;
        while (days.contains(cursor) && count < 3700) {
            count++;
            cursor = shiftDays(cursor, -1);
        }
        return count;
    }

    public static class Stats {
        public final int doneToday;
        public final int pending;
        public final int todayTotal;
        public final int pct;

        public Stats(int doneToday, int pending, int todayTotal, int pct) {
            this.doneToday = doneToday;
            this.pending = pending;
            this.todayTotal = todayTotal;
            this.pct = pct;
        }
    }
}
