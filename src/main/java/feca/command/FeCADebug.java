package feca.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import feca.FeCA;
import feca.command.arguments.AbstractArgument;
import feca.command.utils.CommandResult;
import feca.registry.OnInitializeCallbackRegistry;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class FeCADebug {
    public static class Vars extends HashMap<String, Vars.Var<?>> {
        public static abstract class Var<T> {
            private String name;
            @NonNull private T value;

            public Var(@NonNull T value) {
                this.value = value;
            }

            public T value() { return value; }
            public void set(T value) { this.value = value; }

            abstract void fromString(String str) throws CommandSyntaxException;
            @Override
            public String toString() { return value.toString(); }

            public String typename() { return value.getClass().getSimpleName(); }

            private void setName(String name) { this.name = name; }
            public String name() { return name; }
        }

        private record VarArgumentType(Vars vars) implements ArgumentType<String> {
            private static final DynamicCommandExceptionType UNKNOWN_VARIABLE =
                    new DynamicCommandExceptionType(name -> Component.literal("变量不存在: " + name));

            @Contract(value = "_ -> new", pure = true)
            public static @NonNull VarArgumentType of(Vars vars) {
                return new VarArgumentType(vars);
            }

            @Override
            public @NonNull String parse(@NonNull StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String name = reader.readUnquotedString();
                if (!vars.containsKey(name)) {
                    reader.setCursor(start);
                    throw UNKNOWN_VARIABLE.createWithContext(reader, name);
                }
                return name;
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
                String remaining = builder.getRemaining();
                for (String name : vars.keySet()) {
                    if (name.startsWith(remaining)) {
                        builder.suggest(name);
                    }
                }
                return builder.buildFuture();
            }

            @Contract(pure = true)
            @Override
            public @NonNull @Unmodifiable Collection<String> getExamples() {
                return List.copyOf(vars.keySet());
            }
        }

        final VarArgumentType VAR_ARGUMENT_TYPE = VarArgumentType.of(this);

        static class VarArgument extends AbstractArgument.NonBuildContext<Var<?>> {
            private final Vars vars;

            public VarArgument(Vars vars, String name) {
                super(name);
                this.vars = vars;
            }

            @Override
            protected ArgumentType<?> argumentType() {
                return vars.VAR_ARGUMENT_TYPE;
            }

            @Override
            public Var<?> get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
                return vars.get(ctx.getArgument(name, String.class));
            }

            public String getName(@NonNull CommandContext<CommandSourceStack> ctx) {
                return ctx.getArgument(name, String.class);
            }
        }

        /**
         * 注册一个变量。名称唯一，name 一旦设置不可更改。
         */
        public <R> Var<R> register(String name, Var<R> var) {
            if (this.containsKey(name)) {
                throw new IllegalArgumentException("var " + name + " already exists");
            }
            var.setName(name);
            super.put(name, var);
            return var;
        }

        /// 回调 {@link #register(String, Var)}
        @Override
        public Var<?> put(String name, Var<?> var) {
            register(name, var);
            return null;
        }
    }

    public static final Vars VARS = new Vars();

    private static final Map<String, SubCommandRegistry> CUSTOM_SUB_COMMANDS = new LinkedHashMap<>();

    public interface SubCommandRegistry {
        LiteralArgumentBuilder<CommandSourceStack> register(LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext buildContext, Commands.CommandSelection selection);
    }
    /**
     * 注册自定义子命令。必须在 {@link #register()} 的初始化回调执行前调用。
     */
    public static void registerSubCommand(String name, SubCommandRegistry registry) {
        if (CUSTOM_SUB_COMMANDS.putIfAbsent(name, registry) != null) {
            throw new IllegalArgumentException("sub-command already exists: " + name);
        }
    }

    public static void register() {
        OnInitializeCallbackRegistry.register(() -> {
            ArgumentTypeRegistry.registerArgumentType(
                    FeCA.id("debugger/vars/var"),
                    Vars.VarArgumentType.class,
                    SingletonArgumentInfo.contextFree(() -> FeCADebug.VARS.VAR_ARGUMENT_TYPE)
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            var varArg = new Vars.VarArgument(VARS, "var");

            var cmd = Commands.literal("FeCADebug");

            cmd = cmd.then(Commands.literal("vars")
                    .then(varArg.require()
                            .then(Commands.literal("get")
                                    .executes(ctx -> printVar(ctx, varArg)))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(ctx -> setVar(ctx, varArg))))
                    )
            );

            var sub = Commands.literal("sub");
            for (var entry : CUSTOM_SUB_COMMANDS.entrySet()) {
                var branch = entry.getValue().register(Commands.literal(entry.getKey()), buildContext, selection);
                sub.then(branch);
            }
            cmd = cmd.then(sub);

            dispatcher.register(cmd);
        });
    }

    private static int printVar(CommandContext<CommandSourceStack> ctx, Vars.@NonNull VarArgument varArg) throws CommandSyntaxException {
        var var = varArg.get(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Var: %s %s = \"%s\"", var.typename(), var.name(), var.value())), false);
        return CommandResult.SUCCESS;
    }

    private static int setVar(CommandContext<CommandSourceStack> ctx, Vars.@NonNull VarArgument varArg) throws CommandSyntaxException {
        var var = varArg.get(ctx);
        var value = StringArgumentType.getString(ctx, "value");

        var oldVarValue = var.toString();
        var.fromString(value);
        var newVarValue = var.toString();

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Set var %s: \"%s\" -> \"%s\"", var.name(), oldVarValue, newVarValue)), true);

        return CommandResult.SUCCESS;
    }
}