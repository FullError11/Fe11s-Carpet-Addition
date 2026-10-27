package feca.utils;

import java.util.ArrayList;
import java.util.List;

public class EndServerTickTaskQueue {
    public interface Task {
        void tick(TaskHandle task);
    }

    /// 允许通过继承 TaskHandle 已携带自定义数据
    @SuppressWarnings("unused")
    public static class TaskHandle {
        private boolean discarded = false;
        private int pauseTicks = 0;

        public void discard() { discarded = true; }
        public void pause(int ticks) { pauseTicks += ticks; }
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
