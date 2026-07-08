package fe11.carpetaddition.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

import static fe11.carpetaddition.Feca.LOGGER;

public class DelayedTaskExecutor {
    public static class TaskContext {
        private static final int TICKS_PER_SECOND = 20;

        public interface Callback extends Consumer<TaskContext> {}

        private record DelayedTask(long executeTick, Callback runnable) {}

        private Callback perTickDo;
        private Callback perSecondDo;

        private int nowTick = 0;
        private int accumulatedDelay = 0;

        private boolean shouldRemove = false;

        private final PriorityQueue<DelayedTask> delayedTasks = new PriorityQueue<>(
                Comparator.comparingLong(DelayedTask::executeTick)
        );

        public boolean isShouldRemove() {
            return shouldRemove;
        }

        public TaskContext perTickDo(Callback perTickDo) {
            this.perTickDo = perTickDo;
            return this;
        }

        public TaskContext perSecondDo(Callback perSecondDo) {
            this.perSecondDo = perSecondDo;
            return this;
        }

        public TaskContext afterTickDo(int tick, Callback task) {
            accumulatedDelay += tick;
            delayedTasks.offer(new DelayedTask(nowTick + accumulatedDelay, task));
            return this;
        }

        public TaskContext afterSecondDo(float second, Callback task) {
            return afterTickDo((int)(second * TICKS_PER_SECOND), task);
        }

        public void discard() {
            LOGGER.info("[DelayedTask] Task {} needs to be removed since it's discarded.", this);
            shouldRemove = true;
        }

        public void discardIf(boolean condition) {
            if (condition) discard();
        }

        public void tick() {
            nowTick++;

            // 每tick任务
            if (perTickDo != null) {
                perTickDo.accept(this);
            }

            // 每秒任务
            if (perSecondDo != null && nowTick % TICKS_PER_SECOND == 0) {
                perSecondDo.accept(this);
            }

            // 延迟任务
            while (!delayedTasks.isEmpty() && delayedTasks.peek().executeTick <= nowTick) {
                DelayedTask task = delayedTasks.poll();
                task.runnable.accept(this);
            }

            // 没有待执行任务时标记移除
            if (delayedTasks.isEmpty()) {
                LOGGER.info("[DelayedTask] Task {} needs to be removed since it's already done.", this);
                shouldRemove = true;
            }
        }
    }

    private static final List<TaskContext> tasks = new ArrayList<>();

    public static @NotNull TaskContext createTask() {
        TaskContext ctx = new TaskContext();
        tasks.add(ctx);
        return ctx;
    }

    public static void tick() {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskContext ctx = tasks.get(i);
            ctx.tick();
            if (ctx.isShouldRemove()) {
                tasks.remove(i);
            }
        }
    }
}