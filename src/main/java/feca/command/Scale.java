package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.argument.DoubleArgument;
import feca.command.utils.Execute;
import feca.command.utils.ServerCommand;
import feca.rule.FECARules;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class Scale implements ServerCommand {
    interface Command {
        String NAME = "scale";
    }
    private static final DoubleArgument SCALE_VALUE = DoubleArgument.of("value");

    public @NonNull LiteralArgumentBuilder<CommandSourceStack> serverCommand(@NonNull CommandBuildContext context) {
        return Commands.literal(Command.NAME)
                .requires(stack -> CommandHelper.canUseCommand(stack, FECARules.commandScale))
                .executes(new Status())
                .then(SCALE_VALUE.require(context).executes(new ApplyScale()));
    }

    private static class Status implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return Execute.runIfFromPlayer(context, player -> {
                player.sendSystemMessage(Component.translatable(
                        "feca.cmd.scale.status",
                        getPlayerScale(player).getBaseValue(),
                        FECARules.commandScaleMin,
                        FECARules.commandScaleMax
                ));
                return SINGLE_SUCCESS;
            });
        }
    }

    private static class ApplyScale implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return Execute.runIfFromPlayer(context, player -> {
                var scaleValue = SCALE_VALUE.get(context);
                if (scaleValue < FECARules.commandScaleMin || scaleValue > FECARules.commandScaleMax) {
                    player.sendSystemMessage(Component.translatable("feca.cmd.scale.outOfRange",
                            FECARules.commandScaleMin, FECARules.commandScaleMax, scaleValue).withStyle(ChatFormatting.RED));
                    return SINGLE_FAIL;
                }

                var scale = getPlayerScale(player);
                scale.setBaseValue(scaleValue);
                player.sendSystemMessage(Component.translatable(
                        "feca.cmd.scale.successful", scale.getBaseValue()));
                return SINGLE_SUCCESS;
            });
        }
    }

    private static AttributeInstance getPlayerScale(@NotNull ServerPlayer player) {
        return player.getAttribute(Attributes.SCALE);
    }
}
