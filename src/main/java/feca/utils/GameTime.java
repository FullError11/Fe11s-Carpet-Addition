package feca.utils;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class GameTime {
    public static final int TICKS_PER_SECOND = 20;

    public int seconds;
    public short ticks;

    public GameTime(int seconds, short ticks) {
        this.seconds = seconds;
        this.ticks = ticks;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull GameTime ofTicks(int ticks) {
        int s = Math.floorDiv(ticks, TICKS_PER_SECOND);
        short t = (short) Math.floorMod(ticks, TICKS_PER_SECOND);
        return new GameTime(s, t);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull GameTime ofSeconds(int seconds) {
        return new GameTime(seconds, (short)0);
    }

    public void addTicks(int ticks) {
        setTotalTicks(totalTicks() + ticks);
    }

    public void add(@NonNull GameTime gameTime) {
        addTicks(gameTime.totalTicks());
    }

    private void setTotalTicks(int total) {
        this.seconds = Math.floorDiv(total, TICKS_PER_SECOND);
        this.ticks = (short) Math.floorMod(total, TICKS_PER_SECOND);
    }

    public int totalTicks() {
        return seconds * TICKS_PER_SECOND + ticks;
    }

    public boolean isWholeSecond() {
        return ticks == 0;
    }


    @SuppressWarnings("UnusedReturnValue")
    public boolean tickAdd() {
        if (++this.ticks >= TICKS_PER_SECOND) {
            this.ticks = 0;
            this.seconds++;
            return true;
        }
        return false;
    }


    @SuppressWarnings("UnusedReturnValue")
    public boolean tickSub() {
        if (--this.ticks < 0) {
            this.ticks = (short) (TICKS_PER_SECOND - 1);
            this.seconds--;
            return true;
        }
        return false;
    }
}