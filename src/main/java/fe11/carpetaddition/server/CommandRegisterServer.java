package fe11.carpetaddition.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fe11.carpetaddition.Feca;
import fe11.carpetaddition.*;
import fe11.carpetaddition.commands.*;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandRegisterServer {
    private static final List<Object> COMMANDS = new ArrayList<>(Arrays.asList(
            new Fly(),
            new Home(),
            new ObserverFreezeAreas(),
            new Scale()
    ));

    public interface ServerCommandRegister {
        LiteralArgumentBuilder<CommandSourceStack> registerServerCommand();
    }
    public interface ClientCommandRegister {
        LiteralArgumentBuilder<FabricClientCommandSource> registerClientCommand();
    }

    public static final CommandRegisterServer INSTANCE = new CommandRegisterServer();

    private CommandRegisterServer() {}

    public void registerServerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        COMMANDS.forEach(command -> {
            if (command instanceof ServerCommandRegister serverCommand) {
                dispatcher.register(serverCommand.registerServerCommand());
            }
        });
        if (Feca.DEBUG_MODE) {
            Feca.LOGGER.info("[CommandRegisterServer] Registered server commands");
        }
    }
    public void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        COMMANDS.forEach(command -> {
            if (command instanceof ClientCommandRegister clientCommand) {
                dispatcher.register(clientCommand.registerClientCommand());
            }
        });
        if (Feca.DEBUG_MODE) {
            Feca.LOGGER.info("[CommandRegisterServer] Registered client commands");
        }
    }
}
