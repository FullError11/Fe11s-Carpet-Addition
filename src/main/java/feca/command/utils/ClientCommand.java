package feca.command.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import org.jspecify.annotations.NonNull;

public interface ClientCommand {
    @NonNull LiteralArgumentBuilder<FabricClientCommandSource> clientCommand(@NonNull CommandBuildContext context);
}
