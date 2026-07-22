package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.commands.utils.Executor;
import fe11.carpetaddition.server.CommandRegisterServer;
import fe11.carpetaddition.utils.Attachment;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class Fly implements CommandRegisterServer.ServerCommandRegister {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> registerServerCommand() {
        return Commands.literal("fly")
                .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandFly))
                .executes(Fly::switchFlyMode)
                .then(Commands.literal("switch")
                        .executes(Fly::switchFlyMode)
                )
                .then(Commands.literal("enable")
                        .executes(Fly::enableFlyMode)
                )
                .then(Commands.literal("disable")
                        .executes(Fly::disableFlyMode)
                );
    }

    private static final Attachment<Boolean> isFlyMode = new Attachment<>(
            "player_fly_mode",
            builder -> builder.initializer(() -> false).persistent(Codec.BOOL)
    );

    private static int switchFlyMode(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, player -> {
            if (playerIsFlyMode(player)) {
                return disableFlyMode(player);
            } else {
                return enableFlyMode(player);
            }
        });
    }

    private static int enableFlyMode(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, Fly::enableFlyMode);
    }

    private static int disableFlyMode(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, Fly::disableFlyMode);
    }

    public static boolean playerIsFlyMode(Player player) {
        return isFlyMode.getOrCreate(player);
    }

    private static int enableFlyMode(ServerPlayer player) {
        isFlyMode.set(player, true);
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        player.sendSystemMessage(Component.translatable("feca.message.command.fly.enabled").withStyle(ChatFormatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int disableFlyMode(ServerPlayer player) {
        isFlyMode.set(player, false);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.sendSystemMessage(Component.translatable("feca.message.command.fly.disabled").withStyle(ChatFormatting.RED));
        return Command.SINGLE_SUCCESS;
    }
}
