package at.hugo.bukkit.plugin.shrinecraft;

import at.hugo.bukkit.plugin.shrinecraft.manager.DesignManager;
import at.hugo.bukkit.plugin.shrinecraft.manager.DroppedItemManager;
import at.hugo.bukkit.plugin.shrinecraft.manager.ShrineManager;
import com.advancedkind.plugin.utils.YamlFileConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import at.hugo.bukkit.plugin.shrinecraft.listener.ItemListener;
import co.aikar.commands.PaperCommandManager;

public class ShrineCraftPlugin extends JavaPlugin {
    private NamespacedKey modifiedItemKey;

    private DroppedItemManager droppedItemManager;
    private ShrineManager shrineManager;
    private DesignManager designManager;

    private YamlFileConfig messageConfig;
    private YamlFileConfig designConfig;

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        modifiedItemKey = NamespacedKey.fromString("modifieditem", this);

        // create manager
        droppedItemManager = new DroppedItemManager(this);
        shrineManager = new ShrineManager(this);
        designManager = new DesignManager(this);

        // create configs
        messageConfig = new YamlFileConfig(this, "messages.yml");
        designConfig = new YamlFileConfig(this, "designs.yml");
        reloadConfig();

        commandManager = new PaperCommandManager(this);

        // register command completions
        commandManager.getCommandCompletions().registerAsyncCompletion("designs", c -> designConfig.getKeys(false));
        // register commands
        commandManager.registerCommand(new ShrineCraftCommand(this));

        // register events
        getServer().getPluginManager().registerEvents(new ItemListener(this, droppedItemManager), this);
        getServer().getPluginManager().registerEvents(shrineManager, this);
    }

    @Override
    public void reloadConfig() {
        saveDefaultConfig();
        super.reloadConfig();
        messageConfig.reload();
        designConfig.reload();

        designManager.reload();
        shrineManager.reload();
    }

    public YamlFileConfig getMessagesConfig() {
        return messageConfig;
    }

    public YamlFileConfig getDesignConfig() {
        return designConfig;
    }

    public DesignManager getDesignManager() {
        return designManager;
    }

    public NamespacedKey getModifiedItemKey() {
        return modifiedItemKey;
    }
}
