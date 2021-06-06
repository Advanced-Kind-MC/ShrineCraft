package at.hugo.bukkit.plugin.shrinecraft.animation.idle;

import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.List;

public interface IIdleAnimation {
    void animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame);
}
