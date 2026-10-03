package com.viaversion.minestom;

import com.viaversion.minestom.addon.MinestomViaBackwards;
import com.viaversion.minestom.addon.MinestomViaRewind;
import com.viaversion.minestom.command.CommandAuthorizer;
import com.viaversion.minestom.command.ViaVersionCommand;
import com.viaversion.minestom.network.NetworkServer;
import com.viaversion.minestom.network.Transport;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point that starts a Minestom server with ViaVersion, ViaBackwards and ViaRewind in front of it.
 * <pre>{@code
 * MinecraftServer server = MinecraftServer.init();
 * // set up instances, listeners, ...
 * ViaMinestom.create().start(server, new InetSocketAddress("0.0.0.0", 25565));
 * }</pre>
 * It takes the place of {@link MinecraftServer#start(SocketAddress)}, which must not be called as well.
 */
public final class ViaMinestom {
    private final AtomicBoolean loaded = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Path dataDirectory;
    private final CommandAuthorizer commandAuthorizer;
    private final Transport transport;
    private final NetworkServer networkServer;

    private ViaMinestom(final Builder builder) {
        this.dataDirectory = builder.dataDirectory;
        this.commandAuthorizer = builder.commandAuthorizer;
        this.transport = builder.transport != null ? builder.transport : Transport.detect();
        this.networkServer = new NetworkServer(transport);
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

    /**
     * Starts the Minestom server and begins accepting clients of every supported version on the given address.
     */
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

    /**
     * Returns the address clients connect to, or null while the server is not running.
     */
    public @Nullable InetSocketAddress address() {
        return networkServer.address();
    }

    public Transport transport() {
        return transport;
    }

    /**
     * Loads ViaVersion and its addons without starting anything yet. {@link #start} does so on its own, calling
     * this beforehand is only needed by code that relies on the Via API while the server is still being set up.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }

        final MinestomViaPlatform platform = new MinestomViaPlatform(dataDirectory.resolve("viaversion").toFile());
        final ViaCommandHandler commandHandler = new ViaCommandHandler(true);
        final ViaManagerImpl manager = ViaManagerImpl.builder()
            .platform(platform)
            .injector(new MinestomViaInjector(transport))
            .loader(ViaPlatformLoader.NOOP)
            .commandHandler(commandHandler)
            .build();
        Via.init(manager);
        platform.getConf().reload();

        new MinestomViaBackwards(dataDirectory.resolve("viabackwards").toFile()).install();
        new MinestomViaRewind(dataDirectory.resolve("viarewind").toFile()).install();
        manager.init();

        MinecraftServer.getCommandManager().register(new ViaVersionCommand(commandHandler, commandAuthorizer));
    }

    /**
     * Minestom insists on binding its own socket server. Since clients have to come in through the Via pipeline
     * instead, that server is given a private socket file nobody connects to.
     */
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
        private @Nullable Transport transport;

        private Builder() {
        }

        /**
         * Sets the directory holding the configuration of ViaVersion and its addons, {@code via} by default.
         */
        public Builder dataDirectory(final Path dataDirectory) {
            this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
            return this;
        }

        /**
         * Sets who may use the {@code /viaversion} command, operators and the console by default.
         */
        public Builder commandAuthorizer(final CommandAuthorizer commandAuthorizer) {
            this.commandAuthorizer = Objects.requireNonNull(commandAuthorizer, "commandAuthorizer");
            return this;
        }

        /**
         * Forces a socket implementation instead of picking the best one available.
         */
        public Builder transport(final Transport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        public ViaMinestom build() {
            return new ViaMinestom(this);
        }
    }
}
