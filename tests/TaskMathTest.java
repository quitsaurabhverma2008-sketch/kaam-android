import com.kaam.app.Task;
import com.kaam.app.TaskMath;

import java.util.ArrayList;
import java.util.List;

/** Plain-JVM tests for the Android app's pure logic (no Android runtime needed). */
public class TaskMathTest {

    private static int passed = 0;

    private static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError("FAIL: " + name);
        passed++;
        System.out.println("  ✓ " + name);
    }

    private static long millis(String key) throws Exception {
        return TaskMath.formatter().parse(key).getTime();
    }

    public static void main(String[] args) throws Exception {
        String today = "2026-10-27";

        System.out.println("Date logic:");
        check("valid date key", TaskMath.isValidDateKey("2026-10-27"));
        check("reject 31 Feb", !TaskMath.isValidDateKey("2026-02-31"));
        check("reject junk", !TaskMath.isValidDateKey("hello"));
        check("diff +2 days", TaskMath.diffDays("2026-10-29", today) == 2);
        check("shift -1 day", TaskMath.shiftDays(today, -1).equals("2026-10-26"));
        check("shift across month", TaskMath.shiftDays("2026-11-01", -1).equals("2026-10-31"));
        check("due label today", TaskMath.dueLabel(today, today).equals("Aaj"));
        check("due label tomorrow", TaskMath.dueLabel("2026-10-28", today).equals("Kal"));
        check("due label late", TaskMath.dueLabel("2026-10-20", today).equals("7 din late"));
        check("short date", TaskMath.shortDate("2026-11-01").equals("1 Nov"));
        check("priority labels", TaskMath.labelForPriority(2).equals("High")
                && TaskMath.labelForPriority(1).equals("Medium")
                && TaskMath.labelForPriority(0).equals("Low"));

        Task a = new Task("a", "Alpha", TaskMath.HIGH, "2026-10-28", millis(today));
        Task b = new Task("b", "Beta", TaskMath.MED, today, millis(today));
        Task c = new Task("c", "Gamma", TaskMath.LOW, null, millis(today));

        Task d = new Task("d", "Delta", TaskMath.MED, null, millis(today));
        d.done = true;
        d.completedAt = millis(today);

        Task e = new Task("e", "Echo", TaskMath.MED, null, millis("2026-10-26"));
        e.done = true;
        e.completedAt = millis("2026-10-26");

        Task f = new Task("f", "Foxtrot", TaskMath.LOW, null, millis("2026-10-23"));
        f.done = true;
        f.completedAt = millis("2026-10-23");

        List<Task> tasks = new ArrayList<>();
        tasks.add(a);
        tasks.add(b);
        tasks.add(c);
        tasks.add(d);
        tasks.add(e);
        tasks.add(f);

        System.out.println("Filters:");
        check("today filter count", TaskMath.filter(tasks, TaskMath.FILTER_TODAY, today).size() == 3);
        check("all filter count", TaskMath.filter(tasks, TaskMath.FILTER_ALL, today).size() == 6);
        check("done filter count", TaskMath.filter(tasks, TaskMath.FILTER_DONE, today).size() == 3);
        check("today includes done today", contains(TaskMath.filter(tasks, TaskMath.FILTER_TODAY, today), "d"));
        check("today excludes future due", !contains(TaskMath.filter(tasks, TaskMath.FILTER_TODAY, today), "a"));

        System.out.println("Sorting:");
        List<Task> sorted = TaskMath.sorted(tasks);
        String order = sorted.get(0).title + "," + sorted.get(1).title + ","
                + sorted.get(2).title + "," + sorted.get(3).title + ","
                + sorted.get(4).title + "," + sorted.get(5).title;
        check("priority then done order", order.equals("Alpha,Beta,Gamma,Delta,Echo,Foxtrot"));

        System.out.println("Stats & streak:");
        TaskMath.Stats stats = TaskMath.stats(tasks, today);
        check("done today", stats.doneToday == 1);
        check("pending", stats.pending == 3);
        check("today total", stats.todayTotal == 4);
        check("percent", stats.pct == 25);
        check("streak 2 days", TaskMath.streak(tasks, today) == 2);
        check("empty stats", TaskMath.stats(new ArrayList<Task>(), today).pct == 0);
        check("empty streak", TaskMath.streak(new ArrayList<Task>(), today) == 0);

        Task onlyYesterday = new Task("y", "Yesterday", TaskMath.MED, null, millis("2026-10-26"));
        onlyYesterday.done = true;
        onlyYesterday.completedAt = millis("2026-10-26");
        List<Task> yesterdayList = new ArrayList<>();
        yesterdayList.add(onlyYesterday);
        check("yesterday keeps streak alive", TaskMath.streak(yesterdayList, today) == 1);

        System.out.println("\nALL TESTS PASSED (" + passed + " checks)");
    }

    private static boolean contains(List<Task> tasks, String id) {
        for (Task t : tasks) if (id.equals(t.id)) return true;
        return false;
    }
}
