package feca.utils;

import java.util.ArrayList;
import java.util.List;

public class EndServerTickTaskQueue {
    /// 强制执行一个TaskHandle行为，用于防止忘记 discard 任务
    public static class TaskTickResult {
        private TaskTickResult() {}
    }
    private static final TaskTickResult TASK_TICK_RESULT = new TaskTickResult();

    public interface Task {
        @SuppressWarnings("UnusedReturnValue")
        TaskTickResult tick(TaskHandle task);
    }

    @SuppressWarnings("unused")
    public static class TaskHandle {
        private boolean discarded = false;
        private int pauseTicks = 0;

        public TaskTickResult discard() { discarded = true; return TASK_TICK_RESULT; }
        public TaskTickResult pause(int ticks) { pauseTicks += ticks; return TASK_TICK_RESULT; }
        /// 默认行为
        public TaskTickResult normal() { return TASK_TICK_RESULT; }
    }

    private record TaskContext(TaskHandle handle, Task task) {}

    private static final List<TaskContext> TASKS = new ArrayList<>();

    public static void add(TaskHandle handle, Task task) {
        TASKS.add(new TaskContext(handle, task));
    }
    public static void add(Task task) {
        TASKS.add(new TaskContext(new TaskHandle(), task));
    }

    public static void tick() {
        TASKS.removeIf(ctx -> {
            if (ctx.handle.pauseTicks > 0) {
                ctx.handle.pauseTicks--;
                return false;
            }
            ctx.task.tick(ctx.handle);
            return ctx.handle.discarded;
        });
    }
}
