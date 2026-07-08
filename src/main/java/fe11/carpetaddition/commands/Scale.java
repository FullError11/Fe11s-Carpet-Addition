package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.commands.utils.Executor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;


public class Scale {
    private static final String SCALE_VALUE = "scale";

    public static void registerCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("scale")
            .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandScale))
                .executes(Scale::info)
                .then(Commands.argument(SCALE_VALUE, DoubleArgumentType.doubleArg())
                    .executes(Scale::execute)
                )
        );
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, player -> {
            var scale = getPlayerScale(player);
            player.sendSystemMessage(Component.translatable(
                    "feca.message.command.scale.info",
                    scale.getBaseValue(),
                    FecaCarpetSettings.playerScaleMinValue,
                    FecaCarpetSettings.playerScaleMaxValue
            ));
            return Command.SINGLE_SUCCESS;
        });
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, player -> {
            var scaleValue = ctx.getArgument(SCALE_VALUE, Double.class);
            if (scaleValue < FecaCarpetSettings.playerScaleMinValue
                    || scaleValue > FecaCarpetSettings.playerScaleMaxValue) {
                player.sendSystemMessage(Component.translatable(
                        "feca.message.command.scale.outOfRange",
                        FecaCarpetSettings.playerScaleMinValue,
                        FecaCarpetSettings.playerScaleMaxValue, scaleValue
                ).withStyle(ChatFormatting.RED));
                return -1;
            }

            var scale = getPlayerScale(player);
            scale.setBaseValue(scaleValue);
            player.sendSystemMessage(Component.translatable(
                    "feca.message.command.scale.successful", scale.getBaseValue()));
            return Command.SINGLE_SUCCESS;
        });
    }

    private static AttributeInstance getPlayerScale(@NotNull ServerPlayer player) {
        return player.getAttribute(Attributes.SCALE);
    }
}
