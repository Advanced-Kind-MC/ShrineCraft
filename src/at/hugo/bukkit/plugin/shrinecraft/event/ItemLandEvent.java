package at.hugo.bukkit.plugin.shrinecraft.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ItemLandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled = false;
    private final @NotNull Item item;
    private final @NotNull Block landedOnBlock;
    private final @NotNull UUID dropper;

    public ItemLandEvent(@NotNull Item item, @NotNull Block landedOnBlock, @NotNull UUID dropper) {
        this.landedOnBlock = landedOnBlock;
        this.dropper = dropper;
        this.item = item;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return getHandlerList();
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    public @NotNull Item getItem() {
        return item;
    }

    public @NotNull Block getLandedOBlock() {
        return landedOnBlock;
    }

    /**
     * @return the UUID of the player who dropped the item
     */
    public @NotNull UUID getDropper() {
        return dropper;
    }
}
