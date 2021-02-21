package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;

public class ShrineManager implements Listener {
    private final ShrineCraftPlugin plugin;

    private BukkitTask itemAnimationTask = null;

    private HashSet<Item> allItems = new HashSet<>();
    private Multimap<Location, Item> currentlyCrafting = MultimapBuilder.hashKeys().linkedListValues().build();
    private Multimap<Material, Shrine> shrineMap = MultimapBuilder.enumKeys(Material.class).linkedListValues().build();

    public ShrineManager(final ShrineCraftPlugin plugin, double refreshPerSecond,double degreesPerSecond) {
        this.plugin = plugin;
        update(refreshPerSecond, degreesPerSecond);
 

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemLand(ItemLandEvent event) {
        Item item = event.getItem();
        Block landedOnBlock = event.getLandedOBlock();

        if (!shrineMap.containsKey(landedOnBlock.getType()))
            return;
        put(landedOnBlock.getLocation(), item);
        Collection<Item> savedItems = currentlyCrafting.get(landedOnBlock.getLocation());

        for (Shrine shrine : shrineMap.get(landedOnBlock.getType())) {
            if (shrine.isAt(landedOnBlock)) {
                ItemStack craftResult = shrine.getRecepie(savedItems);
                if(craftResult != null){
                    remove(landedOnBlock.getLocation(), true);
                    Item itemEntity = (Item) landedOnBlock.getWorld().spawnEntity(landedOnBlock.getLocation().clone().add(0.5, 1.5, 0.5),
                        EntityType.DROPPED_ITEM);
                    itemEntity.setItemStack(craftResult);
                    itemEntity.setVelocity(new Vector());
                    return;
                } else if (shrine.hasSimilarRecepie(savedItems)){
                    return;
                }
            }
        }
        remove(landedOnBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (allItems.contains(event.getTarget()) || allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(EntityPickupItemEvent event) {
        if (allItems.contains(event.getItem()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item && allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(ItemDespawnEvent event) {
        if (allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    public void update(double refreshPerSecond, double degreesPerSecond) {
        this.refreshRate = (int)Math.round(20/refreshPerSecond);
        this.degreesPerSecond = degreesPerSecond;
        this.radiansPerFrame = degreesPerSecond/20*refreshRate*Math.PI/180;

        shrineMap.clear();
        for (Object shrineObject : plugin.getConfig().getList("shrines")) {
            Shrine  shrine = new Shrine(Utils.objectToConfigurationSection(shrineObject));
            shrineMap.put(shrine.getCraftingBlockMaterial(), shrine);
        }
 
        if(itemAnimationTask != null)
            itemAnimationTask.cancel();
        itemAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateItems, 0, refreshRate);
        
    }

    private int refreshRate = 10;
    private double degreesPerSecond = 30;
    private double radiansPerFrame = degreesPerSecond/20*refreshRate;

    private static final Vector NORTH = new Vector(1,0,0);
    private long time = 0;
    private void animateItems() {
        for (Location location : currentlyCrafting.keySet()) {
            Location center = location.clone().add(0.5, 2.5, 0.5);
            Collection<Item> items = currentlyCrafting.get(location);
            int total = items.size();
            double degreeIncrement = (2 * Math.PI) / total;
            Vector start = NORTH.clone().multiply(total/Math.PI/4).rotateAroundY(time*radiansPerFrame);
            double maxSpeed = total > 1 ? start.clone().rotateAroundY(radiansPerFrame).subtract(start.clone()).multiply(1.0/refreshRate).length()*1.25 : 0.25/refreshRate;
            double maxSpeedSquared = maxSpeed * maxSpeed;
            for (Item item : items) {
                Vector expectedPosition = start.rotateAroundY(degreeIncrement).clone();
                Vector currentPosition = item.getLocation().clone().toVector().subtract(center.toVector());
                Vector actualPath = expectedPosition.rotateAroundY(radiansPerFrame).subtract(currentPosition).multiply(1.0/refreshRate);
                double actualSpeed = actualPath.lengthSquared();
                if(actualSpeed > maxSpeedSquared)
                    actualPath = actualPath.normalize().multiply(maxSpeed);
                item.setVelocity(actualPath);
            }
        }
        ++time;
    }

    private void put(Location location, Item item) {
        if (item.getItemStack().getAmount() == 1) {
            item.setVelocity(new Vector());
            item.setGravity(false);
            currentlyCrafting.put(location, item);
            allItems.add(item);
        } else {
            ItemStack itemStack = item.getItemStack().clone();
            int amount = itemStack.getAmount();
            itemStack.setAmount(1);
            item.remove();
            for (int i = 0; i < amount; i++) {
                Item newItem = (Item) location.getWorld().spawnEntity(location.clone().add(0.5, 1.5, 0.5),
                        EntityType.DROPPED_ITEM);
                newItem.setItemStack(itemStack);
                newItem.setVelocity(new Vector());
                newItem.setGravity(false);
                currentlyCrafting.put(location, newItem);
                allItems.add(newItem);
            }

        }
    }

    private void remove(Location location) {
        remove(location, false);
    }

    private void remove(Location location, boolean delete) {
        Collection<Item> items = currentlyCrafting.removeAll(location);
        for (Item item : items) {
            allItems.remove(item);
            if (delete)
                item.remove();
            else
                item.setGravity(true);
        }
    }
}
