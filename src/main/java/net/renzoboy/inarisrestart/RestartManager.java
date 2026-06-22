package net.renzoboy.inarisrestart;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RestartManager {

    private static RestartManager instance;

    private final MinecraftServer server;
    private ServerBossEvent bossBar;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;

    private int totalSeconds;
    private int remainingSeconds;
    private String reason;

    private boolean active = false;

    public static RestartManager get(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new RestartManager(server);
        }
        return instance;
    }

    private RestartManager(MinecraftServer server) {
        this.server = server;
    }

    public void startRestart(int seconds, String reason) {
        if (active) {
            cancel();
        }

        this.totalSeconds = seconds;
        this.remainingSeconds = seconds;
        this.reason = reason;
        this.active = true;

        // BossBarOverlay is a nested enum inside BossEvent in MC 26.1.2
        bossBar = new ServerBossEvent(
                java.util.UUID.randomUUID(),
                buildBossBarName(),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.setProgress(1.0f);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
        }

        broadcastTitle();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "inaris-restart-timer");
            t.setDaemon(true);
            return t;
        });

        tickTask = scheduler.scheduleAtFixedRate(() -> {
            server.execute(() -> {
                remainingSeconds--;

                if (remainingSeconds <= 0) {
                    doRestart();
                    return;
                }

                float progress = (float) remainingSeconds / (float) totalSeconds;
                bossBar.setProgress(progress);
                bossBar.setName(buildBossBarName());

                if (shouldShowTitle(remainingSeconds)) {
                    broadcastTitle();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private boolean shouldShowTitle(int secs) {
        if (secs % 60 == 0) return true;
        if (secs == 30)      return true;
        if (secs <= 10)      return true;
        return false;
    }

    private Component buildBossBarName() {
        return Component.literal("Restarting in " + formatTime(remainingSeconds));
    }

    private void broadcastTitle() {
        Component title    = Component.literal("Restarting in " + formatTime(remainingSeconds));
        Component subtitle = Component.literal(reason);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    private void doRestart() {
        active = false;
        if (tickTask != null) tickTask.cancel(false);

        server.execute(() -> {
            if (bossBar != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    bossBar.removePlayer(player);
                }
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.disconnect(Component.literal("Server is restarting. Come back soon!"));
            }
            server.halt(false);
        });
    }

    public void cancel() {
        if (!active) return;
        active = false;

        if (tickTask  != null) tickTask.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();

        server.execute(() -> {
            if (bossBar != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    bossBar.removePlayer(player);
                }
                bossBar = null;
            }
        });
    }

    public void addPlayer(ServerPlayer player) {
        if (active && bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public boolean isActive() {
        return active;
    }

    static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (secs == 0) {
            return mins + " minute" + (mins == 1 ? "" : "s");
        }
        return mins + "m " + secs + "s";
    }
}