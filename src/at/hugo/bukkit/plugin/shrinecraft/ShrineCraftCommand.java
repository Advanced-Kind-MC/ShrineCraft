package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.command.CommandSender;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;

@CommandAlias("shrinecraft")
@CommandPermission("shrinecraft")
public class ShrineCraftCommand extends BaseCommand {
    private final ShrineCraftPlugin plugin;
    public ShrineCraftCommand(final ShrineCraftPlugin plugin) {
        this.plugin = plugin;
    }
    @Default
    @CatchUnknown
    public void onDefault(CommandSender sender) {
        sender.sendMessage("Poopy");
    }

    
    @Subcommand("reload")
    @CommandPermission("shrinecraft.command.reload")
    @Description("Reloads the config file")
    public void onReload(CommandSender sender) {
        sender.sendMessage("Reloading");
        plugin.reload();
        sender.sendMessage("Reloaded");
    }

}
