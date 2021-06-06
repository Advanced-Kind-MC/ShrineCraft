package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

public class Shrine {
    private final @NotNull ShrineInfo shrineInfo;
    private final @NotNull Location center;

    private final @NotNull Location itemCenter;

    private final @NotNull ArrayList<Item> items = new ArrayList<>();

    private boolean crafting = false;
    private BiConsumer<Shrine, ItemStack> finishedCraftingConsumer;

    public Shrine(final @NotNull ShrineInfo shrineInfo, @NotNull Location center) {
        this.shrineInfo = shrineInfo;
        this.center = center;
        this.itemCenter = center.clone().add(0, 2, 0);
    }

    public boolean canCraftItem() {
        return shrineInfo.tryCraft(items) != null;
    }

    public void startCrafting(@NotNull BiConsumer<Shrine, ItemStack> finishedCrafting) {
        crafting = true;
        finishedCraftingConsumer = finishedCrafting;
    }

    public boolean canAddItem(Item item) {
        LinkedList<Item> items = new LinkedList<>();
        items.addAll(this.items);
        items.add(item);
        return shrineInfo.hasSimilarRecipe(items);
    }

    public boolean addItem(Item item) {
        items.add(item);
        item.setGravity(false);
        return true;
    }

    public boolean isAt(Block block) {
        return shrineInfo.isAt(block);
    }

    @NotNull
    public List<Item> discardItems() {
        List<Item> result = List.copyOf(items);
        items.clear();
        result.forEach(item -> item.setGravity(true));
        return result;
    }

    public void addAllItems(List<Item> items) {
        items.forEach(item -> item.setGravity(false));
        this.items.addAll(items);
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public ShrineInfo getShrineInfo() {
        return shrineInfo;
    }

    public void animate(long time, double radiansPerFrame, int ticksTillNextFrame) {
        if (!crafting) {
            shrineInfo.getIdleAnimation().animate(items, itemCenter, time, radiansPerFrame, ticksTillNextFrame);
        } else if (shrineInfo.getMergeAnimation().animate(items, itemCenter, time, radiansPerFrame, ticksTillNextFrame)) {
            finishedCraftingConsumer.accept(this, shrineInfo.tryCraft(items));
        }
    }
}
