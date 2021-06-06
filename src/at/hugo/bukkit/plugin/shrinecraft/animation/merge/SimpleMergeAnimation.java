package at.hugo.bukkit.plugin.shrinecraft.animation.merge;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public class SimpleMergeAnimation implements IMergeAnimation {

    /**
     * @return true when its finished
     */
    @Override
    public boolean animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame) {
        boolean finished = true;
        for (Item item : items) {
            final Vector distance = center.toVector().subtract(item.getLocation().toVector());
            final double distanceSquared = distance.lengthSquared();
            if(distanceSquared > 1) {
                distance.normalize();
            }
            distance.multiply(1D / ticksTillNextFrame);
            item.setVelocity(distance);
            if (distanceSquared >= 0.1) {
                finished = false;
            }
        }
        return finished;
    }
}
