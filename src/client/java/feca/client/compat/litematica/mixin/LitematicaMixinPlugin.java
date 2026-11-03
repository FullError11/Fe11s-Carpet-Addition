package feca.client.compat.litematica.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

/// 未安装 Litematica 时跳过整组兼容 mixin
public class LitematicaMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        enabled = FabricLoader.getInstance().isModLoaded("litematica");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }
}
