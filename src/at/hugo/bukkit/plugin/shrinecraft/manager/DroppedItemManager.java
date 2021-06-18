package at.hugo.bukkit.plugin.shrinecraft.manager;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
            final Block landedAt;
            if (item.isInWater() || item.isInLava() || item.getLocation().getBlock().getType().equals(Material.LAVA)) {
                landedAt = item.getLocation().getBlock();
            } else if (item.isOnGround() && Math.abs(item.getVelocity().getX()) <= 0.01 && Math.abs(item.getVelocity().getZ()) <= 0.01) {
                landedAt = item.getLocation().getBlock().getRelative(BlockFace.DOWN);
            } else {
                double distance = item.getVelocity().lengthSquared();
                if(distance > 2){
                    distance = Math.sqrt(distance);
                } else if(distance < 1){
                    distance = 1;
                }
                RayTraceResult rayTraceResult = item.getLocation().getWorld().rayTraceBlocks(item.getLocation().clone(), item.getVelocity().lengthSquared() <= 0 ? new Vector(0,-1,0) : item.getVelocity(), distance, FluidCollisionMode.ALWAYS, false);
                if (rayTraceResult != null && rayTraceResult.getHitBlock() != null) {
                    Block rayTracedBlock = rayTraceResult.getHitBlock();
                    if (rayTracedBlock.getType().equals(Material.LAVA)) {
                        landedAt = rayTracedBlock;
                    } else if (rayTracedBlock.getType().equals(Material.FIRE)) {
                        landedAt = rayTracedBlock.getRelative(BlockFace.DOWN);
                    } else {
                        landedAt = null;
                    }
                } else {
                    landedAt = null;
                }
            }
            if (landedAt != null) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new ItemLandEvent(item, landedAt, uuid)));
                iterator.remove();
            }
        }
    }
}
