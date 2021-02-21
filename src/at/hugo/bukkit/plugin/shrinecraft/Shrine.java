package at.hugo.bukkit.plugin.shrinecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class Shrine {
    private Material[][][] design;
    private int xOffset, yOffset, zOffset;

    private LinkedList<Recepie> recepies = new LinkedList<>();

    public Shrine(ConfigurationSection configurationSection) {
        List<List<List<String>>> configDesign = (List<List<List<String>>>) configurationSection.getList("design");
        {
            int y = 0;
            this.design = new Material[configDesign.size()][][];
            for (List<List<String>> list : configDesign) {
                int z = 0;
                this.design[y] = new Material[list.size()][];
                for (List<String> list2 : list) {
                    int x = 0;
                    this.design[y][z] = new Material[list2.size()];
                    for (String materialName : list2) {
                        this.design[y][z][x] = Material.matchMaterial(materialName);
                        ++x;
                    }
                    ++z;
                }
                ++y;
            }
        }
        
        List<Object> recepiesConfig = (List<Object>) configurationSection.getList("recepies");

        for (Object recepieObject : recepiesConfig) {
            ConfigurationSection recepieConfig = Utils.objectToConfigurationSection(recepieObject);
            recepies.add(new Recepie(recepieConfig));
        }

        ConfigurationSection offsets = Utils.objectToConfigurationSection(configurationSection.get("offset"));
        xOffset = offsets.getInt("x");
        yOffset = offsets.getInt("y");
        zOffset = offsets.getInt("z");

        for (int y = 0; y < this.design.length; y++) {
            for (int z = 0; z < this.design[y].length; z++) {
                ArrayList<String> output = new ArrayList<>(this.design[y].length);
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

    public boolean isAt(Block block) {
        if (!getCraftingBlockMaterial().equals(block.getType()))
            return false;

        Block startingBlock = block.getRelative(-xOffset, -yOffset, -zOffset);
        for (int y = 0; y < this.design.length; y++) {
            for (int z = 0; z < this.design[y].length; z++) {
                for (int x = 0; x < this.design[y][z].length; x++) {
                    if (this.design[y][z][x] != null
                            && !this.design[y][z][x].equals(startingBlock.getRelative(x, y, z).getType()))
                        return false;
                }
            }
        }
        return true;
    }

	public ItemStack getRecepie(Collection<Item> items) {
        for (Recepie recepie : recepies) {
            ItemStack result = recepie.isFullfilledBy(items);
            if(result != null){
                return result;
            }
        }
		return null;
	}

	public boolean hasSimilarRecepie(Collection<Item> items) {
        for (Recepie recepie : recepies) {
            if(recepie.isSemiFulfilled(items)){
                return true;
            }
        }
		return false;
	}
}
