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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            sender.sendMessage(plugin.getMessagesConfig().getComponent("commands.setdesign.worldedit-not-enabled"));
            return;
        }
        WorldEditUtils.createDesignFromRegion(sender, name);
    }
}
