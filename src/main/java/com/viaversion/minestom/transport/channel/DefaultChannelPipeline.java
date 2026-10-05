package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

final class DefaultChannelPipeline implements ChannelPipeline {
    private static final String HEAD_NAME = "head";
    private static final String TAIL_NAME = "tail";

    private final AbstractChannel channel;
    private final DefaultChannelHandlerContext head;
    private final DefaultChannelHandlerContext tail;

    DefaultChannelPipeline(final AbstractChannel channel) {
        this.channel = channel;
        this.head = new DefaultChannelHandlerContext(this, HEAD_NAME, new Head());
        this.tail = new DefaultChannelHandlerContext(this, TAIL_NAME, new Tail());
        tail.linkAfter(head);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public synchronized ChannelPipeline addLast(final String name, final ChannelHandler handler) {
        insert(tail.previous(), name, handler);
        return this;
    }

    @Override
    public synchronized ChannelPipeline addBefore(final String baseName, final String name, final ChannelHandler handler) {
        insert(require(baseName).previous(), name, handler);
        return this;
    }

    @Override
    public synchronized ChannelPipeline addAfter(final String baseName, final String name, final ChannelHandler handler) {
        insert(require(baseName), name, handler);
        return this;
    }

    @Override
    public synchronized ChannelPipeline remove(final ChannelHandler handler) {
        final DefaultChannelHandlerContext context = lookup(handler);
        if (context == null) {
            throw new NoSuchElementException(handler.getClass().getName());
        }
        detach(context);
        return this;
    }

    @Override
    public synchronized ChannelHandler remove(final String name) {
        final DefaultChannelHandlerContext context = require(name);
        detach(context);
        return context.handler();
    }

    @Override
    public synchronized ChannelHandler replace(final String oldName, final String newName, final ChannelHandler newHandler) {
        final DefaultChannelHandlerContext replaced = require(oldName);
        if (!oldName.equals(newName)) {
            requireUnused(newName);
        }
        attach(replaced, prepare(newName, newHandler));
        detach(replaced);
        return replaced.handler();
    }

    @Override
    public @Nullable ChannelHandler first() {
        final DefaultChannelHandlerContext first = head.next();
        return first == tail ? null : first.handler();
    }

    @Override
    public @Nullable ChannelHandler get(final String name) {
        final DefaultChannelHandlerContext context = lookup(name);
        return context == null ? null : context.handler();
    }

    @Override
    public @Nullable ChannelHandlerContext context(final ChannelHandler handler) {
        return lookup(handler);
    }

    @Override
    public @Nullable ChannelHandlerContext context(final String name) {
        return lookup(name);
    }

    @Override
    public List<String> names() {
        final List<String> names = new ArrayList<>();
        for (DefaultChannelHandlerContext context = head.next(); context != tail; context = context.next()) {
            names.add(context.name());
        }
        return names;
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        final Map<String, ChannelHandler> handlers = new LinkedHashMap<>();
        for (DefaultChannelHandlerContext context = head.next(); context != tail; context = context.next()) {
            handlers.put(context.name(), context.handler());
        }
        return handlers;
    }

    @Override
    public ChannelPipeline fireChannelRead(final Object message) {
        head.fireChannelRead(message);
        return this;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(final Throwable cause) {
        head.fireExceptionCaught(cause);
        return this;
    }

    void uncaught(final Throwable cause) {
        channel.exceptionCaught(cause);
    }

    private void insert(final DefaultChannelHandlerContext predecessor, final String name, final ChannelHandler handler) {
        requireUnused(name);
        attach(predecessor, prepare(name, handler));
    }

    private DefaultChannelHandlerContext prepare(final String name, final ChannelHandler handler) {
        Objects.requireNonNull(name, "name");
        claim(Objects.requireNonNull(handler, "handler"));
        return new DefaultChannelHandlerContext(this, name, handler);
    }

    private void attach(final DefaultChannelHandlerContext predecessor, final DefaultChannelHandlerContext context) {
        context.linkAfter(predecessor);
        try {
            context.handler().handlerAdded(context);
        } catch (final Throwable cause) {
            context.unlink();
            fireExceptionCaught(cause);
        }
    }

    private void detach(final DefaultChannelHandlerContext context) {
        context.unlink();
        try {
            context.handler().handlerRemoved(context);
        } catch (final Throwable cause) {
            fireExceptionCaught(cause);
        }
    }

    private void requireUnused(final String name) {
        if (lookup(name) != null) {
            throw new IllegalArgumentException("Duplicate handler name: " + name);
        }
    }

    private DefaultChannelHandlerContext require(final String name) {
        final DefaultChannelHandlerContext context = lookup(name);
        if (context == null) {
            throw new NoSuchElementException(name);
        }
        return context;
    }

    private @Nullable DefaultChannelHandlerContext lookup(final @Nullable String name) {
        for (DefaultChannelHandlerContext context = head.next(); context != tail; context = context.next()) {
            if (context.name().equals(name)) {
                return context;
            }
        }
        return null;
    }

    private @Nullable DefaultChannelHandlerContext lookup(final ChannelHandler handler) {
        for (DefaultChannelHandlerContext context = head.next(); context != tail; context = context.next()) {
            if (context.handler() == handler) {
                return context;
            }
        }
        return null;
    }

    private static void claim(final ChannelHandler handler) {
        if (!(handler instanceof ChannelHandlerAdapter adapter)) {
            return;
        }
        if (adapter.added && !adapter.isSharable()) {
            throw new IllegalStateException(handler.getClass().getName() + " is not sharable and belongs to a pipeline already");
        }
        adapter.added = true;
    }

    private final class Head extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) {
            channel.write(message, promise);
        }
    }

    private final class Tail extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(final ChannelHandlerContext context, final Object message) {
            ReferenceCountUtil.release(message);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) {
            channel.exceptionCaught(cause);
        }
    }
}
