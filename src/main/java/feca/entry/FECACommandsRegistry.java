package feca.entry;

import com.mojang.brigadier.CommandDispatcher;
import feca.command.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import org.jspecify.annotations.NonNull;

public class FECACommandsRegistry implements CommandRegistrationCallback, ClientCommandRegistrationCallback {
    @Override
    public void register(@NonNull CommandDispatcher<CommandSourceStack> dispatcher, @NonNull CommandBuildContext context, @NonNull CommandSelection environment) {
        dispatcher.register(new Fly().serverCommand(context));
        dispatcher.register(new Home().serverCommand(context));
        dispatcher.register(new Scale().serverCommand(context));
        dispatcher.register(new ObserverFreezeAreas().serverCommand(context));
    }

    @Override
    public void register(@NonNull CommandDispatcher<FabricClientCommandSource> dispatcher, @NonNull CommandBuildContext context) {
        dispatcher.register(new ObserverFreezeAreas().clientCommand(context));
    }
}
