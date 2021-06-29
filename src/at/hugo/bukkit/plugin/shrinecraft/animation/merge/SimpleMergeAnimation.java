package at.hugo.bukkit.plugin.shrinecraft.animation.merge;

import at.hugo.bukkit.plugin.shrinecraft.Utils;
import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.List;

public class SimpleMergeAnimation implements IMergeAnimation {
    private static final long SHRINK_START_TICK = 100;
    private static final double MAX_ROTATION_SPEED_MULTIPLIER = 4D;

    /**
     * @return true when its finished
     */
    @Override
    public boolean animate(final List<Item> items, final Location center, long time, double radiansPerFrame, final int ticksTillNextFrame) {
        time = time * ticksTillNextFrame;
        final Vector centerVector = center.toVector();
        boolean finished = true;
        if (time < SHRINK_START_TICK) {
            finished = false;
            radiansPerFrame *= (MAX_ROTATION_SPEED_MULTIPLIER - 1) / SHRINK_START_TICK * time + 1;

            for (Item item : items) {
                final Vector currentPos = item.getLocation().toVector().subtract(centerVector);
                final Vector toPos = currentPos.clone().rotateAroundY(radiansPerFrame);
                final Vector path = toPos.subtract(currentPos);
                if(item.getFireTicks() > 0) {
                    item.setVelocity(Utils.VECTOR_ZERO);
                    item.teleport(item.getLocation().clone().add(path));
                } else {
                    final Vector velocity = path.multiply(1D / ticksTillNextFrame);
                    item.setVelocity(velocity);
                }
            }
        } else {
            radiansPerFrame *= MAX_ROTATION_SPEED_MULTIPLIER;
            for (Item item : items) {
                final Vector currentPos = item.getLocation().toVector().subtract(centerVector);

                final double horizontalDistanceSquaredToCenter = currentPos.getX() * currentPos.getX() + currentPos.getZ() * currentPos.getZ();
                final double distanceSquaredToCenter = horizontalDistanceSquaredToCenter + currentPos.getY() * currentPos.getY();

                final Vector path;
                if (distanceSquaredToCenter <= 0.01) {
                    item.setVelocity(Utils.VECTOR_ZERO);
                    continue;
                } else if (horizontalDistanceSquaredToCenter <= 0.25 * 0.25) {
                    finished = false;
                    path = currentPos.multiply(-1);
                } else {
                    finished = false;
                    final Vector toPos = currentPos.clone().rotateAroundY(radiansPerFrame);
                    toPos.subtract(toPos.clone().normalize().multiply(0.25));
                    path = toPos.subtract(currentPos);
                }
                if(item.getFireTicks() > 0) {
                    item.setVelocity(Utils.VECTOR_ZERO);
                    item.teleport(item.getLocation().clone().add(path));
                } else {
                    final Vector velocity = path.multiply(1D / ticksTillNextFrame);
                    item.setVelocity(velocity);
                }

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
