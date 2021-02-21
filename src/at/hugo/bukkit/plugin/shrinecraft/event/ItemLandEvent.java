package at.hugo.bukkit.plugin.shrinecraft.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ItemLandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled = false;
    private Item item;
    private Block landedOnBlock;

    public ItemLandEvent(Item item, Block landedOnBlock) {
        this.landedOnBlock = landedOnBlock;
        this.item = item;
    }

    public ItemLandEvent(boolean isAsync, Item item, Block landedOnBlock) {
        super(isAsync);
        this.landedOnBlock = landedOnBlock;
        this.item = item;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public HandlerList getHandlers() {
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

    public Item getItem() {
        return item;
    }

    public Block getLandedOBlock() {
        return landedOnBlock;
    }
}
