package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;

public class DroppedItemManager {
    private WeakHashMap<Item, UUID> trackedItems = new WeakHashMap<>();
    public DroppedItemManager(ShrineCraftPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::itemLandedCheck, 0, 0);
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

    private void itemLandedCheck(){
        if(trackedItems.isEmpty()) return;
        Iterator<Entry<Item,UUID>> iterator = trackedItems.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Item,UUID> entry = iterator.next();
            Item item = entry.getKey();
            UUID uuid = entry.getValue();
            if(item.isDead()){
                
                Bukkit.getServer().broadcastMessage("Removed item "+ item.getItemStack().getType()+", which died by "+item.getLastDamageCause().getCause());
            } else if(item.isOnGround()){
                //Bukkit.getServer().broadcastMessage("Landed on the Ground "+ item.getItemStack().getType());
                Bukkit.getPluginManager().callEvent(new ItemLandEvent(item,item.getLocation().getBlock().getRelative(BlockFace.DOWN)));
            } else if( item.isInWater()) {
                //Bukkit.getServer().broadcastMessage("Landed in water "+ item.getItemStack().getType());
                Bukkit.getPluginManager().callEvent(new ItemLandEvent(item,item.getLocation().getBlock()));
            } else{
                continue;
            }
            iterator.remove();
        }
    }
}
