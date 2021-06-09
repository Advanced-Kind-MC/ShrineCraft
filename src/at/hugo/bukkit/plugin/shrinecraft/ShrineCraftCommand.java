package at.hugo.bukkit.plugin.shrinecraft;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

@CommandAlias("shrinecraft")
@CommandPermission("shrinecraft.command")
public class ShrineCraftCommand extends BaseCommand {
    private final ShrineCraftPlugin plugin;

    public ShrineCraftCommand(final ShrineCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("shrinecraft.command.reload")
    @Description("Reloads the config file")
    public void onReload(CommandSender sender) {
        sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.reload.start"));
        plugin.reloadConfig();
        sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.reload.finish"));
    }

    @Subcommand("setDesign")
    @CommandPermission("shrinecraft.command.setdesign")
    @CommandCompletion("@designs")
    public void onCreateRegion(final Player sender, final @Single String name) {
        final BukkitPlayer worldEditPlayer = BukkitAdapter.adapt(sender);
        final LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
        final World selectionWorld = localSession.getSelectionWorld();
        final Region region;
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException e) {
            sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.no-selection"));
            return;
        }
        final CuboidRegion cuboidRegion = region.getBoundingBox();
        org.bukkit.World world = BukkitAdapter.adapt(selectionWorld);
        Location minLoc = BukkitAdapter.adapt(world, cuboidRegion.getMinimumPoint());
        Location maxLoc = BukkitAdapter.adapt(world, cuboidRegion.getMaximumPoint());

        if (!cuboidRegion.contains(BlockVector3.at(sender.getLocation().getX(),
                sender.isInLava() || sender.isInLava() ? sender.getLocation().getY() : sender.getLocation().getY() - 1,
                sender.getLocation().getZ()))) {
            sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.not-inside-region"));
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
        if (sender.isInLava() || sender.isInLava()) {
            plugin.getDesignManager().setDesign(name, Utils.convertStringListToMaterialArray(design),
                    sender.getLocation().getBlockX() - minLoc.getBlockX(),
                    sender.getLocation().getBlockY() - minLoc.getBlockY(),
                    sender.getLocation().getBlockZ() - minLoc.getBlockZ());
        } else {
            plugin.getDesignManager().setDesign(name, Utils.convertStringListToMaterialArray(design),
                    sender.getLocation().getBlockX() - minLoc.getBlockX(),
                    sender.getLocation().getBlockY() - minLoc.getBlockY() - 1,
                    sender.getLocation().getBlockZ() - minLoc.getBlockZ());
        }
        sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.success").replaceText(TextReplacementConfig.builder().match("%name%").replacement(name).build()));
    }
}
