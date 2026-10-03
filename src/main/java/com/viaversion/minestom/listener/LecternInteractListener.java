package com.viaversion.minestom.listener;

import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.book.FilteredText;
import net.minestom.server.item.component.WritableBookContent;
import net.minestom.server.item.component.WrittenBookContent;
import org.jetbrains.annotations.Nullable;

/**
 * Clients before 1.14 cannot display the lectern screen, so the book lying on it is opened for them directly.
 */
public final class LecternInteractListener extends ProtocolEventListener<PlayerBlockInteractEvent> {
    private static final String BOOK_TAG = "Book";

    public LecternInteractListener(final EventNode<Event> node) {
        super(node, PlayerBlockInteractEvent.class, Protocol1_14To1_13_2.class);
    }

    @Override
    protected void handle(final PlayerBlockInteractEvent event) {
        final Block block = event.getBlock();
        if (event.isCancelled() || !block.compare(Block.LECTERN) || !isOnPipe(event.getPlayer())) {
            return;
        }

        final CompoundBinaryTag blockData = block.nbt();
        if (blockData == null) {
            return;
        }

        final Book book = readBook(blockData.getCompound(BOOK_TAG));
        if (book != null) {
            event.getPlayer().openBook(book);
            event.setBlockingItemUse(true);
            event.setCancelled(true);
        }
    }

    private static @Nullable Book readBook(final CompoundBinaryTag itemData) {
        if (itemData.isEmpty()) {
            return null;
        }

        final ItemStack item;
        try {
            item = ItemStack.fromItemNBT(itemData, MinecraftServer.getRegistries());
        } catch (final RuntimeException e) {
            return null;
        }

        final WrittenBookContent written = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            final List<Component> pages = new ArrayList<>(written.pages().size());
            for (final FilteredText<Component> page : written.pages()) {
                pages.add(page.text());
            }
            return Book.book(Component.text(written.title().text()), Component.text(written.author()), pages);
        }

        final WritableBookContent writable = item.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writable != null) {
            final List<Component> pages = new ArrayList<>(writable.pages().size());
            for (final FilteredText<String> page : writable.pages()) {
                pages.add(Component.text(page.text()));
            }
            return Book.book(Component.empty(), Component.empty(), pages);
        }
        return null;
    }
}
