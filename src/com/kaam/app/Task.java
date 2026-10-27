package com.kaam.app;

/** Plain task model. No Android imports — unit-testable on the JVM. */
public class Task {
    public String id;
    public String title;
    public int priority;      // 0 = low, 1 = medium, 2 = high
    public String dueDate;    // yyyy-MM-dd, or null
    public boolean done;
    public long createdAt;
    public long completedAt;  // 0 = not completed

    public Task() {
    }

    public Task(String id, String title, int priority, String dueDate, long createdAt) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }
}
