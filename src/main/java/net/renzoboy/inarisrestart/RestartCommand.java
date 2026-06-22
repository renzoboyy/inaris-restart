package net.renzoboy.inarisrestart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RestartCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /restart <seconds> [reason]
        dispatcher.register(
                Commands.literal("restart")
                        // Commands.LEVEL_ADMINS == 2 — correct way in MC 26.1.2
                        .requires(src -> src.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                                ? src.getServer().getPlayerList().isOp(player.nameAndId())
                                : true)
                        .then(
                                Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                        .then(
                                                Commands.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeRestart(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                StringArgumentType.getString(ctx, "reason")))
                                        )
                                        .executes(ctx -> executeRestart(ctx,
                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                "Scheduled restart"))
                        )
        );

        // /cancelrestart
        dispatcher.register(
                Commands.literal("cancelrestart")
                        .requires(src -> src.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                                ? src.getServer().getPlayerList().isOp(player.nameAndId())
                                : true)
                        .executes(RestartCommand::executeCancelRestart)
        );
    }

    private static int executeRestart(CommandContext<CommandSourceStack> ctx, int seconds, String reason) {
        CommandSourceStack src = ctx.getSource();
        RestartManager manager = RestartManager.get(src.getServer());

        if (manager.isActive()) {
            manager.cancel();
            src.sendSuccess(() -> Component.literal("§ePrevious restart cancelled. Starting new countdown..."), true);
        }

        manager.startRestart(seconds, reason);

        String timeLabel = RestartManager.formatTime(seconds);
        src.sendSuccess(
                () -> Component.literal("§cRestart scheduled in §f" + timeLabel + "§c. Reason: §f" + reason),
                true
        );
        src.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§c[Server] §fRestarting in §c" + timeLabel + "§f. Reason: " + reason),
                false
        );
        return 1;
    }

    private static int executeCancelRestart(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        RestartManager manager = RestartManager.get(src.getServer());

        if (!manager.isActive()) {
            src.sendFailure(Component.literal("No restart is currently scheduled."));
            return 0;
        }

        manager.cancel();
        src.sendSuccess(() -> Component.literal("§aRestart cancelled."), true);
        src.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§a[Server] §fThe scheduled restart has been cancelled."),
                false
        );
        return 1;
    }
}