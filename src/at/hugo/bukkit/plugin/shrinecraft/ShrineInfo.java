package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.hugo.bukkit.plugin.shrinecraft.animation.idle.CircularIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.idle.IIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.IMergeAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.SimpleMergeAnimation;
import at.hugo.bukkit.plugin.shrinecraft.manager.DesignManager;
import com.advancedkind.plugin.utils.utils.ConfigUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
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

    public Set<Material> getCraftingBlockMaterials() {
        DesignManager.Design design = plugin.getDesignManager().getDesign(designKey);
        if (design != null) {
            return design.getInputBlockMaterials();
        } else {
            return EnumSet.noneOf(Material.class);
        }
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

    public ShrinePosition getShrinePositionAt(final Block block) {
        if (!getCraftingBlockMaterials().contains(block.getType()))
            return null;
        DesignManager.Design design = plugin.getDesignManager().getDesign(designKey);

        if (design == null)
            return null;

        for (var direction : directions) {
            BlockVector offset = getOffset(block, direction, design);
            if (offset != null) {
                Location origin = block.getLocation().clone().subtract(direction.rotateVector(offset.clone()));
                return new ShrinePosition(this, direction, origin);
            }
        }
        return null;
    }


    private BlockVector getOffset(final Block block, DesignManager.Design.Direction direction, DesignManager.Design design) {
        BlockVector[] offsets = design.inputOffsets;
        for (BlockVector offset : offsets) {
            if (isAt(block, direction, design, offset)) {
                return offset;
            }
        }

        return null;
    }

    public boolean isAt(Block block, DesignManager.Design.Direction direction, DesignManager.Design design, BlockVector offset) {
        for (int y = 0; y < design.materials.length; y++) {
            for (int z = 0; z < design.materials[y].length; z++) {
                for (int x = 0; x < design.materials[y][z].length; x++) {
                    Material material = design.materials[y][z][x];
                    if (material == null) continue;
                    Vector current = new Vector(x, y, z).subtract(offset);
                    direction.rotateVector(current);
                    if (!material.equals(block.getRelative(current.getBlockX(), current.getBlockY(), current.getBlockZ()).getType())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public BlockVector[] getInputOffsets() {
        DesignManager.Design design = plugin.getDesignManager().getDesign(designKey);
        if (design != null) {
            return design.inputOffsets;
        } else {
            return new BlockVector[]{new BlockVector(0, 0, 0)};
        }

    }

    public boolean isAt(Location center, DesignManager.Design.Direction direction, BlockVector offset) {
        DesignManager.Design design = plugin.getDesignManager().getDesign(designKey);
        if (design == null) {
            return false;
        }
        return isAt(center.getBlock(), direction, design, offset);
    }

    public static class ShrineInputLocation {
        private final @NotNull Location location;
        private final @NotNull DesignManager.Design.Direction direction;
        private final @NotNull BlockVector offset;
        private final @NotNull ShrineInfo shrineInfo;

        public ShrineInputLocation(@NotNull Location location, @NotNull DesignManager.Design.Direction direction, @NotNull BlockVector offset, @NotNull ShrineInfo shrineInfo) {
            this.location = location;
            this.direction = direction;
            this.offset = offset;
            this.shrineInfo = shrineInfo;
        }

        public Location getLocation() {
            return location;
        }

        public DesignManager.Design.Direction getDirection() {
            return direction;
        }

        public BlockVector getOffset() {
            return offset;
        }

        public ShrineInfo getShrineInfo() {
            return shrineInfo;
        }
    }

    public static class ShrinePosition {
        private final @NotNull ShrineInfo shrineInfo;
        private final @NotNull DesignManager.Design.Direction direction;
        private final @NotNull Location origin;

        public ShrinePosition(@NotNull ShrineInfo shrineInfo, @NotNull DesignManager.Design.Direction direction, @NotNull Location origin) {
            this.shrineInfo = shrineInfo;
            this.direction = direction;
            this.origin = origin;
        }

        public ShrineInfo getShrineInfo() {
            return shrineInfo;
        }

        public DesignManager.Design.Direction getDirection() {
            return direction;
        }

        public Location getOrigin() {
            return origin;
        }
    }
}
