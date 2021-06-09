package at.hugo.bukkit.plugin.shrinecraft.animation.merge;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.List;

public class SimpleMergeAnimation implements IMergeAnimation {

    /**
     * @return true when its finished
     */
    @Override
    public boolean animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame) {
        final Vector centerVector = center.toVector();
        boolean finished = true;
        for (Item item : items) {
            final Vector currentPos = item.getLocation().toVector().subtract(centerVector);
            final double distanceSquaredToCenter = currentPos.lengthSquared();
            final Vector toPos;
            final double nextInwardDistance = 0.05 * ticksTillNextFrame;
            if (distanceSquaredToCenter >= nextInwardDistance * nextInwardDistance) {
                // Function used to speed up the rotation the further in it gets
                // f(x) = 140 / (x * x + 10) + 1
                toPos = currentPos.clone().rotateAroundY((20 / (distanceSquaredToCenter + 10) + 1) * radiansPerFrame);
                toPos.subtract(toPos.clone().normalize().multiply(nextInwardDistance));
            } else {
                toPos = new Vector(0, 0, 0);
            }

            final Vector path = toPos.subtract(currentPos).multiply(1D / ticksTillNextFrame);
            item.setVelocity(path);
            if (distanceSquaredToCenter >= 0.1) {
                finished = false;
            }
        }
        if (finished) {
            ParticleBuilder particleBuilder = new ParticleBuilder(Particle.EXPLOSION_HUGE).location(center).receivers(25).force(true).offset(0, 0, 0).spawn();
            Sound sound = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "entity.generic.explode"), Sound.Source.PLAYER, 1, 1);
            final double x = center.getX();
            final double y = center.getY();
            final double z = center.getZ();
            particleBuilder.receivers().forEach(player -> player.playSound(sound, x, y, z));
        }
        return finished;
    }
}
