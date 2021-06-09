package at.hugo.bukkit.plugin.shrinecraft;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Utils {

    public static final Vector VECTOR_ZERO = new Vector(0, 0, 0);


    private Utils() {
    }

    public static Collection<ItemStack> condenseItemsToItemStacks(final Collection<Item> items) {
        final LinkedList<ItemStack> itemStacks = new LinkedList<>();
        for (final Item item : items) {
            ItemStack itemStack = item.getItemStack().clone();
            for (final ItemStack i : itemStacks) {
                if (i.isSimilar(itemStack)) {
                    i.setAmount(i.getAmount() + itemStack.getAmount());
                    itemStack = null;
                    break;
                }
            }
            if (itemStack != null)
                itemStacks.add(itemStack);

        }
        return itemStacks;
    }

    @NotNull
    public static Material[][][] convertStringListToMaterialArray(List<List<List<String>>> materialYList) {
        final Material[][][] materials = new Material[materialYList.size()][][];
        for (int y = 0; y < materials.length; y++) {
            final List<List<String>> materialZList = materialYList.get(materials.length - y - 1);
            materials[y] = new Material[materialZList.size()][];
            for (int z = 0; z < materials[y].length; z++) {
                final List<String> materialXList = materialZList.get(z);
                materials[y][z] = new Material[materialXList.size()];
                for (int x = 0; x < materials[y][z].length; x++) {
                    materials[y][z][x] = Material.matchMaterial(materialXList.get(x));
                }
            }
        }
        return materials;
    }

    @NotNull
    public static List<List<List<String>>> convertMaterialsArrayToStringList(Material[][][] materials) {
        final List<List<List<String>>> result = new LinkedList<>();
        for (int y = 0; y < materials.length; y++) {
            List<List<String>> zList = new LinkedList<>();
            for (int z = 0; z < materials[y].length; z++) {
                List<String> xList = new LinkedList<>();
                for (int x = 0; x < materials[y][z].length; x++) {
                    Material material = materials[materials.length - y - 1][z][x];
                    if (material == null) {
                        xList.add("");
                    } else {
                        xList.add(material.toString());
                    }
                }
                zList.add(xList);
            }
            result.add(zList);
        }
        return result;
    }
}
