package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Shrine {
    private final @NotNull LinkedHashSet<Item> items = new LinkedHashSet<>();

    private final @NotNull ShrineCraftPlugin plugin;

    private final @NotNull Player player;
    private final @NotNull ShrineInfo shrineInfo;
    private final @NotNull Location center;
    private final @NotNull Location itemCenter;

    private final @NotNull Consumer<Shrine> onTimeout;

    private BiConsumer<Shrine, ItemStack> finishedCraftingConsumer;
    private boolean crafting = false;
    private BukkitTask timeoutTask = null;

    public Shrine(final @NotNull ShrineCraftPlugin plugin, final @NotNull Player owner, final @NotNull ShrineInfo shrineInfo, final @NotNull Location center, final @NotNull Consumer<Shrine> onTimeout) {
        this.plugin = plugin;
        this.player = owner;
        this.shrineInfo = shrineInfo;

        this.center = center;
        this.itemCenter = center.clone().add(0, 2, 0);

        this.onTimeout = onTimeout;

        renewTimeoutTask();
    }

    private void renewTimeoutTask() {
        if (timeoutTask != null)
            timeoutTask.cancel();
        timeoutTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> onTimeout.accept(this), 20L * 15);
    }

    public boolean onItemMerge(final ItemMergeEvent event) {
        if (items.contains(event.getEntity()) || items.contains(event.getTarget())) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    public boolean canCraftItem() {
        return shrineInfo.tryCraft(items) != null;
    }

    public void startCrafting(final @NotNull BiConsumer<Shrine, ItemStack> finishedCrafting) {
        crafting = true;
        finishedCraftingConsumer = finishedCrafting;
    }

    public boolean canAddItem(Item item) {
        LinkedList<Item> items = new LinkedList<>(this.items);
        items.add(item);
        return shrineInfo.hasSimilarRecipe(items);
    }

    public void addItem(Item item) {
        List<Item> items = splitItem(item);
        items.forEach(this::prepareItem);
        this.items.addAll(items);
        renewTimeoutTask();
    }

    public void addAllItems(List<Item> items) {
        items.forEach(this::addItem);
    }

    private void prepareItem(Item item) {
        item.getPersistentDataContainer().set(plugin.getModifiedItemKey(), PersistentDataType.BYTE, (byte) 1);
        item.setGravity(false);
        item.setFireTicks(0);
        item.setInvulnerable(true);
        item.setCanMobPickup(false);
        item.setCanPlayerPickup(false);
        item.setWillAge(false);
        item.setTicksLived(1);
    }

    public boolean isAt(Block block) {
        return shrineInfo.isAt(block);
    }


    @NotNull
    public List<Item> removeItems() {
        List<Item> result = new LinkedList<>(items);
        items.clear();
        for (Item item : result) {
            item.setCanPlayerPickup(true);
            item.setVelocity(Utils.VECTOR_ZERO);
            item.setWillAge(true);
        }
        return result;
    }

    public void animate(long time, double radiansPerFrame, int ticksTillNextFrame) {
        if (!crafting) {
            shrineInfo.getIdleAnimation().animate(List.copyOf(items), itemCenter, time, radiansPerFrame, ticksTillNextFrame);
        } else if (shrineInfo.getMergeAnimation().animate(List.copyOf(items), itemCenter, time, radiansPerFrame, ticksTillNextFrame)) {
            finishedCraftingConsumer.accept(this, shrineInfo.tryCraft(items));
        }
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return center;
    }

    public @NotNull Location getItemCenter() {
        return itemCenter;
    }


    private static List<Item> splitItem(Item item) {
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
}
