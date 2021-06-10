package at.hugo.bukkit.plugin.shrinecraft.manager;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DesignManager {
    public static class Design {
        public enum Direction {
            NORTH, EAST, SOUTH, WEST;

            public Vector rotateVector(Vector vector) {
                for (int i = 0; i < ordinal(); i++) {
                    double x = vector.getX();
                    double z = vector.getZ();
                    vector.setZ(x);
                    vector.setX(-z);
                }
                return vector;
            }
        }

        public final Material[][][] materials;
        public final @NotNull BlockVector[] inputOffsets;

        private Design(@NotNull Material[][][] materials, final @NotNull BlockVector[] inputOffsets) {
            this.materials = materials;
            this.inputOffsets = inputOffsets;
        }

        public Set<Material> getInputBlockMaterials() {
            EnumSet<Material> result = EnumSet.noneOf(Material.class);
            for (BlockVector inputOffset : inputOffsets) {
                Material material = materials[inputOffset.getBlockY()][inputOffset.getBlockZ()][inputOffset.getBlockX()];
                if (material != null) {
                    result.add(material);
                }
            }
            return result;
        }

        private Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("materials", Utils.convertMaterialsArrayToStringList(materials));
            result.put("input-offsets", inputOffsets);
            return result;
        }
    }

    private final @NotNull ShrineCraftPlugin plugin;
    private final HashMap<String, Design> designMap = new HashMap<>();

    public DesignManager(final @NotNull ShrineCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        designMap.clear();
        for (String designKey : plugin.getDesignConfig().getKeys(false)) {
            final ConfigurationSection designConfig = plugin.getDesignConfig().getConfigurationSection(designKey);
            @SuppressWarnings("unchecked") final List<List<List<String>>> materialList = (List<List<List<String>>>) designConfig.getList("materials");
            final Material[][][] materials = Utils.convertStringListToMaterialArray(materialList);
            final List<BlockVector> inputOffsetsList = (List<BlockVector>) designConfig.getList("input-offsets");
            final BlockVector[] inputOffsets = inputOffsetsList.toArray(new BlockVector[inputOffsetsList.size()]);
            designMap.put(designKey, new Design(materials, inputOffsets));
        }
    }

    public void setDesign(String designKey, @NotNull Material[][][] materials, BlockVector[] inputOffsets) {
        Design design = new Design(materials, inputOffsets);
        this.designMap.put(designKey, design);
        plugin.getDesignConfig().set(designKey, design.serialize());
        plugin.getDesignConfig().save();
    }

    @Nullable
    public Design getDesign(@NotNull String designKey) {
        return designMap.get(designKey);
    }
}
