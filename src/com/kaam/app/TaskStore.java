package com.kaam.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** SharedPreferences JSON storage. No network, no database. */
public final class TaskStore {

    private static final String PREFS = "kaam";
    private static final String KEY = "tasks_json";

    private TaskStore() {
    }

    public static List<Task> load(Context context) {
        List<Task> tasks = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;

                String title = o.optString("title", "").trim();
                if (title.length() == 0 || title.length() > 120) continue;

                Task t = new Task();
                t.id = o.optString("id", "restored_" + i + "_" + System.currentTimeMillis());
                t.title = title;
                t.priority = Math.max(0, Math.min(2, o.optInt("priority", 1)));

                String due = o.isNull("due") ? null : o.optString("due", null);
                t.dueDate = TaskMath.isValidDateKey(due) ? due : null;

                t.done = o.optBoolean("done", false);
                t.createdAt = o.optLong("created", System.currentTimeMillis());
                t.completedAt = o.optLong("completed", 0);
                if (t.done && t.completedAt <= 0) t.completedAt = t.createdAt;

                tasks.add(t);
            }
        } catch (JSONException e) {
            // corrupted storage -> start clean instead of crashing
        }
        return tasks;
    }

    public static void save(Context context, List<Task> tasks) {
        JSONArray array = new JSONArray();
        if (tasks != null) {
            for (Task t : tasks) {
                if (t == null) continue;
                JSONObject o = new JSONObject();
                try {
                    o.put("id", t.id);
                    o.put("title", t.title);
                    o.put("priority", t.priority);
                    o.put("due", t.dueDate == null ? JSONObject.NULL : t.dueDate);
                    o.put("done", t.done);
                    o.put("created", t.createdAt);
                    o.put("completed", t.completedAt);
                } catch (JSONException e) {
                    continue;
                }
                array.put(o);
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, array.toString())
                .apply();
    }
}
