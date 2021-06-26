package at.hugo.bukkit.plugin.shrinecraft;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockVector;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class WorldEditUtils {
    private static final EnumSet<BlockFace> NEIGHBORING_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP);

    private WorldEditUtils() {}
    public static void createDesignFromRegion(Player player, String designName) {
        final ShrineCraftPlugin plugin = JavaPlugin.getPlugin(ShrineCraftPlugin.class);

        final BukkitPlayer worldEditPlayer = BukkitAdapter.adapt(player);
        final LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
        final World selectionWorld = localSession.getSelectionWorld();
        final Region region;
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException e) {
            player.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.no-selection"));
            return;
        }
        final CuboidRegion cuboidRegion = region.getBoundingBox();
        org.bukkit.World world = BukkitAdapter.adapt(selectionWorld);
        Location minLoc = BukkitAdapter.adapt(world, cuboidRegion.getMinimumPoint());
        Location maxLoc = BukkitAdapter.adapt(world, cuboidRegion.getMaximumPoint());

        if (!cuboidRegion.contains(BlockVector3.at(player.getLocation().getX(),
                player.isInLava() || player.isInLava() ? player.getLocation().getY() : player.getLocation().getY() - 1,
                player.getLocation().getZ()))) {
            player.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.not-inside-region"));
        }

        List<List<List<String>>> design = new LinkedList<>();
        for (int y = maxLoc.getBlockY(); y >= minLoc.getBlockY(); y--) {
            List<List<String>> zList = new LinkedList<>();
            for (int z = minLoc.getBlockZ(); z <= maxLoc.getBlockZ(); z++) {
                List<String> xList = new LinkedList<>();
                for (int x = minLoc.getBlockX(); x <= maxLoc.getBlockX(); x++) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (material == Material.AIR) {
                        xList.add("");
                    } else if (material == Material.STRING) {
                        xList.add("AIR");
                    } else {
                        xList.add(material.toString());
                    }

                }
                zList.add(xList);
            }
            design.add(zList);
        }
        final Block startBlock;
        if (player.isInLava() || player.isInLava()) {
            startBlock = player.getLocation().getBlock();
        } else {
            startBlock = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        }
        final Material startMaterial = startBlock.getType();
        final HashSet<Block> knownBlocks = new HashSet<>();
        final LinkedList<Block> queuedBlocks = new LinkedList<>();
        final LinkedList<Block> foundBlocks = new LinkedList<>();
        queuedBlocks.add(startBlock);
        knownBlocks.add(startBlock);
        while (!queuedBlocks.isEmpty()) {
            Block block = queuedBlocks.poll();
            foundBlocks.add(block);
            for (BlockFace face : NEIGHBORING_FACES) {
                Block neighbor = block.getRelative(face);
                if (!knownBlocks.contains(neighbor) && startMaterial.equals(neighbor.getType()) && cuboidRegion.contains(BlockVector3.at(neighbor.getX(), neighbor.getY(), neighbor.getZ()))) {
                    queuedBlocks.add(neighbor);
                }
                knownBlocks.add(neighbor);
            }
        }
        BlockVector[] inputOffsets = foundBlocks.stream().map(block -> new BlockVector(block.getX() - minLoc.getBlockX(), block.getY() - minLoc.getBlockY(), block.getZ() - minLoc.getBlockZ())).toArray(BlockVector[]::new);

        plugin.getDesignManager().setDesign(designName, Utils.convertStringListToMaterialArray(design), inputOffsets);
        player.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.success").replaceText(TextReplacementConfig.builder().match("%name%").replacement(designName).build()));
    }
}
