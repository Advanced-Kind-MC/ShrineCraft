package at.hugo.bukkit.plugin.shrinecraft.manager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import at.hugo.bukkit.plugin.shrinecraft.Shrine;
import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.ShrineInfo;
import com.advancedkind.plugin.utils.utils.ConfigUtils;
import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import at.hugo.bukkit.plugin.shrinecraft.event.ItemLandEvent;
import org.jetbrains.annotations.NotNull;

public class ShrineManager implements Listener {
    private static final Sound PICKUP_SOUND = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "entity.item.pickup"), Sound.Source.PLAYER, .25f, 1);

    private final ShrineCraftPlugin plugin;

    private BukkitTask itemAnimationTask = null;
    private BukkitTask particleAnimationTask = null;

    private final ConcurrentHashMap<Location, Shrine> activeShrines = new ConcurrentHashMap<>();
    private final Multimap<Material, ShrineInfo> shrineMap = MultimapBuilder.enumKeys(Material.class).linkedListValues().build();

    public ShrineManager(final ShrineCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        for (Shrine shrine : activeShrines.values()) {
            if (shrine.onItemMerge(event)) {
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        Iterator<Shrine> iterator = activeShrines.values().iterator();
        while (iterator.hasNext()) {
            final Shrine shrine = iterator.next();
            // check if player owns the shrine
            if (!shrine.getPlayer().equals(player)) continue;

            final Location loc = shrine.getLocation().clone();
            loc.setY(event.getTo().getY());
            // check if hes further than max distance away from the shrine
            if (event.getTo().distanceSquared(loc) > maxDistanceFromShrineSquared) {
                iterator.remove();
                returnItemsToPlayer(shrine.removeItems(), player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        Iterator<Shrine> iterator = activeShrines.values().iterator();
        while (iterator.hasNext()) {
            Shrine shrine = iterator.next();
            if (shrine.getPlayer().equals(player)) {
                iterator.remove();
                returnItemsToPlayer(shrine.removeItems(), player);
            }
        }
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
        final Location location = landedOnBlock.getLocation().clone().add(0.5, 0.5, 0.5);

        // check if the landed on block is even valid for shrines
        if (!shrineMap.containsKey(landedOnBlock.getType()))
            return;

        boolean wasShrine = false;
        // is already a shrine here?
        if (activeShrines.containsKey(location)) {
            final Shrine shrine = activeShrines.get(location);
            wasShrine = true;

            // check if the owner if the shrine is the player who threw it
            if (!shrine.getPlayer().equals(player)) {
                returnItemsToPlayer(items, player);
                // end of this path
                return;
            }
            // can the item be added to the shrine and is the shrine valid?
            if (shrine.canAddItem(item) && shrine.isAt(landedOnBlock)) {
                // this shrine is valid and the item can be added
                shrine.addAllItems(items);
                // see if you can now craft an item here
                tryCraftItem(activeShrines.get(location));
                // end of this path
                return;
            } else {
                // disband this shrine
                activeShrines.remove(location);
                items.addAll(shrine.removeItems());
                // continue with new shrine creation
            }
        }

        // new shrine creation
        // go through all possible shrines
        for (ShrineInfo shrineInfo : shrineMap.get(landedOnBlock.getType())) {
            // check if the shrine has a similar recipe and is at this location
            if (shrineInfo.hasSimilarRecipe(items) && shrineInfo.isAt(landedOnBlock)) {
                // create the shrine
                final Shrine shrine = new Shrine(plugin, player, shrineInfo, location, this::removeShrine);
                shrine.addAllItems(items);
                activeShrines.put(location, shrine);
                // try to craft an item
                tryCraftItem(activeShrines.get(location));
                return;
            }
        }

        if (wasShrine) {
            returnItemsToPlayer(items, player);
        }
    }

    private void removeShrine(final @NotNull Shrine shrine) {
        activeShrines.remove(shrine.getLocation());
        returnItemsToPlayer(shrine.removeItems(), shrine.getPlayer());
    }

    private void returnItemsToPlayer(List<Item> items, Player player) {
        boolean pickedUpItem = false;
        // give items into player inventory
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (player.getInventory().addItem(item.getItemStack()).isEmpty()) {
                item.remove();
                iterator.remove();
                pickedUpItem = true;
            }
        }
        if (pickedUpItem) {
            player.playSound(PICKUP_SOUND);
        }
    }

    private void tryCraftItem(final Shrine shrine) {
        if (shrine.canCraftItem()) {
            shrine.startCrafting(this::finishedCrafting);
        }
    }

    private void finishedCrafting(final Shrine shrine, final ItemStack craftedItem) {
        activeShrines.remove(shrine.getLocation());
        shrine.removeItems().forEach(Item::remove);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Item item = createItemEntity(shrine.getItemCenter(), craftedItem);
            item.setPickupDelay(5);
        });
    }

    private Item createItemEntity(Location location, ItemStack itemStack) {
        Item item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(itemStack);
        item.setGravity(false);
        item.setVelocity(new Vector());
        return item;
    }


    public void reload() {
        this.ticksBetweenRefreshes = plugin.getConfig().getInt("animation.ticks-between-refreshes");
        double degreesPerSecond = plugin.getConfig().getDouble("animation.degrees-per-second");

        this.radiansPerFrame = (degreesPerSecond / (20D / (this.ticksBetweenRefreshes + 1))) * (Math.PI / 180);

        final double particleCircumference = 2D * Math.PI * maxDistanceFromShrine;
        final double particleDistance = 6;

        final int particleCount = (int) Math.round(particleCircumference / particleDistance);
        final Vector particleVector = new Vector(1, 0, 0).multiply(maxDistanceFromShrine);
        final double particleRotation = (particleCircumference / particleCount) / maxDistanceFromShrine;
        this.particleRotation = 1D / maxDistanceFromShrine;


        particleVectors = new Vector[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particleVectors[i] = particleVector.clone();
            particleVector.rotateAroundY(particleRotation);
        }

        shrineMap.clear();
        for (Object shrineObject : plugin.getConfig().getList("shrines")) {
            ShrineInfo shrineInfo = new ShrineInfo(plugin, ConfigUtils.objectToConfigurationSection(shrineObject));
            shrineMap.put(shrineInfo.getCraftingBlockMaterial(), shrineInfo);
        }

        if (itemAnimationTask != null)
            itemAnimationTask.cancel();
        itemAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateItems, 0, this.ticksBetweenRefreshes);
        if (particleAnimationTask != null)
            particleAnimationTask.cancel();
        particleAnimationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateParticles, 0, 4);

    }

    private int ticksBetweenRefreshes = 10;
    private double radiansPerFrame = 30D / 20 * ticksBetweenRefreshes;

    private long time = 0;

    private final int maxDistanceFromShrine = 15;
    private final int maxDistanceFromShrineSquared = maxDistanceFromShrine * maxDistanceFromShrine;
    private Vector[] particleVectors = new Vector[0];
    private double particleRotation = Math.PI / 16;

    private final static ParticleBuilder PARTICLE_BUILDER = new ParticleBuilder(Particle.REDSTONE).offset(0, 0, 0).force(true).color(Color.GREEN, 2).count(1);


    private void animateParticles() {
        for (Shrine shrine : activeShrines.values()) {
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
        for (Shrine shrine : activeShrines.values()) {
            shrine.animate(time, radiansPerFrame, ticksBetweenRefreshes + 1);
        }
        ++time;
    }

}
