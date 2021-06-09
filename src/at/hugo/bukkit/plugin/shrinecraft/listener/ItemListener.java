package at.hugo.bukkit.plugin.shrinecraft.listener;

import java.util.UUID;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.Utils;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import at.hugo.bukkit.plugin.shrinecraft.manager.DroppedItemManager;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class ItemListener implements Listener {
    private final @NotNull ShrineCraftPlugin plugin;
    private final @NotNull DroppedItemManager droppedItemManager;

    public ItemListener(final @NotNull ShrineCraftPlugin plugin, final @NotNull DroppedItemManager droppedItemManager) {
        this.plugin = plugin;
        this.droppedItemManager = droppedItemManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(EntityAddToWorldEvent event) {
        // only check items
        if (!event.getEntity().getType().equals(EntityType.DROPPED_ITEM)) {
            return;
        }
        Item item = (Item) event.getEntity();
        var data = event.getEntity().getPersistentDataContainer();
        if (data.has(plugin.getModifiedItemKey(), PersistentDataType.BYTE)) {
            data.remove(plugin.getModifiedItemKey());
            item.setCanPlayerPickup(true);
            item.setVelocity(Utils.VECTOR_ZERO);
            item.setWillAge(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        droppedItemManager.add(event.getItemDrop(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        UUID uuid = droppedItemManager.remove(event.getEntity());
        if (uuid == null) return;
        droppedItemManager.add(event.getTarget(), uuid);
    }
}
