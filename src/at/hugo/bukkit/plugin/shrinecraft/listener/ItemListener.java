package at.hugo.bukkit.plugin.shrinecraft.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import at.hugo.bukkit.plugin.shrinecraft.DroppedItemManager;

public class ItemListener implements Listener {
    private final DroppedItemManager droppedItemManager;

    public ItemListener(final DroppedItemManager droppedItemManager) {
        this.droppedItemManager = droppedItemManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        droppedItemManager.add(event.getItemDrop(), event.getPlayer());
        Bukkit.getLogger().info("New item: " + event.getItemDrop().getCustomName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        UUID uuid = droppedItemManager.remove(event.getEntity());
        if(uuid == null) return;
        droppedItemManager.add(event.getTarget(), uuid);
        Bukkit.getLogger().info("Item Merged: " + event.getTarget().getItemStack().getItemMeta().getDisplayName());
        Bukkit.getLogger().info("Item Merged to: " + event.getTarget().getItemStack().getItemMeta().getDisplayName());
    }
}
