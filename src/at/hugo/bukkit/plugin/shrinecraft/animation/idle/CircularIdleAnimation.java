package at.hugo.bukkit.plugin.shrinecraft.animation.idle;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CircularIdleAnimation implements IIdleAnimation {

    private static final Vector[] START_VECTORS = new Vector[]{new Vector(1, 0, 0), new Vector(-1, 0, 0)};
//    private static final TreeMap<Integer, Integer> radia = new TreeMap<>();
//
//    static {
//        radia.put(-1, 0);
//        for (int i = 0; i < 50; i++) {
//            int radius = i * 2;
//            int value = (int) Math.floor(radius * Math.PI * 4);
//            if (i != 0)
//                value += radia.lastKey();
//            radia.put(value, radius);
//        }
//    }

    private static int[] getOrbitDistribution(int itemCount) {
        LinkedList<Integer> result = new LinkedList<>();
        result.add(itemCount % 2);
        itemCount -= result.getLast();

        int maxItems = 4;

        while (itemCount > 0) {
            int itemsInOrbit = itemCount >= maxItems ? maxItems : itemCount;
            itemCount -= itemsInOrbit;
            maxItems *= 2;
            if (itemCount < maxItems / 4) {
                itemsInOrbit += itemCount;
                itemCount = 0;
            }
            result.add(itemsInOrbit);
        }
        return result.stream().mapToInt(value -> value).toArray();
    }


    @Override
    public void animate(List<Item> items, Location center, long time, double radiansPerFrame, int ticksTillNextFrame) {
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

//            if (radius > 0) {
//                expectedPosition.rotateAroundY(radiaBetweenItems * radius);
//            }

            for (int itemsToGo = 0; itemsToGo < itemsInThisOrbit; ++itemsToGo) {
                final Item item = iterator.previous();

                final Vector currentPosition = item.getLocation().clone().toVector().subtract(center.toVector());
                final Vector path = expectedPosition.clone().subtract(currentPosition).multiply(1D / ticksTillNextFrame);
                double speed = path.lengthSquared();
                if (speed > maxSpeedSquared)
                    path.normalize().multiply(maxSpeed);
                item.setVelocity(path);

                // roate for next item
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
