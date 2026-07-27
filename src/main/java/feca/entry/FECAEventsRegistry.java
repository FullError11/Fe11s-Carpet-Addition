package feca.entry;

import carpet.CarpetServer;
import feca.function.StopCreeperGriefing;
import feca.function.VillagerDropSpawnEgg;
import feca.render.ObserverFreezeAreasRender;
import feca.rule.RuleChangedEvents;
import feca.utils.AnvilRegisterServer;
import feca.utils.DelayedTask;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;

public class FECAEventsRegistry {
    public static void registerServerEvents() {
        // 基础组件
        ServerTickEvents.END_SERVER_TICK.register(server -> DelayedTask.tick());
        // 命令
        CommandRegistrationCallback.EVENT.register(new FECACommandsRegistry());
        // 规则功能 辅助
        EnchantmentEvents.ALLOW_ENCHANTING.register(new AnvilRegisterServer());
        // 规则功能 核心
        ServerLivingEntityEvents.AFTER_DEATH.register(new VillagerDropSpawnEgg());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(new StopCreeperGriefing());
    }

    public static void registerCarpetServerEvents() {
        CarpetServer.settingsManager.registerRuleObserver(new RuleChangedEvents());
    }

    public static void registerClientEvents() {
        // 命令
        ClientCommandRegistrationCallback.EVENT.register(new FECACommandsRegistry());
        // 命令功能
        WorldRenderEvents.BEFORE_ENTITIES.register(new ObserverFreezeAreasRender());
    }
}
