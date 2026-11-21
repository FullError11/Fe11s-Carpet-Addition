package feca.utils.mixin;

public final class PickupRangeHolder {
    public interface Getter {
        PickupRangeHolder feca$getPickupRange();
    }

    private int extraRange;

    /// @return 玩家的额外水平拾取距离
    public int getExtraRange() {
        return extraRange;
    }

    /// @return 设定后的值
    @SuppressWarnings("UnusedReturnValue")
    public int setExtraRange(int extraRange) {
        this.extraRange = extraRange;
        return extraRange;
    }

    public void clear() {
        this.extraRange = 0;
    }
}
