package feca.test.support;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * 让测试 JVM 拥有与游戏一致的常量与内置注册表。
 *
 * <p>{@code ItemStack}、{@code DataComponents}、{@code Items} 等类强依赖
 * {@code BuiltInRegistries}，不引导会直接抛 {@code NullPointerException} /
 * {@code IllegalStateException}（"Registry not bootstrapped"）。</p>
 *
 * <p>顺序固定为：先 {@link SharedConstants#tryDetectVersion()} 固定版本信息，
 * 再 {@link Bootstrap#bootStrap()} 建立注册表。多个测试类共享同一个 JVM，
 * 因此这里做成幂等的一次性初始化。</p>
 */
public final class GameBootstrap {
    private static boolean bootstrapped = false;

    private GameBootstrap() {}

    /// 幂等的注册表引导；重复调用无副作用
    public static synchronized void ensure() {
        if (bootstrapped) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bootstrapped = true;
    }
}
