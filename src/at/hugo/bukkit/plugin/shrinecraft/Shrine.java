package at.hugo.bukkit.plugin.shrinecraft;

import at.hugo.bukkit.plugin.shrinecraft.manager.DesignManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class Shrine {
    private final static @NotNull ItemStack NO_PREVIEW = new ItemStack(Material.BARRIER);

    private final @NotNull LinkedHashSet<Item> items = new LinkedHashSet<>();

    private final @NotNull HashSet<Location> inputLocations = new HashSet<>();

    private final @NotNull ShrineCraftPlugin plugin;

    private final @NotNull Player player;
    private final @NotNull ShrineInfo shrineInfo;

    private final @NotNull Location center;
    private final @NotNull Location itemRotationCenter;
    private final @NotNull Location itemOutputLocation;

    private final @NotNull DesignManager.Design.Direction direction;
    private final @NotNull BlockVector offset;

    private final @NotNull Consumer<Shrine> onTimeout;
    private final @NotNull BiConsumer<Shrine, ItemStack> finishedCraftingConsumer;

    private final Item previewItem;
    private final Block interactBlock;

    private long timeoutTicks;

    private boolean isCrafting = false;
    private BukkitTask timeoutTask = null;


    public Shrine(final @NotNull ShrineCraftPlugin plugin, final @NotNull Player owner, final @NotNull ShrineInfo.ShrinePosition shrinePositionData,
                  final @NotNull Consumer<Shrine> onTimeout, final @NotNull BiConsumer<Shrine, ItemStack> finishedCrafting) {
        this.plugin = plugin;
        this.player = owner;
        this.shrineInfo = shrinePositionData.getShrineInfo();
        this.direction = shrinePositionData.getDirection();
        this.offset = shrineInfo.getInputOffsets()[0];

        this.center = shrinePositionData.getOrigin().clone().add(shrinePositionData.getDirection().rotateVector(offset.clone())).add(0.5, 0.5, 0.5);
        for (BlockVector inputOffset : shrineInfo.getInputOffsets()) {
            inputLocations.add(shrinePositionData.getOrigin().clone().add(shrinePositionData.getDirection().rotateVector(inputOffset.clone())));
        }
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
        previewItem.setTicksLived(2);
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
                previewItem.remove();
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

    public boolean containsItem(Entity item) {
        return items.contains(item) || previewItem.equals(item);
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
        item.setVelocity(Utils.VECTOR_ZERO);
        item.setGravity(false);
        item.setInvulnerable(true);
        item.setFireTicks(0);
        item.setCanMobPickup(false);
        item.setCanPlayerPickup(false);
        item.setWillAge(false);
        item.setTicksLived(2);
    }

    public boolean isStillComplete() {
        return shrineInfo.isAt(center, direction, offset);
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

    private Long craftingStartTime = null;

    public void animate(long time, double radiansPerFrame, int ticksTillNextFrame) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            ArrayList<Player> players = new ArrayList<>(1);
            items.forEach(item -> reshowIfBurnig(item, players));
        }
        if (!isCrafting) {
            shrineInfo.getIdleAnimation().animate(List.copyOf(items), itemRotationCenter, time, radiansPerFrame, ticksTillNextFrame);
        } else {
            if (craftingStartTime == null) {
                craftingStartTime = time;
            }
            if (shrineInfo.getMergeAnimation().animate(List.copyOf(items), previewItem.getLocation(), time - craftingStartTime, radiansPerFrame, ticksTillNextFrame)) {
                finishedCraftingConsumer.accept(this, shrineInfo.tryCraft(items));
            }
        }
    }

    private void reshowIfBurnig(final @NotNull Item item, final @NotNull Collection<Player> players) {
        if (item.getFireTicks() <= 0 || item.getFireTicks() >= 300 || Math.abs(item.getLocation().getY() - itemRotationCenter.getY()) >= 0.01 || item.isInLava()) {
            return;
        }
        item.setFireTicks(0);
        if (players.isEmpty()) {
            players.addAll(center.getNearbyPlayers(50));
        }
        if (players.isEmpty()) {
            return;
        }
        PacketContainer spawnEntityPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        PacketContainer entityMetadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        spawnEntityPacket.getEntityModifier(item.getWorld()).write(0, item);
        spawnEntityPacket.getUUIDs().write(0, item.getUniqueId());
        spawnEntityPacket.getIntegers().write(0, item.getEntityId());
        spawnEntityPacket.getDoubles()
                .write(0, item.getLocation().getX())
                .write(1, item.getLocation().getY())
                .write(2, item.getLocation().getZ());

        // set velocity
        spawnEntityPacket.getIntegers()
                .write(1, (int) (item.getVelocity().getX() * 8000.0D))
                .write(2, (int) (item.getVelocity().getY() * 8000.0D))
                .write(3, (int) (item.getVelocity().getZ() * 8000.0D));
        // pitch & yaw
//        spawnEntityPacket.getIntegers()
//                .write(4, 0)
//                .write(5, 0);
        spawnEntityPacket.getEntityTypeModifier().write(0, item.getType());

        entityMetadataPacket.getEntityModifier(item.getWorld()).write(0, item);
        entityMetadataPacket.getIntegers().write(0, item.getEntityId());
        entityMetadataPacket.getWatchableCollectionModifier().write(0, WrappedDataWatcher.getEntityWatcher(item).getWatchableObjects());
        players.forEach(player1 -> {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player1, spawnEntityPacket);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player1, entityMetadataPacket);
            } catch (InvocationTargetException e) {
                plugin.getLogger().log(Level.SEVERE, e, () -> String.format("Something went wrong when sending item packets: %s", e.getCause()));
            }
        });
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return center;
    }

    public Set<Location> getAllInputLocations() {
        return (Set<Location>) inputLocations.clone();
    }

    private static List<Item> splitItem(Item item) {
        if (item.getItemStack().getAmount() == 1) return List.of(item);
        final ItemStack itemStack = item.getItemStack();
        final int amount = itemStack.getAmount();
        itemStack.setAmount(1);
        item.setItemStack(itemStack);
        final Location location = item.getLocation();
        final List<Item> result = new ArrayList<>(amount);
        result.add(item);
        for (int i = 1; i < amount; i++) {
            Item newItem = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
            newItem.setItemStack(itemStack);
            newItem.setVelocity(Utils.VECTOR_ZERO);
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
