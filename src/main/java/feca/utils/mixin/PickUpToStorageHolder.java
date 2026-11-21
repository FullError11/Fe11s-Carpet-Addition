package feca.utils.mixin;

public final class PickUpToStorageHolder {
    public interface Getter {
        PickUpToStorageHolder feca$getPickUpToStorage();
    }

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    /// 设定开关
    /// @return 设定后的状态
    public boolean setEnabled(boolean enabled) {
        this.enabled = enabled;
        return enabled;
    }

    /// 切换开关
    /// @return 切换后的状态
    public boolean toggle() {
        return enabled = !enabled;
    }
}
