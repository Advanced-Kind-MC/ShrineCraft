package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.plugin.java.JavaPlugin;

import at.hugo.bukkit.plugin.shrinecraft.listener.ItemListener;
import co.aikar.commands.PaperCommandManager;

public class ShrineCraftPlugin extends JavaPlugin {
    DroppedItemManager droppedItemManager;
    ShrineManager shrineManager;
    
    PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        commandManager = new PaperCommandManager(this);
        droppedItemManager = new DroppedItemManager(this);
        shrineManager = new ShrineManager(this, getConfig().getDouble("animation.refreshes-per-second"),
                getConfig().getDouble("animation.degrees-per-second"));
        getServer().getPluginManager().registerEvents(new ItemListener(droppedItemManager), this);
        getServer().getPluginManager().registerEvents(shrineManager, this);
        commandManager.registerCommand(new ShrineCraftCommand(this));
    }

    public void reload() {
        reloadConfig();
        shrineManager.update(getConfig().getDouble("animation.refreshes-per-second"), getConfig().getDouble("animation.degrees-per-second"));
    }
}
