package feca.function;

import feca.FeCA;
import feca.function.utils.InfinityInventoryStorage;
import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public class InfinityInventory implements RuleImpl {
    static {
        ClassForceLoader.load(feca.command.InfinityInventory.class);
    }

    public static final AttachmentType<InfinityInventoryStorage> STORAGE_ATTACHMENT_TYPE =
            AttachmentRegistry.create(FeCA.id("infinity_inventory_storage"), builder -> builder
                    .persistent(InfinityInventoryStorage.NBT_CODEC)
                    .copyOnDeath()
                    .initializer(InfinityInventoryStorage::createWithDefaultProcessors)
            );

    public static @NonNull InfinityInventoryStorage getStorage(@NonNull Player player) {
        return player.getAttachedOrCreate(STORAGE_ATTACHMENT_TYPE);
    }
}
