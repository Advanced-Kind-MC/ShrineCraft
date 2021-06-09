package at.hugo.bukkit.plugin.shrinecraft.manager;

import java.util.Iterator;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;

public class DroppedItemManager {
    private final @NotNull ShrineCraftPlugin plugin;
    private final ConcurrentHashMap<Item, UUID> trackedItems = new ConcurrentHashMap<>();

    public DroppedItemManager(final @NotNull ShrineCraftPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::itemLandedCheck, 0, 0);
    }

    public void add(@NotNull Item item, Player player) {
        trackedItems.put(item, player.getUniqueId());
    }

    public void add(@NotNull Item item, UUID uuid) {
        trackedItems.put(item, uuid);
    }

    public UUID remove(@NotNull Item item) {
        return trackedItems.remove(item);
    }

    private void itemLandedCheck() {
        if (trackedItems.isEmpty()) return;
        Iterator<Entry<Item, UUID>> iterator = trackedItems.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Item, UUID> entry = iterator.next();
            Item item = entry.getKey();
            if (!item.isValid()) {
                iterator.remove();
                continue;
            }
            UUID uuid = entry.getValue();
            if (item.isInWater() || item.isInLava()) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new ItemLandEvent(item, item.getLocation().getBlock(), uuid)));
            } else if (item.isOnGround() && Math.abs(item.getVelocity().getX()) <= 0.01 && Math.abs(item.getVelocity().getZ()) <= 0.01) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new ItemLandEvent(item, item.getLocation().getBlock().getRelative(BlockFace.DOWN), uuid)));
            } else {
                continue;
            }
            iterator.remove();
        }
    }
}
