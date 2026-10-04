package com.viaversion.minestom;

import com.viaversion.minestom.addon.MinestomViaBackwards;
import com.viaversion.minestom.addon.ViaAddon;
import com.viaversion.minestom.command.CommandAuthorizer;
import com.viaversion.minestom.command.ViaVersionCommand;
import com.viaversion.minestom.network.NetworkServer;
import com.viaversion.minestom.network.NetworkSettings;
import com.viaversion.minestom.platform.MinestomViaInjector;
import com.viaversion.minestom.platform.MinestomViaPlatform;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class ViaMinestom {
    private final AtomicBoolean loaded = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Path dataDirectory;
    private final CommandAuthorizer commandAuthorizer;
    private final List<ViaAddon> addons;
    private final NetworkSettings networkSettings;
    private final NetworkServer networkServer;

    private ViaMinestom(final Builder builder) {
        this.dataDirectory = builder.dataDirectory;
        this.commandAuthorizer = builder.commandAuthorizer;
        this.addons = List.copyOf(builder.addons);
        this.networkSettings = builder.networkSettings;
        this.networkServer = new NetworkServer(networkSettings);
    }

    public static ViaMinestom create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start(final MinecraftServer server, final String host, final int port) {
        start(server, new InetSocketAddress(host, port));
    }

    public void start(final MinecraftServer server, final InetSocketAddress address) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("ViaMinestom has already been started");
        }

        load();
        server.start(internalAddress());
        ((ViaManagerImpl) Via.getManager()).onServerLoaded();

        networkServer.bind(address);
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::stop);
    }

    public void stop() {
        if (!started.get() || !stopped.compareAndSet(false, true)) {
            return;
        }

        networkServer.close();
        ((ViaManagerImpl) Via.getManager()).destroy();
    }

    public @Nullable InetSocketAddress address() {
        return networkServer.address();
    }

    public NetworkSettings networkSettings() {
        return networkSettings;
    }

    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }

        final MinestomViaPlatform platform = new MinestomViaPlatform(dataDirectory.resolve("viaversion").toFile());
        final ViaCommandHandler commandHandler = new ViaCommandHandler(true);
        final ViaManagerImpl manager = ViaManagerImpl.builder()
            .platform(platform)
            .injector(new MinestomViaInjector())
            .loader(ViaPlatformLoader.NOOP)
            .commandHandler(commandHandler)
            .build();
        Via.init(manager);
        platform.getConf().reload();

        for (final ViaAddon addon : addons) {
            addon.install(dataDirectory);
        }
        manager.init();

        MinecraftServer.getCommandManager().register(new ViaVersionCommand(commandHandler, commandAuthorizer));
    }

    private static SocketAddress internalAddress() {
        try {
            final Path directory = Files.createTempDirectory("viaminestom");
            directory.toFile().deleteOnExit();
            return UnixDomainSocketAddress.of(directory.resolve("minestom.sock"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static final class Builder {
        private Path dataDirectory = Path.of("via");
        private CommandAuthorizer commandAuthorizer = CommandAuthorizer.operators();
        private final List<ViaAddon> addons = new ArrayList<>(List.of(new MinestomViaBackwards()));
        private NetworkSettings networkSettings = NetworkSettings.defaults();

        private Builder() {
        }

        public Builder dataDirectory(final Path dataDirectory) {
            this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
            return this;
        }

        public Builder commandAuthorizer(final CommandAuthorizer commandAuthorizer) {
            this.commandAuthorizer = Objects.requireNonNull(commandAuthorizer, "commandAuthorizer");
            return this;
        }

        public Builder addon(final ViaAddon addon) {
            this.addons.add(Objects.requireNonNull(addon, "addon"));
            return this;
        }

        public Builder network(final NetworkSettings networkSettings) {
            this.networkSettings = Objects.requireNonNull(networkSettings, "networkSettings");
            return this;
        }

        public ViaMinestom build() {
            return new ViaMinestom(this);
        }
    }
}
