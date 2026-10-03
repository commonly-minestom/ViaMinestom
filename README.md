# ViaMinestom

Runs [ViaVersion](https://github.com/ViaVersion/ViaVersion) and [ViaBackwards](https://github.com/ViaVersion/ViaBackwards)
inside a [Minestom](https://github.com/Minestom/Minestom) server, so that clients from 1.9 up to versions newer than
the server can join it directly, without a proxy in between. [ViaRewind](https://github.com/ViaVersion/ViaRewind) adds
1.7 and 1.8 on top through its own Minestom module.

## Usage

Install the library into the local Maven repository with `./gradlew publishToMavenLocal`, then depend on it:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.viaversion.com")
}

dependencies {
    implementation("com.viaversion:viaminestom:1.0.0-SNAPSHOT")
}
```

Start the server through `ViaMinestom` instead of `MinecraftServer#start`:

```java
MinecraftServer server = MinecraftServer.init();
// instances, event listeners, commands...

ViaMinestom.create().start(server, "0.0.0.0", 25565);
```

The configuration files of the three projects are created under `via/` on the first start. The directory, the
permission check of the `/viaversion` command and the socket implementation can be changed through the builder:

```java
ViaMinestom.builder()
    .dataDirectory(Path.of("config", "via"))
    .commandAuthorizer((sender, permission) -> permissions.has(sender, permission))
    .build()
    .start(server, "0.0.0.0", 25565);
```

Further addons are passed to the builder as well. With the Minestom module of ViaRewind on the classpath:

```java
ViaMinestom.builder()
    .addon(new ViaMinestomAddon())
    .build()
    .start(server, "0.0.0.0", 25565);
```

The version a player is really on is available from the regular Via API (`Via.getAPI().getPlayerProtocolVersion(uuid)`)
or from the connection itself:

```java
if (player.getPlayerConnection() instanceof ViaPlayerConnection connection) {
    ProtocolVersion version = connection.clientVersion();
}
```

## Packet interceptors

Libraries that need to see the packets of a connection, an anticheat for instance, register a
`PacketInterceptorFactory` with `PacketInterceptors`. Their interceptor is shown every packet twice, in the version of
the client and in the version of the server, and may change or drop it at either stage. Injecting packets at a stage is
done through `ViaPlayerConnection#write` and `ViaPlayerConnection#read`.

Code that relies on the Via API while the server is still being set up can call `ViaMinestom#load()` ahead of `start`.

## How it works

Minestom reads and writes its sockets itself, while Via translates packets inside a Netty pipeline. ViaMinestom
therefore accepts the clients on a Netty server of its own and hands every channel to Minestom as a
`PlayerSocketConnection`, which keeps authentication, proxy forwarding, compression and the packet events of Minestom
working as usual:

```
socket <-> cipher <-> framing <-> compression <-> [interceptors] <-> Via <-> [interceptors] <-> ViaPlayerConnection <-> Minestom
```

Packets are still parsed, dispatched and serialized on virtual threads, one pair per connection. Clients on the
version of the server skip the translation entirely and receive the packets Minestom has framed ahead of time as they
are.

The socket server built into Minestom is bound to a private socket file and stays unused, which means that
`MinecraftServer.getServer()` does not report the public address. Use `ViaMinestom#address()` for that.

## Building

```
./gradlew build
```

Requires Java 25, like Minestom itself.
