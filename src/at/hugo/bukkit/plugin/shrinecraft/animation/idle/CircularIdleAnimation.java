package at.hugo.bukkit.plugin.shrinecraft.animation.idle;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CircularIdleAnimation implements IIdleAnimation {

    private static final Vector[] START_VECTORS = new Vector[]{new Vector(1, 0, 0), new Vector(-1, 0, 0)};

    private static int[] getOrbitDistribution(int itemCount) {

        LinkedList<Integer> result = new LinkedList<>();
        result.add(itemCount % 2);
        itemCount -= result.getLast();

        while (itemCount > 0) {
            final double particleCircumference = 2D * Math.PI * result.size();
            int itemsInOrbit = (int) Math.round(particleCircumference / 1);
            if (itemsInOrbit % 2 == 1) {
                itemsInOrbit -= 1;
            }
            if (itemCount <= itemsInOrbit) {
                result.add(itemCount);
                break;
            } else if (itemCount <= itemsInOrbit * 2) {
                result.add(itemCount);
                break;
            } else {
                itemCount -= itemsInOrbit;
                result.add(itemsInOrbit);
            }
        }
        return result.stream().mapToInt(value -> value).toArray();
    }


    @Override
    public void animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame) {
        final double maxHeightChange = ticksTillNextFrame * 0.1;
        final int totalItems = items.size();

        final int[] distribution = getOrbitDistribution(totalItems);
        final ListIterator<Item> iterator = items.listIterator(totalItems);

        for (int radius = 0; radius < distribution.length; radius++) {
            int itemsInThisOrbit = distribution[radius];
            if (itemsInThisOrbit <= 0) continue;

            // reset starting point
            final Vector expectedPosition = START_VECTORS[radius % 2].clone().multiply(radius)
                    .rotateAroundY(radiansPerFrame * time);

            // set the max speed
            final double maxSpeed;
            if (radius == 0) {
                maxSpeed = 0.1;
            } else {
                // double the arc length
                maxSpeed = radius * radiansPerFrame;
            }
            final double maxSpeedSquared = maxSpeed * maxSpeed;

            final double radiaBetweenItems = (2 * Math.PI) / itemsInThisOrbit;

            for (int itemsToGo = 0; itemsToGo < itemsInThisOrbit; ++itemsToGo) {
                final Item item = iterator.previous();

                final Vector currentPosition = item.getLocation().toVector().subtract(center.toVector());
                final Vector toPos = expectedPosition.clone();

                if (Math.abs(toPos.getY()) > maxHeightChange) {
                    toPos.setY(Math.copySign(maxHeightChange, toPos.getY()));
                }

                final Vector path = toPos.subtract(currentPosition);
                final Vector horizontalSpeedVector = path.clone();
                horizontalSpeedVector.setY(0);
                final double horizontalSpeed = horizontalSpeedVector.lengthSquared();
                if (horizontalSpeed > maxSpeedSquared) {
                    final Vector maxSpeedVector = horizontalSpeedVector.normalize().multiply(maxSpeed);
                    path.setX(maxSpeedVector.getX());
                    path.setZ(maxSpeedVector.getZ());
                }
                final Vector velocity = path.multiply(1D / ticksTillNextFrame);
                item.setVelocity(velocity);

                // rotate for next item
                if (itemsToGo % 2 == 0) {
                    expectedPosition.rotateAroundY(Math.PI);
                } else {
                    expectedPosition.rotateAroundY(radiaBetweenItems + Math.PI);
                }
//                ++finishedItems;
            }
        }
    }
}
