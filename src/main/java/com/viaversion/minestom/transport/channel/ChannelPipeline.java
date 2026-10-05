package com.viaversion.minestom.transport.channel;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface ChannelPipeline {

    Channel channel();

    ChannelPipeline addLast(String name, ChannelHandler handler);

    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);

    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);

    ChannelPipeline remove(ChannelHandler handler);

    ChannelHandler remove(String name);

    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    @Nullable ChannelHandler first();

    @Nullable ChannelHandler get(String name);

    @Nullable ChannelHandlerContext context(ChannelHandler handler);

    @Nullable ChannelHandlerContext context(String name);

    List<String> names();

    Map<String, ChannelHandler> toMap();

    ChannelPipeline fireChannelRead(Object message);

    ChannelPipeline fireExceptionCaught(Throwable cause);
}
