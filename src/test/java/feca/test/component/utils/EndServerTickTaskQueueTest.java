package feca.test.component.utils;

import feca.utils.EndServerTickTaskQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 组件测试: {@link EndServerTickTaskQueue}（服务端 tick 末尾的任务队列）。
 *
 * <p>队列是全局静态状态且没有 clear API，因此每个用例结束前都要把自己的任务跑完
 * （任务自我 discard），避免影响其它用例。</p>
 */
class EndServerTickTaskQueueTest {
    @Test
    @DisplayName("按添加顺序执行任务")
    void runsInInsertionOrder() {
        final List<String> ran = new ArrayList<>();
        EndServerTickTaskQueue.add(task -> {
            ran.add("first");
            return task.discard();
        });
        EndServerTickTaskQueue.add(task -> {
            ran.add("second");
            return task.discard();
        });

        EndServerTickTaskQueue.tick();

        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    @DisplayName("discard 后不再执行")
    void discardRemovesTask() {
        final int[] runs = {0};
        EndServerTickTaskQueue.add(task -> {
            runs[0]++;
            return task.discard();
        });

        EndServerTickTaskQueue.tick();
        EndServerTickTaskQueue.tick();
        EndServerTickTaskQueue.tick();

        assertEquals(1, runs[0], "discard 之后任务必须从队列移除");
    }

    @Test
    @DisplayName("pause 跳过指定刻数后再执行")
    void pauseSkipsTicks() {
        final List<Integer> runsAtTick = new ArrayList<>();
        final int[] currentTick = {0};
        EndServerTickTaskQueue.add(task -> {
            currentTick[0]++;
            if (currentTick[0] < 3) {
                return task.pause(2);   // 休息 2 刻：下一次在第 3 刻执行
            }
            runsAtTick.add(currentTick[0]);
            return task.discard();
        });

        for (int i = 0; i < 12; i++) {
            EndServerTickTaskQueue.tick();
        }

        assertEquals(List.of(3), runsAtTick, "第 3 次执行任务时应是队列的第 7 次 tick");
    }

    @Test
    @DisplayName("同一刻内多次 pause 会累加")
    void pauseAccumulates() {
        final List<Integer> runsAtTick = new ArrayList<>();
        final int[] currentTick = {0};
        EndServerTickTaskQueue.add(task -> {
            currentTick[0]++;
            if (currentTick[0] == 1) {
                task.pause(1);
                task.pause(1);   // 合计 2 刻
                return task.normal();
            }
            runsAtTick.add(currentTick[0]);
            return task.discard();
        });

        for (int i = 0; i < 6; i++) {
            EndServerTickTaskQueue.tick();
        }

        // pause(1)+pause(1) = 跳过 2 次 tick，任务在第 4 次 tick 调用时第二次执行
        assertEquals(List.of(2), runsAtTick, "累加的 pause 应跳过 2 次 tick");
    }

    @Test
    @DisplayName("normal 不改变任务状态")
    void normalKeepsTask() {
        final int[] runs = {0};
        EndServerTickTaskQueue.add(task -> {
            runs[0]++;
            return runs[0] < 2 ? task.normal() : task.discard();
        });

        EndServerTickTaskQueue.tick();
        EndServerTickTaskQueue.tick();
        assertEquals(2, runs[0]);
    }
}
