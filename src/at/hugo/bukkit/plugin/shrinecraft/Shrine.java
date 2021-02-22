package at.hugo.bukkit.plugin.shrinecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class Shrine {
    private Material[][][] design;
    private final int xOffset, yOffset, zOffset;

    private final LinkedList<Recepie> recepies = new LinkedList<>();

    public Shrine(final ConfigurationSection configurationSection) {
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
                        this.design[y][z][x] = Material.matchMaterial(materialName);
                        ++x;
                    }
                    ++z;
                }
                ++y;
            }
        }

        final List<?> recepiesConfig = configurationSection.getList("recepies");

        for (final Object recepieObject : recepiesConfig) {
            final ConfigurationSection recepieConfig = Utils.objectToConfigurationSection(recepieObject);
            recepies.add(new Recepie(recepieConfig));
        }

        final ConfigurationSection offsets = Utils.objectToConfigurationSection(configurationSection.get("offset"));
        xOffset = offsets.getInt("x");
        yOffset = offsets.getInt("y");
        zOffset = offsets.getInt("z");

        for (int y = 0; y < this.design.length; y++) {
            for (int z = 0; z < this.design[y].length; z++) {
                final ArrayList<String> output = new ArrayList<>(this.design[y].length);
                for (int x = 0; x < this.design[y][z].length; x++) {
                    output.add(this.design[y][z][x].toString());
                }
                Bukkit.getLogger().info(() -> String.join(", ", output));
            }
        }
        Bukkit.getLogger().info(() -> String.format("Offsets: %s, %s, %s", xOffset, yOffset, zOffset));
        Bukkit.getLogger().info(() -> "Input block: " + getCraftingBlockMaterial().toString());

    }

    public Material getCraftingBlockMaterial() {
        return design[yOffset][zOffset][xOffset];
    }

    public boolean isAt(final Block block) {
        if (!getCraftingBlockMaterial().equals(block.getType()))
            return false;

        return isAt(block, BlockFace.NORTH) || isAt(block, BlockFace.EAST) || isAt(block, BlockFace.SOUTH)
                || isAt(block, BlockFace.WEST);
    }

    public ItemStack getRecepie(final Collection<Item> items) {
        for (final Recepie recepie : recepies) {
            final ItemStack result = recepie.isFullfilledBy(items);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public boolean hasSimilarRecepie(final Collection<Item> items) {
        for (final Recepie recepie : recepies) {
            if (recepie.isSemiFulfilled(items)) {
                return true;
            }
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
                    for (int z = this.design[y].length-1; z >= 0; z--) {
                        for (int x = this.design[y][z].length-1; x >= 0; x--) {
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
                    for (int z = this.design[y].length-1; z >= 0; z--) {
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
                        for (int x = this.design[y][z].length-1; x >= 0; x--) {
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
