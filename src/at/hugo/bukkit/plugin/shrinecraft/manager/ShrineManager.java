package at.hugo.bukkit.plugin.shrinecraft.manager;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.hugo.bukkit.plugin.shrinecraft.Shrine;
import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.ShrineInfo;
import at.hugo.bukkit.plugin.shrinecraft.Utils;
import com.advancedkind.plugin.utils.utils.ConfigUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;
import org.jetbrains.annotations.NotNull;

public class ShrineManager implements Listener {
    private static final Sound PICKUP_SOUND = Sound.sound(org.bukkit.Sound.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, .25f, 1);

    private final ShrineCraftPlugin plugin;

    private BukkitTask itemAnimationTask = null;
    private BukkitTask particleAnimationTask = null;

    private final ConcurrentHashMap<Location, Shrine> shrineLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Item, Player> playerItems = new ConcurrentHashMap<>();
    private final Set<Shrine> activeShrines = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Multimap<Material, ShrineInfo> shrineMap = MultimapBuilder.enumKeys(Material.class).linkedListValues().build();


    // item animation variables
    private int ticksBetweenRefreshes = 10;
    private double radiansPerFrame = 30D / 20 * ticksBetweenRefreshes;
    /**
     * current item tick
     */
    private long time = 0;

    /**
     * maximum distance someone can be from the shrine
     */
    private double maxDistanceFromShrine = 15;
    private double maxDistanceFromShrineSquared = maxDistanceFromShrine * maxDistanceFromShrine;
    /**
     * the Vectors of for the particle shrine border animation
     */
    private Vector[] particleVectors = new Vector[0];
    /**
     * rotation speed for the particle shrine border animation
     */
    private double particleRotation = Math.PI / 16;

    private final static ParticleBuilder PARTICLE_BUILDER = new ParticleBuilder(Particle.REDSTONE).offset(0, 0, 0).force(true).color(Color.GREEN, 2).count(1);


    public ShrineManager(final ShrineCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * loads the data from the config files
     */
    public void reload() {
        // max distance from the shrine a player can move
        this.maxDistanceFromShrine = plugin.getConfig().getDouble("shrine-max-distance");
        this.maxDistanceFromShrineSquared = maxDistanceFromShrine * maxDistanceFromShrine;

        // region: item animation
        // item animation refresh rate
        this.ticksBetweenRefreshes = plugin.getConfig().getInt("animation.ticks-between-refreshes");
        // item animation rotation in degrees per second
        final double degreesPerSecond = plugin.getConfig().getDouble("animation.degrees-per-second");

        // item animation rotation in radians per frame
        this.radiansPerFrame = (degreesPerSecond / (20D / (this.ticksBetweenRefreshes + 1))) * (Math.PI / 180);
        // endregion: item animation

        // region: particle shrine border

        // ticks between particle ticking ticks
        final long particleWaitTicks = plugin.getConfig().getLong("particle-border.ticks-between-refreshes");

        PARTICLE_BUILDER.color(Color.fromRGB(
                plugin.getConfig().getInt("particle-border.particle-color.red"),
                plugin.getConfig().getInt("particle-border.particle-color.green"),
                plugin.getConfig().getInt("particle-border.particle-color.blue")),
                (float) plugin.getConfig().getDouble("particle-border.particle-size")
        );

        // circumference
        final double particleCircumference = 2D * Math.PI * maxDistanceFromShrine;
        // distance between particles
        final double particleDistance = plugin.getConfig().getDouble("particle-border.particle-distance");

        // particle count
        final int particleCount = (int) Math.round(particleCircumference / particleDistance);
        // distance vector
        final Vector particleVector = new Vector(1, 0, 0).multiply(maxDistanceFromShrine);
        // radians between each particle
        final double particleRotation = (particleCircumference / particleCount) / maxDistanceFromShrine;
        // rotation speed
        this.particleRotation = plugin.getConfig().getDouble("particle-border.particle-rotation-distance") / maxDistanceFromShrine;

        // prepare position vectors
        particleVectors = new Vector[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particleVectors[i] = particleVector.clone();
            particleVector.rotateAroundY(particleRotation);
        }

        // endregion: particle shrine border

        // region: shrine info loading
        shrineMap.clear();
        for (Object shrineObject : plugin.getConfig().getList("shrines")) {
            ShrineInfo shrineInfo = new ShrineInfo(plugin, ConfigUtils.objectToConfigurationSection(shrineObject));
            shrineInfo.getCraftingBlockMaterials().forEach(material -> shrineMap.put(material, shrineInfo));
        }
        // reloading shrines
        activeShrines.forEach(Shrine::reload);
        // endregion: shrine info loading

        // (re)starting tasks
        if (itemAnimationTask != null)
            itemAnimationTask.cancel();
        itemAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateItems, 0, this.ticksBetweenRefreshes);
        if (particleAnimationTask != null)
            particleAnimationTask.cancel();
        particleAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateParticles, 0, particleWaitTicks);
    }

    private final @NotNull HashMap<Player, BukkitTask> droppedItemPlayers = new HashMap<>();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupOthersItem(PlayerAttemptPickupItemEvent event) {
        if (playerItems.containsKey(event.getItem()) && !event.getPlayer().equals(playerItems.get(event.getItem()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        BukkitTask task = droppedItemPlayers.put(event.getPlayer(), Bukkit.getScheduler().runTaskLater(plugin, () -> droppedItemPlayers.remove(event.getPlayer()), 5));
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractShrine(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (event.getAction().equals(Action.LEFT_CLICK_AIR) && droppedItemPlayers.containsKey(player)) {
            return;
        }

        List<Block> blocks = null;
        for (Shrine shrine : activeShrines) {
            if (shrine.getPlayer().equals(player)) {
                if (blocks == null) {
                    blocks = player.getLineOfSight(null, 5);
                }
                if (shrine.onTriggerBlock(blocks)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (playerItems.containsKey(event.getEntity()) || playerItems.containsKey(event.getTarget())) {
            event.setCancelled(true);
            return;
        }
        for (Shrine shrine : activeShrines) {
            if (shrine.containsItem(event.getEntity()) || shrine.containsItem(event.getTarget())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHopperPickupItem(InventoryPickupItemEvent event) {
        if (playerItems.containsKey(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        if (doesShrinesIncludeItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Item)) {
            return;
        }
        if (doesShrinesIncludeItem((Item) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            removeShrines(player);
            return;
        }
        getShrines(player).forEach(shrine -> {
            final Location loc = shrine.getLocation().clone();
            loc.setY(event.getTo().getY());
            // check if hes further than max distance away from the shrine
            if (event.getTo().distanceSquared(loc) > maxDistanceFromShrineSquared) {
                removeShrine(shrine);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        var items = getShrines(player).map(this::removeShrineAndGetItems).flatMap(List::stream).collect(Collectors.toList());
        items.forEach(Entity::remove);
        event.getDrops().addAll(items.stream().map(Item::getItemStack).collect(Collectors.toList()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        removeShrines(player);
        removePlayerItems(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemLand(ItemLandEvent event) {
        final Player player = plugin.getServer().getPlayer(event.getDropper());
        // if the player isn't online anymore ignore it
        if (player == null)
            return;

        final Item item = event.getItem();

        final LinkedList<Item> items = new LinkedList<>();
        items.add(item);

        final Block landedOnBlock = event.getLandedOBlock();
        final Location location = landedOnBlock.getLocation();

        // check if the landed on block is even valid for shrines
        if (!shrineMap.containsKey(landedOnBlock.getType()))
            return;

        boolean wasShrine = false;
        // is already a shrine here?
        if (shrineLocations.containsKey(location)) {
            final Shrine shrine = shrineLocations.get(location);
            wasShrine = true;

            // check if the shrine is crafting or
            // check if the owner if the shrine is the player who threw it
            if (shrine.isCrafting() || !shrine.getPlayer().equals(player)) {
                returnItemsToPlayer(items, player);
                // end of this path
                return;
            }
            // can the item be added to the shrine and is the shrine valid?
            if (shrine.canAddItem(item) && shrine.isStillComplete()) {
                // this shrine is valid and the item can be added
                shrine.addAllItems(items);
                // end of this path
                return;
            } else {
                // disband this shrine
                activeShrines.remove(shrine);
                shrine.getAllInputLocations().forEach(shrineLocations::remove);
                items.addAll(shrine.disbandShrine());
                // continue with new shrine creation
            }
        }

        // new shrine creation
        // go through all possible shrines
        for (ShrineInfo shrineInfo : shrineMap.get(landedOnBlock.getType())) {
            // check if the shrine has a similar recipe and is at this location
            if (shrineInfo.hasSimilarRecipe(items)) {
                ShrineInfo.ShrinePosition shrinePosition = shrineInfo.getShrinePositionAt(landedOnBlock);
                if (shrinePosition != null) {
                    // create the shrine
                    final Shrine shrine = new Shrine(plugin, player, shrinePosition, this::removeShrine, this::finishedCrafting);
                    shrine.addAllItems(items);
                    activeShrines.add(shrine);
                    shrine.getAllInputLocations().forEach(inputLocation -> shrineLocations.put(inputLocation, shrine));
                    return;
                }
            } else if (!wasShrine && shrineInfo.wouldAcceptAnyOf(items)) {
                ShrineInfo.ShrinePosition shrinePosition = shrineInfo.getShrinePositionAt(landedOnBlock);
                if (shrinePosition != null) {
                    wasShrine = true;
                }
            }
        }

        if (wasShrine) {
            returnItemsToPlayer(items, player);
        }
    }

    private boolean doesShrinesIncludeItem(Item item) {
        for (Shrine shrine : activeShrines) {
            if (shrine.containsItem(item)) {
                return true;
            }
        }
        return false;
    }

    private void removeShrines(Player player) {
        getShrines(player).forEach(this::removeShrine);
    }

    @NotNull
    private Stream<Shrine> getShrines(Player player) {
        return activeShrines.stream().filter(shrine -> shrine.getPlayer().equals(player));
    }

    @NotNull
    private Stream<Item> getPlayerItems(Player player) {
        return playerItems.entrySet().stream().filter(entry -> entry.getValue().equals(player)).map(Map.Entry::getKey);
    }

    private void removePlayerItems(Player player) {
        getPlayerItems(player).forEach(playerItems::remove);
    }

    private void removeShrine(final @NotNull Shrine shrine) {
        returnItemsToPlayer(removeShrineAndGetItems(shrine), shrine.getPlayer());
    }

    private List<Item> removeShrineAndGetItems(final @NotNull Shrine shrine) {
        activeShrines.remove(shrine);
        shrine.getAllInputLocations().forEach(shrineLocations::remove);
        return shrine.disbandShrine();
    }

    private void returnItemsToPlayer(List<Item> items, Player player) {
        boolean pickedUpItem = false;
        // give items into player inventory
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (player.getInventory().addItem(item.getItemStack()).isEmpty()) {
                item.getItemStack().setAmount(0);
                item.remove();
                iterator.remove();
                pickedUpItem = true;
            }
        }
        if (pickedUpItem) {
            player.playSound(PICKUP_SOUND);
        }
        items.forEach(item -> playerItems.put(item, player));
    }

    private void finishedCrafting(final Shrine shrine, final ItemStack craftedItem) {
        activeShrines.remove(shrine);
        shrine.getAllInputLocations().forEach(shrineLocations::remove);
        shrine.disbandShrine().forEach(Item::remove);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Item item = createItemEntity(shrine.getItemOutputLocation(), craftedItem);
            item.setPickupDelay(5);
            playerItems.put(item, shrine.getPlayer());
        });
    }

    private Item createItemEntity(Location location, ItemStack itemStack) {
        Item item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(itemStack);
        item.setGravity(false);
        item.setVelocity(Utils.VECTOR_ZERO);
        return item;
    }

    private void animateParticles() {
        for (Shrine shrine : activeShrines) {
            final Location particleCenter = shrine.getLocation().clone().add(0, 1.5, 0);
            PARTICLE_BUILDER.receivers(shrine.getPlayer());
            for (Vector particleVector : particleVectors) {
                PARTICLE_BUILDER.location(particleCenter.clone().add(particleVector)).spawn();
            }
        }

        for (Vector particleVector : particleVectors) {
            particleVector.rotateAroundY(particleRotation);
        }
    }

    private void animateItems() {
        Iterator<Map.Entry<Item, Player>> iterator = playerItems.entrySet().iterator();
        final double maxSpeed = 0.2 * (ticksBetweenRefreshes + 1) * 0.2 * (ticksBetweenRefreshes + 1);
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Item item = entry.getKey();
            Player player = entry.getValue();

            if (!item.isValid() || !player.isOnline()) {
                iterator.remove();
            }
            if (item.hasGravity()) {
                item.setGravity(false);
            }
            if (!item.getWorld().equals(player.getWorld())) {
                item.teleportAsync(player.getLocation());
            } else {
                Vector path = player.getLocation().toVector().add(new Vector(0, 1, 0)).subtract(item.getLocation().toVector());
                if (path.lengthSquared() > maxSpeed) {
                    path.normalize().multiply(maxSpeed);
                }
                path.multiply(1D / (ticksBetweenRefreshes + 1));
                item.setVelocity(path);
            }

        }
        for (Shrine shrine : activeShrines) {
            shrine.animate(time, radiansPerFrame, ticksBetweenRefreshes + 1);
        }
        ++time;
    }

}
