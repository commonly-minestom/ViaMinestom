package com.viaversion.minestom.platform;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import com.viaversion.minestom.util.Slf4jLoggerAdapter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.PlatformTask;
import com.viaversion.viaversion.api.platform.ViaPlatform;
import java.io.File;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.Git;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.LoggerFactory;

public final class MinestomViaPlatform implements ViaPlatform<Player> {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final Logger logger = new Slf4jLoggerAdapter(LoggerFactory.getLogger("ViaVersion"));
    private final MinestomViaApi api = new MinestomViaApi();
    private final File dataFolder;
    private final MinestomViaConfig config;

    public MinestomViaPlatform(final File dataFolder) {
        this.dataFolder = dataFolder;
        this.config = new MinestomViaConfig(dataFolder, logger);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public String getPlatformName() {
        return "Minestom";
    }

    @Override
    public String getPlatformVersion() {
        return Git.version();
    }

    @Override
    public PlatformTask<?> runSync(final Runnable runnable) {
        return new MinestomViaTask(MinecraftServer.getSchedulerManager().scheduleNextTick(runnable));
    }

    @Override
    public PlatformTask<?> runSync(final Runnable runnable, final long delay) {
        if (delay <= 0) {
            return runSync(runnable);
        }
        return new MinestomViaTask(MinecraftServer.getSchedulerManager()
            .buildTask(runnable)
            .delay(TaskSchedule.tick(Math.toIntExact(delay)))
            .schedule());
    }

    @Override
    public PlatformTask<?> runRepeatingSync(final Runnable runnable, final long period) {
        return new MinestomViaTask(MinecraftServer.getSchedulerManager()
            .buildTask(runnable)
            .repeat(TaskSchedule.tick(Math.toIntExact(period)))
            .schedule());
    }

    @Override
    public void sendMessage(final UserConnection connection, final String message) {
        final Player player = ViaPlayerConnection.player(connection);
        if (player != null) {
            player.sendMessage(LEGACY_SERIALIZER.deserialize(message));
        }
    }

    @Override
    public boolean kickPlayer(final UserConnection connection, final String message) {
        final Player player = ViaPlayerConnection.player(connection);
        if (player == null) {
            return false;
        }
        player.kick(LEGACY_SERIALIZER.deserialize(message));
        return true;
    }

    @Override
    public void sendCustomPayload(final UserConnection connection, final String channel, final byte[] message) {
        final Player player = ViaPlayerConnection.player(connection);
        if (player != null) {
            EventDispatcher.call(new PlayerPluginMessageEvent(player, channel, message));
        }
    }

    @Override
    public void sendCustomPayloadToClient(final UserConnection connection, final String channel, final byte[] message) {
        final Player player = ViaPlayerConnection.player(connection);
        if (player != null) {
            player.sendPluginMessage(channel, message);
        }
    }

    @Override
    public MinestomViaApi getApi() {
        return api;
    }

    @Override
    public MinestomViaConfig getConf() {
        return config;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }
}
