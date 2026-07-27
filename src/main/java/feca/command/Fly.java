package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.FECA;
import feca.command.argument.BooleanArgument;
import feca.command.utils.Execute;
import feca.command.utils.ServerCommand;
import feca.rule.FECARules;
import feca.rule.option.BooleanOption;
import feca.utils.Attachment;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public class Fly implements ServerCommand {
    private static final BooleanArgument ARGUMENT_MODE = BooleanArgument.of("mode");
    private interface Command {
        String NAME = "fly";
    }

    @Override
    public @NonNull LiteralArgumentBuilder<CommandSourceStack> serverCommand(@NonNull CommandBuildContext context) {
        return Commands.literal(Command.NAME)
                .requires(stack -> CommandHelper.canUseCommand(stack, FECARules.commandFly))
                .executes(new SwitchMode())
                .then(ARGUMENT_MODE.require(context).executes(new SetMode()));
    }

    private static final Attachment<Boolean> FLY_MODE = Attachment.Boolean(FECA.id("fly_mode"), false);

    public static boolean playerIsFlyMode(@NonNull ServerPlayer player) {
        return FLY_MODE.getOrCreate(player);
    }

    private static class SwitchMode implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return Execute.runIfFromPlayer(context, player -> {
                SetMode.setMode(player, !playerIsFlyMode(player));
                return SINGLE_SUCCESS;
            });
        }
    }

    private static class SetMode implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return Execute.runIfFromPlayer(context, player -> {
                setMode(player, ARGUMENT_MODE.get(context));
                return SINGLE_SUCCESS;
            });
        }

        public static void setMode(ServerPlayer player, boolean mode) {
            if (FLY_MODE.getOrCreate(player) == mode) {
                player.sendSystemMessage(Component.translatable("feca.cmd.fly.nochange", String.valueOf(mode)).withStyle(ChatFormatting.RED));
                return;
            }

            // noinspection PointlessBooleanExpression
            if (mode == true) {
                FLY_MODE.set(player, true);
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                player.sendSystemMessage(Component.translatable("feca.cmd.fly.enabled").withStyle(ChatFormatting.GREEN));
            } else {
                FLY_MODE.set(player, false);
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.sendSystemMessage(Component.translatable("feca.cmd.fly.disabled").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (BooleanOption.isFalse(FECARules.commandFly) && Fly.playerIsFlyMode(player)) {
            SetMode.setMode(player, false);
        }
    }
}
