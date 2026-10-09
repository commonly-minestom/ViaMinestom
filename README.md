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
}

dependencies {
    implementation("com.viaversion:viaminestom:1.0.0-SNAPSHOT")
}
```

ViaVersion and ViaBackwards are part of the jar, so neither of them has to be declared as a dependency.

Start the server through `ViaMinestom` instead of `MinecraftServer#start`:

```java
MinecraftServer server = MinecraftServer.init();
// instances, event listeners, commands...

ViaMinestom.create().start(server, "0.0.0.0", 25565);
```

The configuration files of the three projects are created under `via/` on the first start. The directory, the
permission check of the `/viaversion` command and the network settings can be changed through the builder:

```java
ViaMinestom.builder()
    .dataDirectory(Path.of("config", "via"))
    .commandAuthorizer((sender, permission) -> permissions.has(sender, permission))
    .network(NetworkSettings.defaults().withReadTimeout(Duration.ofSeconds(60)))
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

An addon built against the upstream ViaVersion artifacts has to go through the relocation described under
[How it works](#how-it-works) before it is put on the classpath, and must not bring a ViaVersion of its own along.

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
the client and in the version of the server, and may change or drop it at either stage.

Every buffer starts with the packet id, followed by the payload. An incoming packet is returned to pass it on, as the
very same instance unless it is replaced, or dropped by returning null. An outgoing packet continues once it is written
to the sink, which may happen at most once; not writing it drops it. All calls happen on the thread that owns the
connection, so an interceptor needs no synchronisation of its own.

Injecting packets at a stage is done through `ViaPlayerConnection#write` and `ViaPlayerConnection#read`, from any
thread. The overloads taking a `PacketStage` enter the pipeline right past the interceptors of that stage.

Code that relies on the Via API while the server is still being set up can call `ViaMinestom#load()` ahead of `start`.

## How it works

Minestom reads and writes its sockets itself, while ViaVersion translates packets through the channel API of its own
engine. ViaMinestom therefore accepts the clients on a socket server of its own, built on the JDK alone, and hands every
connection to Minestom as a `PlayerSocketConnection`, which keeps authentication, proxy forwarding, compression and the
packet events of Minestom working as usual:

```
socket <-> cipher <-> framing <-> compression <-> [interceptors] <-> Via <-> [interceptors] <-> ViaPlayerConnection <-> Minestom
```

Each connection is driven by three virtual threads. A reader blocks on the socket, strips the optional PROXY protocol
header, decrypts the stream and splits it into frames. An actor owns the whole protocol state and processes frames,
outgoing packets and the tasks of ViaVersion strictly in the order they arrive, batching the bytes it produces. A
writer blocks on the socket while sending those batches. The channel ViaVersion is given is an in-memory one whose
event loop is the actor of the connection, so the translation engine runs single-threaded exactly as it does on every
other platform, without an event loop group, a socket or a native transport of its own.

ViaVersion and ViaBackwards are compiled against the buffer and channel API of Netty. ViaMinestom does not depend on
Netty: `com.viaversion.minestom.transport` implements the part of that API the two libraries use, with heap buffers
and the in-memory channel, and the build rewrites every reference to `io.netty` inside them to that package before
bundling them into the jar. Before the jar is assembled, the `verifyLinkage` task resolves each of those references
against the transport classes and checks what the two libraries inherit from them, so an update of either library that
relies on something not implemented yet fails the build instead of a running server. Only lookups made through
reflection are beyond its reach.

Addons compiled against Netty need the same treatment. With the Shadow plugin that is a single rule:

```kotlin
tasks.shadowJar {
    relocate("io.netty", "com.viaversion.minestom.transport")
}
```

Unlike the two bundled libraries, an addon relocated this way is not verified, so anything it needs and the transport
classes lack only shows once it is used.

Clients on the version of the server skip the translation entirely and receive the packets Minestom has framed ahead
of time as they are. Connections that stay silent for longer than `NetworkSettings#readTimeout` are closed, and a
connection whose shutdown does not complete within `NetworkSettings#closeTimeout` is dropped.

A connection also has `NetworkSettings#loginTimeout` to get through the handshake and login, which stops clients that
trickle bytes to stay in them, and `NetworkSettings#configurationTimeout` to reach the play state; the latter is longer
so that large resource packs are not cut off. A client that falls more than `NetworkSettings#maxPendingWriteBytes` behind on what it is
sent is disconnected, and reading from a client is paused while more than `NetworkSettings#maxPendingReadBytes` of its
packets are waiting to be processed. `NetworkSettings#maxConnections` and `NetworkSettings#maxConnectionsPerAddress`
cap how many sockets are accepted; both are unlimited by default. The per-address limit counts the address of the TCP
peer, so behind a proxy every player shares the proxy's address and it should be left unset.

The socket server built into Minestom is bound to a private socket file and stays unused, which means that
`MinecraftServer.getServer()` does not report the public address. Use `ViaMinestom#address()` for that.

## Building

```
./gradlew build
```

Requires Java 25, like Minestom itself.
