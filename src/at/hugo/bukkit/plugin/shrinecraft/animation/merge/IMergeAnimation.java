package at.hugo.bukkit.plugin.shrinecraft.animation.merge;

import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.Collection;
import java.util.List;

public interface IMergeAnimation {
    /**
     * @return true when its finished
     */
    boolean animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame);
}
