package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import at.hugo.bukkit.plugin.shrinecraft.animation.idle.CircularIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.idle.IIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.IMergeAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.SimpleMergeAnimation;
import at.hugo.bukkit.plugin.shrinecraft.manager.DesignManager;
import com.advancedkind.plugin.utils.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ShrineInfo {
    private final @NotNull ShrineCraftPlugin plugin;

    private final @NotNull String designKey;

    private final @NotNull IIdleAnimation idleAnimation;
    private final @NotNull IMergeAnimation mergeAnimation;

    private final LinkedList<Recipe> recipes = new LinkedList<>();
    private final EnumSet<DesignManager.Design.Direction> directions = EnumSet.noneOf(DesignManager.Design.Direction.class);

    public ShrineInfo(@NotNull ShrineCraftPlugin plugin, final ConfigurationSection configurationSection) {
        this.plugin = plugin;

        this.designKey = configurationSection.getString("design");

        final List<?> recipesConfig = configurationSection.getList("recipes");

        for (final Object recipeObject : recipesConfig) {
            final ConfigurationSection recipeConfig = ConfigUtils.objectToConfigurationSection(recipeObject);
            recipes.add(new Recipe(recipeConfig));
        }

        if (configurationSection.isString("direction")) {
            String direction = configurationSection.getString("direction");
            directions.add(DesignManager.Design.Direction.valueOf(direction));
        } else if (configurationSection.isList("direction")) {
            configurationSection.getStringList("direction").forEach(direction -> this.directions.add(DesignManager.Design.Direction.valueOf(direction)));
        } else {
            directions.add(DesignManager.Design.Direction.NORTH);
            directions.add(DesignManager.Design.Direction.EAST);
            directions.add(DesignManager.Design.Direction.SOUTH);
            directions.add(DesignManager.Design.Direction.WEST);
        }
        switch (configurationSection.getString("animation.idle", "circular")) {
            default:
                idleAnimation = new CircularIdleAnimation();
        }
        switch (configurationSection.getString("animation.idle", "simple")) {
            default:
                mergeAnimation = new SimpleMergeAnimation();
        }
    }

    public @NotNull IIdleAnimation getIdleAnimation() {
        return idleAnimation;
    }

    public @NotNull IMergeAnimation getMergeAnimation() {
        return mergeAnimation;
    }

    public Material getCraftingBlockMaterial() {
        return plugin.getDesignManager().getDesign(designKey).getInputBlockMaterial();
    }

    public ItemStack tryCraft(final Collection<Item> items) {
        for (final Recipe recipe : recipes) {
            final ItemStack result = recipe.isFulfilledBy(items);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public boolean hasSimilarRecipe(final Collection<Item> items) {
        for (final Recipe recipe : recipes) {
            if (recipe.isSemiFulfilled(items)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAt(final Block block) {
        if (!getCraftingBlockMaterial().equals(block.getType()))
            return false;
        for (var direction : directions) {
            if (isAt(block, direction))
                return true;
        }
        return false;
    }

    private boolean isAt(final Block block, DesignManager.Design.Direction direction) {
        DesignManager.Design design = plugin.getDesignManager().getDesign(designKey);
        if (design == null)
            return false;
        for (int y = 0; y < design.materials.length; y++) {
            for (int z = 0; z < design.materials[y].length; z++) {
                for (int x = 0; x < design.materials[y][z].length; x++) {
                    Material material = design.materials[y][z][x];
                    if (material == null) continue;
                    Vector current = new Vector(x - design.xInputOffset, y - design.yInputOffset, z - design.zInputOffset);
                    direction.rotateVector(current);
                    if (!material.equals(block.getRelative(current.getBlockX(), current.getBlockY(), current.getBlockZ()).getType())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
