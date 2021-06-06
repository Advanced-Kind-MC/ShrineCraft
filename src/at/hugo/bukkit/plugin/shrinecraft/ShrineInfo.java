package at.hugo.bukkit.plugin.shrinecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import at.hugo.bukkit.plugin.shrinecraft.animation.idle.CircularIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.idle.IIdleAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.IMergeAnimation;
import at.hugo.bukkit.plugin.shrinecraft.animation.merge.SimpleMergeAnimation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ShrineInfo {
    private Material[][][] design;
    private final int xOffset, yOffset, zOffset;

    private final @NotNull IIdleAnimation idleAnimation;
    private final @NotNull IMergeAnimation mergeAnimation;

    private final LinkedList<Recipe> recipes = new LinkedList<>();
    private final EnumSet<BlockFace> directions = EnumSet.noneOf(BlockFace.class);

    public ShrineInfo(final ConfigurationSection configurationSection) {
        final List<List<List<String>>> configDesign = (List<List<List<String>>>) configurationSection.getList("design");
        {
            int y = 0;
            this.design = new Material[configDesign.size()][][];
            for (final List<List<String>> list : configDesign) {
                int z = 0;
                this.design[y] = new Material[list.size()][];
                for (final List<String> list2 : list) {
                    int x = 0;
                    this.design[y][z] = new Material[list2.size()];
                    for (final String materialName : list2) {
                        if(materialName == null || materialName.isBlank() || materialName.equalsIgnoreCase("null"))
                            this.design[y][z][x]=null;
                        else
                            this.design[y][z][x] =  Material.matchMaterial(materialName);
                        ++x;
                    }
                    ++z;
                }
                ++y;
            }
        }

        final List<?> recipesConfig = configurationSection.getList("recipes");

        for (final Object recipeObject : recipesConfig) {
            final ConfigurationSection recipeConfig = Utils.objectToConfigurationSection(recipeObject);
            recipes.add(new Recipe(recipeConfig));
        }

        final ConfigurationSection offsets = Utils.objectToConfigurationSection(configurationSection.get("offset"));
        xOffset = offsets.getInt("x");
        yOffset = offsets.getInt("y");
        zOffset = offsets.getInt("z");

        if (configurationSection.isString("direction")) {
            String direction = configurationSection.getString("direction");
            directions.add(BlockFace.valueOf(direction));
        } else if (configurationSection.isList("direction")) {
            configurationSection.getStringList("direction").forEach(direction -> this.directions.add(BlockFace.valueOf(direction)));
        } else {
            directions.add(BlockFace.NORTH);
            directions.add(BlockFace.EAST);
            directions.add(BlockFace.SOUTH);
            directions.add(BlockFace.WEST);
        }
        switch (configurationSection.getString("animation.idle", "circular")){
            default:
                idleAnimation = new CircularIdleAnimation();
        }
        switch (configurationSection.getString("animation.idle", "simple")){
            default:
                mergeAnimation = new SimpleMergeAnimation();
        }
//        for (int y = 0; y < this.design.length; y++) {
//            for (int z = 0; z < this.design[y].length; z++) {
//                final ArrayList<String> output = new ArrayList<>(this.design[y].length);
//                for (int x = 0; x < this.design[y][z].length; x++) {
//                    if(this.design[y][z][x] == null)
//                        output.add("     ");
//                    else
//                        output.add(this.design[y][z][x].toString());
//                }
//                Bukkit.getLogger().info(() -> String.join(", ", output));
//            }
//        }
        Bukkit.getLogger().info(() -> String.format("Offsets: %s, %s, %s", xOffset, yOffset, zOffset));
        Bukkit.getLogger().info(() -> "Input block: " + getCraftingBlockMaterial().toString());

    }

    public IIdleAnimation getIdleAnimation() {
        return idleAnimation;
    }

    public IMergeAnimation getMergeAnimation() {
        return mergeAnimation;
    }

    public Material getCraftingBlockMaterial() {
        return design[yOffset][zOffset][xOffset];
    }

    public ItemStack tryCraft(final Collection<Item> items) {
        for (final Recipe recipe : recipes) {
            final ItemStack result = recipe.isFullfilledBy(items);
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
        for (BlockFace direction : directions) {
            if(isAt(block,direction))
                return true;
        }
        return false;
    }

    private boolean isAt(final Block block, BlockFace direction) {
        Block startingBlock;
        switch (direction) {
            case NORTH:
                startingBlock = block.getRelative(-xOffset, -yOffset, -zOffset);
                for (int y = 0; y > -this.design.length; y--) {
                    for (int z = 0; z < this.design[y].length; z++) {
                        for (int x = 0; x < this.design[y][z].length; x++) {
                            if (this.design[y][z][x] != null
                                    && !this.design[y][z][x].equals(startingBlock.getRelative(x, y, z).getType()))
                                return false;
                        }
                    }
                }
                break;
            case SOUTH:
                startingBlock = block.getRelative(xOffset, -yOffset, zOffset);
                for (int y = 0; y > -this.design.length; y--) {
                    for (int z = this.design[y].length - 1; z >= 0; z--) {
                        for (int x = this.design[y][z].length - 1; x >= 0; x--) {
                            if (this.design[y][z][x] != null
                                    && !this.design[y][z][x].equals(startingBlock.getRelative(x, y, z).getType()))
                                return false;
                        }
                    }
                }
                break;
            case EAST:
                startingBlock = block.getRelative(zOffset, -yOffset, -xOffset);
                for (int y = 0; y > -this.design.length; y--) {
                    for (int z = this.design[y].length - 1; z >= 0; z--) {
                        for (int x = 0; x < this.design[y][z].length; x++) {
                            if (this.design[y][z][x] != null
                                    && !this.design[y][z][x].equals(startingBlock.getRelative(x, y, z).getType()))
                                return false;
                        }
                    }
                }
                break;
            case WEST:
                startingBlock = block.getRelative(-zOffset, -yOffset, xOffset);
                for (int y = 0; y > -this.design.length; y--) {
                    for (int z = 0; z < this.design[y].length; z++) {
                        for (int x = this.design[y][z].length - 1; x >= 0; x--) {
                            if (this.design[y][z][x] != null
                                    && !this.design[y][z][x].equals(startingBlock.getRelative(x, y, z).getType()))
                                return false;
                        }
                    }
                }
                break;

        }
        return true;
    }
}
