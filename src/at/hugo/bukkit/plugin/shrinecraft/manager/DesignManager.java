package at.hugo.bukkit.plugin.shrinecraft.manager;

import at.hugo.bukkit.plugin.shrinecraft.ShrineCraftPlugin;
import at.hugo.bukkit.plugin.shrinecraft.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        public final int xInputOffset, yInputOffset, zInputOffset;

        private Design(@NotNull Material[][][] materials, int xInputOffset, int yInputOffset, int zInputOffset) {
            this.materials = materials;
            this.xInputOffset = xInputOffset;
            this.yInputOffset = yInputOffset;
            this.zInputOffset = zInputOffset;
        }

        public Material getInputBlockMaterial() {
            return materials[yInputOffset][zInputOffset][xInputOffset];
        }

        private Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("materials", Utils.convertMaterialsArrayToStringList(materials));
            Map<String, Object> inputOffset = new HashMap<>();
            inputOffset.put("x", xInputOffset);
            inputOffset.put("z", zInputOffset);
            inputOffset.put("y", yInputOffset);
            result.put("input-offset", inputOffset);
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
            final int x = designConfig.getInt("input-offset.x");
            final int y = designConfig.getInt("input-offset.y");
            final int z = designConfig.getInt("input-offset.z");
            designMap.put(designKey, new Design(materials, x, y, z));
        }
    }

    public void setDesign(String designKey, @NotNull Material[][][] materials, int xInputOffset, int yInputOffset, int zInputOffset) {
        Design design = new Design(materials, xInputOffset, yInputOffset, zInputOffset);
        this.designMap.put(designKey, design);
        plugin.getDesignConfig().set(designKey, design.serialize());
        plugin.getDesignConfig().save();
    }

    @Nullable
    public Design getDesign(@NotNull String designKey) {
        return designMap.get(designKey);
    }
}
