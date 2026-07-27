package feca.utils;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

public class DelayedTask {
    private static final int TICKS_PER_SECOND = 20;
    private static final List<DelayedTask> TASKS = new ArrayList<>();

    private Status status = Status.Active;
    private int nowTick = 0;
    private Consumer<DelayedTask> everyTick;
    private Consumer<DelayedTask> everySecond;
    private final Queue<Task> delayedTasks = new PriorityQueue<>();

    private DelayedTask() {}

    /**
     * @apiNote 当任务结束时，必须手动调用 task.done()或task.cancal() 来标记完成或取消，否则将一直允许everyXXX任务
     */
    @SuppressWarnings("unused")
    @Contract(value = " -> new", pure = true)
    public static @NonNull Builder newTask() {
        return new DelayedTask().new Builder();
    }

    public static void tick() {
        // run
        TASKS.forEach(task -> {
            // tick
            if (task.everyTick != null) task.everyTick.accept(task);
            // second
            if (task.everySecond != null && task.nowTick % TICKS_PER_SECOND == 0) {
                task.everySecond.accept(task);
            }
            // delayed
            while (!task.delayedTasks.isEmpty() && task.delayedTasks.peek().delayed <= task.nowTick) {
                task.delayedTasks.poll().tasks.forEach(t -> t.accept(task));
            }
            // update tick count
            // must run at last, to make sure has tick 0
            task.nowTick++;
        });
        // clear
        TASKS.removeIf(task -> !task.getStatus().isActive());
    }

    @SuppressWarnings("unused")
    public Status getStatus() {
        return status;
    }

    @SuppressWarnings("unused")
    public int nowTick() {
        return nowTick;
    }
    @SuppressWarnings("unused")
    public int nowSecond() {
        return nowTick / TICKS_PER_SECOND;
    }

    @SuppressWarnings("unused")
    public void done() {
        updateStatus(Status.Done);
    }
    @SuppressWarnings("unused")
    public void cancel() {
        updateStatus(Status.Canceled);
    }

    private void updateStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        Active,
        Done,
        Canceled;

        @SuppressWarnings("unused")
        public boolean isActive() {
            return this == Status.Active;
        }
        @SuppressWarnings("unused")
        public boolean isDone() {
            return this == Status.Done;
        }
        @SuppressWarnings("unused")
        public boolean isCanceled() {
            return this == Status.Canceled;
        }
    }

    public class Builder {
        @SuppressWarnings("unused")
        @CheckReturnValue
        @Contract("_ -> this")
        public Builder everyTick(Consumer<DelayedTask> consumer) {
            if (everyTick != null) {
                throw new IllegalStateException("everyTick has already been set");
            }
            everyTick = consumer;
            return this;
        }
        @SuppressWarnings("unused")
        @CheckReturnValue
        @Contract("_ -> this")
        public Builder everySecond(Consumer<DelayedTask> consumer) {
            if (everySecond != null) {
                throw new IllegalStateException("everySecond has already been set");
            }
            everySecond = consumer;
            return this;
        }
        @SuppressWarnings("unused")
        @CheckReturnValue
        @Contract("_, _ -> this")
        public Builder afterTick(int tick, Consumer<DelayedTask> consumer) {
            for (Task task : delayedTasks) {
                if (task.delayed() == tick) {
                    task.tasks.add(consumer);
                    return this;
                }
            }

            ArrayList<Consumer<DelayedTask>> list = new ArrayList<>();
            list.add(consumer);
            delayedTasks.add(new Task(tick, list));
            return this;
        }
        @SuppressWarnings("unused")
        @CheckReturnValue
        @Contract("_, _ -> this")
        public Builder afterSecond(int second, Consumer<DelayedTask> consumer) {
            return afterTick(second * TICKS_PER_SECOND, consumer);
        }

        public void run() {
            TASKS.add(DelayedTask.this);
        }
    }

    private record Task(int delayed, List<Consumer<DelayedTask>> tasks) implements Comparable<Task> {
        @Override public int compareTo(@NonNull Task o) {
            return Integer.compare(this.delayed(), o.delayed());
        }
    }
}
