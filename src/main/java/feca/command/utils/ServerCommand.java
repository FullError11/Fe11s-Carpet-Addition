package feca.command.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NonNull;

public interface ServerCommand {
    @NonNull LiteralArgumentBuilder<CommandSourceStack> serverCommand(@NonNull CommandBuildContext context);
}

