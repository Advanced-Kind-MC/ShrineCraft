package at.hugo.bukkit.plugin.shrinecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    //private HashMap<Location, AbstractIdleAnimation> idleAnimations = new HashMap<>();
    //private Multimap<Location, Item> currentlyCrafting = MultimapBuilder.hashKeys().linkedListValues().build();
    private HashMap<Location, Shrine> activeShrines = new HashMap<Location, Shrine>();
    private Multimap<Material, ShrineInfo> shrineMap = MultimapBuilder.enumKeys(Material.class).linkedListValues().build();

    public ShrineManager(final ShrineCraftPlugin plugin, int refreshPerSecond, double degreesPerSecond) {
        this.plugin = plugin;
        update(refreshPerSecond, degreesPerSecond);

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (allItems.contains(event.getTarget()) || allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (allItems.contains(event.getItem()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemDestroy(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item && allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (allItems.contains(event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemLand(ItemLandEvent event) {
        final Item item = event.getItem();
        final Block landedOnBlock = event.getLandedOBlock();
        final Location location = landedOnBlock.getLocation().clone().add(0.5, 0.5, 0.5);

        if (!shrineMap.containsKey(landedOnBlock.getType()))
            return;
        final List<Item> items = new LinkedList<>();
        if (activeShrines.containsKey(location)) {
            Shrine shrine = activeShrines.get(location);
            if (shrine.canAddItem(item) && shrine.isAt(landedOnBlock)) {
                List<Item> splitItems = splitItem(item);
                shrine.addAllItems(splitItems);
                allItems.addAll(splitItems);
                // item added to the shrine, everything went well!
                if (shrine.canCraftItem()) {
                    shrine.startCrafting((shrine1, craftedItem) -> {
                        activeShrines.remove(location);
                        shrine.discardItems().forEach(this::removeItem);
                        items.forEach(this::removeItem);
                        createItemEntity(landedOnBlock, craftedItem);
                    });
                }
                return;
            }
            activeShrines.remove(location);
            items.addAll(shrine.discardItems());
        }

        final List<Item> oldItems = List.copyOf(items);
        items.add(item);

        for (ShrineInfo shrineInfo : shrineMap.get(landedOnBlock.getType())) {
            if (shrineInfo.isAt(landedOnBlock)) {
                if (shrineInfo.hasSimilarRecipe(items)) {
                    Shrine shrine = new Shrine(shrineInfo, location);
                    List<Item> splitItems = splitItem(item);
                    shrine.addAllItems(splitItems);
                    shrine.addAllItems(oldItems);
                    allItems.addAll(splitItems);
                    activeShrines.put(location, shrine);
                    return;
                }
            }
        }
        allItems.removeAll(oldItems);
    }

    private Item createItemEntity(Block floorLocation, ItemStack itemStack) {
        Item item = (Item) floorLocation.getWorld().spawnEntity(
                floorLocation.getLocation().clone().add(0.5, 1.5, 0.5), EntityType.DROPPED_ITEM);
        item.setItemStack(itemStack);
        item.setGravity(false);
        item.setVelocity(new Vector());
        return item;
    }


    public void update(int ticksBetweenRefreshes, double degreesPerSecond) {
        this.ticksBetweenRefreshes = ticksBetweenRefreshes;
        this.radiansPerFrame = (degreesPerSecond / (20D / (this.ticksBetweenRefreshes + 1))) * (Math.PI / 180);

        shrineMap.clear();
        for (Object shrineObject : plugin.getConfig().getList("shrines")) {
            ShrineInfo shrineInfo = new ShrineInfo(Utils.objectToConfigurationSection(shrineObject));
            shrineMap.put(shrineInfo.getCraftingBlockMaterial(), shrineInfo);
        }

        if (itemAnimationTask != null)
            itemAnimationTask.cancel();
        itemAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateItems, 0, this.ticksBetweenRefreshes);

    }

    private List<Item> splitItem(Item item) {
        if (item.getItemStack().getAmount() == 1) return List.of(item);
        final Location location = item.getLocation();
        final ItemStack itemStack = item.getItemStack().clone();
        final int amount = itemStack.getAmount();
        final List<Item> result = new ArrayList<>(amount);
        itemStack.setAmount(1);
        item.remove();
        for (int i = 0; i < amount; i++) {
            Item newItem = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
            newItem.setItemStack(itemStack);
            newItem.setVelocity(new Vector());
            result.add(newItem);
        }
        return result;
    }

    private void removeItem(final Item item) {
        allItems.remove(item);
        item.remove();
    }


    private int ticksBetweenRefreshes = 10;
    private double framesPerSecond = 20D / ticksBetweenRefreshes;
    private double radiansPerFrame = 30 / 20 * ticksBetweenRefreshes;
    private long time = 0;

    private void animateItems() {
        for (Shrine shrine : activeShrines.values()) {
            shrine.animate(time, radiansPerFrame, ticksBetweenRefreshes+1);
        }
        ++time;
//        for (Location location : currentlyCrafting.keySet()) {
//            Location center = location.clone().add(0.5, 2.5, 0.5);
//            LinkedList<Item> items = currentlyCrafting.get(location).stream()
//                    .collect(Collectors.toCollection(LinkedList::new));
//            int total = items.size();
//            double degreeIncrement = (2 * Math.PI) / total;
//            Vector start = new Vector();
//            double maxSpeed = 0;
//            double maxSpeedSquared = 0;
//            Iterator<Item> iterator = items.descendingIterator();
//            int i = 0;
//            int lastRadius = -1;
//            while (iterator.hasNext()) {
//                Item item = iterator.next();
//                int currentRadius = radia.ceilingEntry(i).getValue();
//                if (lastRadius != currentRadius) {
//                    lastRadius = currentRadius;
//                    start = NORTH.clone().multiply(currentRadius)
//                            .rotateAroundY((time * radiansPerFrame) % (2 * Math.PI));
//                    maxSpeed = currentRadius > 1
//                            ? start.clone().rotateAroundY(radiansPerFrame).subtract(start.clone())
//                            .multiply(1.0 / refreshRate).length() * 1.25
//                            : .5 / refreshRate;
//                    maxSpeedSquared = maxSpeed * maxSpeed;
//                    if (i == 0)
//                        degreeIncrement = 2 * Math.PI;
//                    else
//                        degreeIncrement = (2 * Math.PI) / (Math.min(total, radia.higherKey(i) + 1) - radia.floorKey(i) - 1);
//                }
//                Vector expectedPosition = start.rotateAroundY(degreeIncrement).clone();
//                Vector currentPosition = item.getLocation().clone().toVector().subtract(center.toVector());
//                Vector actualPath = expectedPosition.rotateAroundY(radiansPerFrame).subtract(currentPosition)
//                        .multiply(1.0 / refreshRate);
//                double actualSpeed = actualPath.lengthSquared();
//                if (actualSpeed > maxSpeedSquared)
//                    actualPath = actualPath.normalize().multiply(maxSpeed);
//                item.setVelocity(actualPath);
//                ++i;
//            }
//        }
    }

//    private void put(Location location, Item item) {
//        if (item.getItemStack().getAmount() == 1) {
//            item.setVelocity(new Vector());
//            item.setGravity(false);
//            currentlyCrafting.put(location, item);
//            allItems.add(item);
//        } else {
//            ItemStack itemStack = item.getItemStack().clone();
//            int amount = itemStack.getAmount();
//            itemStack.setAmount(1);
//            item.remove();
//            for (int i = 0; i < amount; i++) {
//                Item newItem = (Item) location.getWorld().spawnEntity(location.clone().add(0.5, 1.5, 0.5),
//                        EntityType.DROPPED_ITEM);
//                newItem.setItemStack(itemStack);
//                newItem.setVelocity(new Vector());
//                newItem.setGravity(false);
//                currentlyCrafting.put(location, newItem);
//                allItems.add(newItem);
//            }
//
//        }
//    }

//    private void remove(Location location) {
//        remove(location, false);
//    }

//    private void remove(Location location, boolean delete) {
//        Collection<Item> items = currentlyCrafting.removeAll(location);
//        for (Item item : items) {
//            allItems.remove(item);
//            if (delete)
//                item.remove();
//            else
//                item.setGravity(true);
//        }
//    }
}
