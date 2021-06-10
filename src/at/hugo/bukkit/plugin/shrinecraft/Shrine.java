package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
    private final static @NotNull ItemStack NO_PREVIEW = new ItemStack(Material.BARRIER);

    private final @NotNull LinkedHashSet<Item> items = new LinkedHashSet<>();

    private final @NotNull ShrineCraftPlugin plugin;

    private final @NotNull Player player;
    private final @NotNull ShrineInfo shrineInfo;

    private final @NotNull Location center;
    private final @NotNull Location itemRotationCenter;
    private final @NotNull Location itemOutputLocation;

    private final @NotNull Consumer<Shrine> onTimeout;
    private final @NotNull BiConsumer<Shrine, ItemStack> finishedCraftingConsumer;

    private final Item previewItem;
    private final Block interactBlock;

    private long timeoutTicks;

    private boolean isCrafting = false;
    private BukkitTask timeoutTask = null;


    public Shrine(final @NotNull ShrineCraftPlugin plugin, final @NotNull Player owner, final @NotNull ShrineInfo shrineInfo, final @NotNull Location center,
                  final @NotNull Consumer<Shrine> onTimeout, final @NotNull BiConsumer<Shrine, ItemStack> finishedCrafting) {
        this.plugin = plugin;
        this.player = owner;
        this.shrineInfo = shrineInfo;

        this.center = center;
        this.itemRotationCenter = center.clone().add(0, 3, 0);
        this.itemOutputLocation = center.clone().add(0, 1.5, 0);
        this.interactBlock = itemOutputLocation.getBlock();

        this.onTimeout = onTimeout;
        this.finishedCraftingConsumer = finishedCrafting;

        previewItem = (Item) itemOutputLocation.getWorld().spawnEntity(itemOutputLocation, EntityType.DROPPED_ITEM, CreatureSpawnEvent.SpawnReason.CUSTOM);
        previewItem.setFireTicks(0);
        previewItem.setGravity(false);
        previewItem.setInvulnerable(true);
        previewItem.setPersistent(false);
        previewItem.setTicksLived(1);
        previewItem.setVelocity(Utils.VECTOR_ZERO);

        previewItem.setCanMobPickup(false);
        previewItem.setCanPlayerPickup(false);
        previewItem.setWillAge(false);

        reload();

        renewTimeoutTask();
    }

    public boolean onTriggerBlock(List<Block> blocks) {
        if (blocks.contains(interactBlock)) {
            if (canCraftItem()) {
                timeoutTask.cancel();
                isCrafting = true;
            } else {
                onTimeout.accept(this);
            }
            return true;
        }
        return false;
    }

    public void reload() {
        timeoutTicks = plugin.getConfig().getLong("shrine-timeout") * 20L;
    }

    private void renewTimeoutTask() {
        if (timeoutTask != null)
            timeoutTask.cancel();
        timeoutTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> onTimeout.accept(this), timeoutTicks);
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
        ItemStack itemStack = shrineInfo.tryCraft(this.items);
        if (itemStack == null) {
            previewItem.setItemStack(NO_PREVIEW);
        } else {
            previewItem.setItemStack(itemStack);
        }
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
    public List<Item> disbandShrine() {
        previewItem.remove();
        timeoutTask.cancel();

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
        if (!isCrafting) {
            shrineInfo.getIdleAnimation().animate(List.copyOf(items), itemRotationCenter, time, radiansPerFrame, ticksTillNextFrame);
        } else if (shrineInfo.getMergeAnimation().animate(List.copyOf(items), previewItem.getLocation(), time, radiansPerFrame, ticksTillNextFrame)) {
            finishedCraftingConsumer.accept(this, shrineInfo.tryCraft(items));
        }
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return center;
    }

    public @NotNull Location getItemRotationCenter() {
        return itemRotationCenter;
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

    public boolean isCrafting() {
        return isCrafting;
    }

    public Location getItemOutputLocation() {
        return itemOutputLocation;
    }
}
