package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CollectibleSpawnEggOptions;
import feca.rule.utils.RuleImpl;
import feca.utils.EnchantmentHelper;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import static feca.FeCA.LOGGER;

public class CollectibleSpawnEgg implements RuleImpl {
    public static CollectibleSpawnEggOptions getOption() {
        return Rules.collectibleSpawnEgg;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
            UseEntityCallback.EVENT.register((player, level, interactionHand, entity, _) -> {
                if (getOption() ==  CollectibleSpawnEggOptions.BUCKET) {
                    if (player.getItemInHand(interactionHand).is(Items.BUCKET)) {
                        // 对于交易过的村民，返回PASS (禁止收起+不阻断进入交易界面)
                        if (entity instanceof Villager villager) {
                            if (villager.getVillagerXp() != 0) {
                                return InteractionResult.PASS;
                            }
                        }

                        return SpawnEggItem.byId(entity.getType()).<InteractionResult>map(item -> {
                            if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity livingEntity) {
                                PoiReleaser.AUTO.release(serverLevel.getServer(), livingEntity);
                            }
                            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(item));
                            entity.discard();
                            return InteractionResult.SUCCESS;
                        })
                        // 失败时，返回PASS
                        // 否则阻断其它操作影响体验(比如右键船)
                        .orElse(InteractionResult.PASS);
                    }
                }
                return InteractionResult.PASS;
            });
        });
    }

    private static boolean itemStackHasSilkTouch(@NonNull ItemStack stack) {
        return EnchantmentHelper.has(stack, Enchantments.SILK_TOUCH);
    }
    private static boolean dropSpawnEgg(Level level, @NonNull Entity entity) {
        var spawnEgg = SpawnEggItem.byId(entity.getType());
        if (spawnEgg.isEmpty()) return false;

        Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(spawnEgg.get()));
        return true;
    }

    public static boolean dropSpawnEggWhenDieIfEnabled(LivingEntity entity, ServerLevel level, DamageSource source) {
        if (CollectibleSpawnEgg.getOption() == CollectibleSpawnEggOptions.KILL
                && source.getEntity() instanceof Player player) {
            if (itemStackHasSilkTouch(player.getItemInHand(InteractionHand.MAIN_HAND))
                    || itemStackHasSilkTouch(player.getItemInHand(InteractionHand.OFF_HAND))) {
                return CollectibleSpawnEgg.dropSpawnEgg(level, entity);
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface PoiReleaser<T extends LivingEntity> {
        void release(@NonNull MinecraftServer server, @NonNull T entity);

        /// see [兴趣点类型](https://zh.minecraft.wiki/w/%E5%85%B4%E8%B6%A3%E7%82%B9#%E5%85%B4%E8%B6%A3%E7%82%B9%E7%B1%BB%E5%9E%8B)

        PoiReleaser<Villager> VILLAGER = (server, villager) -> {
            var brain = villager.getBrain();
            // 床 (village)
            forgetAndReleasePoi(server, brain, MemoryModuleType.HOME);
            // 工作方块 (acquirable_job_site | village)
            forgetAndReleasePoi(server, brain, MemoryModuleType.JOB_SITE);
            // 钟/集合点 (village)
            forgetAndReleasePoi(server, brain, MemoryModuleType.MEETING_POINT);
        };

        PoiReleaser<LivingEntity> AUTO = (server, entity) -> {
            if (entity instanceof Villager) {
                VILLAGER.release(server, (Villager)entity);
            }
        };

        private static void forgetAndReleasePoi(
                @NonNull MinecraftServer server, @NonNull Brain<?> brain, MemoryModuleType<GlobalPos> type) {
            brain.getMemory(type).ifPresent(globalPos -> {
                var level = server.getLevel(globalPos.dimension());
                var dimensionId = globalPos.dimension().identifier();
                var pos = globalPos.pos();
                if (level == null) {
                    LOGGER.warn("[POI] Dimension {} not loaded, cannot release {} at {}", dimensionId, type, pos);
                } else {
                    var poiManager = level.getPoiManager();
                    // 预检存在性，避免 release 抛 IllegalStateException
                    if (poiManager.getType(globalPos.pos()).isPresent()) {
                        if (poiManager.release(globalPos.pos())) {
                            LOGGER.debug("[POI] Released {} at {} in {}", type, pos, dimensionId);
                        } else {
                            LOGGER.warn("[POI] release() returned false for {} at {} in {}", type, pos, dimensionId);
                        }
                    } else {
                        LOGGER.debug("[POI] No POI record at {} in {}, skip release", pos, dimensionId);
                    }
                }

                // 无论 release 是否执行成功，都清除这条记忆
                brain.eraseMemory(type);
                LOGGER.debug("[POI] Erased memory {}", type);
            });
        }
    }
}
