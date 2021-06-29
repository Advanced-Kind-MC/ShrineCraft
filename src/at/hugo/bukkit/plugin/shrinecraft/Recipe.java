package at.hugo.bukkit.plugin.shrinecraft;

import com.advancedkind.plugin.utils.utils.ConfigUtils;
import com.jojodmo.customitems.api.CustomItemsAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Recipe {
    private final LinkedList<RecipeItem> recipeItems = new LinkedList<>();
    private final ItemStack result;
    private final int recipeItemCount;

    public Recipe(final ConfigurationSection recipeConfig) throws IllegalArgumentException {
        // parse recipe input items
        final List<?> in = recipeConfig.getList("in");
        for (final Object object : in) {
            if (object instanceof String) {
                String material = (String) object;
                // just the material is given
                recipeItems.add(new RecipeItem(1, ConfigUtils.getMaterial(material), null, null));
                continue;
            }
            // get configuration section from object
            final ConfigurationSection configurationSection = ConfigUtils.objectToConfigurationSection(object);
            if (configurationSection == null) {
                // couldnt parse configuration section
                throw new IllegalArgumentException(String.format("Could not parse input item number %s, could not get Configuration Section!", recipeItems.size() + 1));
            }
            // get material
            final Material material = ConfigUtils.getMaterial(configurationSection, "material");
            if (material == null) {
                throw new IllegalArgumentException(String.format("Could not parse input item number %s, unknown Material \"%s\"!", recipeItems.size() + 1, configurationSection.getString("material")));
            }
            // get amount
            final int amount = configurationSection.getInt("amount", 1);
            // get custom model number
            final Integer customModel = ConfigUtils.getInteger(configurationSection, "customModel");
            // load enchantments
            final HashMap<Enchantment, Integer> enchantments = new HashMap<>();
            if (configurationSection.isSet("enchantments")) {
                final ConfigurationSection enchantmentSection = ConfigUtils.objectToConfigurationSection(configurationSection.get("enchantments"));
                if (enchantmentSection != null) {
                    for (String enchantmentName : enchantmentSection.getKeys(false)) {
                        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(enchantmentName));
                        if (enchantment == null) {
                            throw new IllegalArgumentException(String.format("Could not parse input item number %s, Unknown Enchantment \"%s\"!", recipeItems.size() + 1, enchantmentName));
                        }
                        int level = enchantmentSection.getInt(enchantmentName);
                        enchantments.put(enchantment, level);
                    }
                }
            }
            RecipeItem recipeItem = new RecipeItem(amount, material, customModel, enchantments);
            recipeItems.add(recipeItem);
            JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().info(recipeItem.toString());
        }

        // save the amount of items needed for this recipe
        recipeItemCount = recipeItems.stream().mapToInt(recipeItem -> recipeItem.amount).sum();

        // load item result
        final ConfigurationSection outConfig = ConfigUtils.objectToConfigurationSection(recipeConfig.get("out"));

        // CustomItems support
        if (outConfig.isString("CustomItems-item")) {
            // check if plugin is enabled
            if (!Bukkit.getPluginManager().isPluginEnabled("CustomItems")) {
                JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().severe("Could not get CustomItems item, plugin is not enabled or installed!");
            } else {
                final String customItemsItemName = outConfig.getString("CustomItems-item");
                final ItemStack customItemItem = CustomItemsAPI.getCustomItem(customItemsItemName);
                if (customItemItem == null) {
                    throw new IllegalArgumentException(String.format("Could not load result item, unknown CustomItems item \"%s\"!", customItemsItemName));
                }

                result = customItemItem;
                return;
            }
        }
        // load out item material
        if (!outConfig.isString("material")) {
            throw new IllegalArgumentException("Could not load result item, no material is defined!");
        }
        Material material = ConfigUtils.getMaterial(outConfig, "material");
        if (material == null) {
            String materialName = outConfig.getString("material");
            throw new IllegalArgumentException(String.format("Could not load result item, unknown Material \"%s\"!", materialName));
        }
        // create item
        result = new ItemStack(material, outConfig.getInt("amount", 1));
        ItemMeta meta = result.getItemMeta();
        // set item name
        if (outConfig.isString("name")) {
            meta.displayName(ConfigUtils.getComponent(outConfig, "name"));
        }
        // set item lore
        if (outConfig.isList("lore")) {
            meta.lore(outConfig.getStringList("lore").stream().map(LegacyComponentSerializer.legacyAmpersand()::deserialize).collect(Collectors.toList()));
        }
        // set custom model data
        if (outConfig.isInt("customModel")) {
            meta.setCustomModelData(outConfig.getInt("customModel"));
        }
        // apply item meta
        result.setItemMeta(meta);
    }

    public ItemStack isFulfilledBy(final Collection<Item> items) {
        int itemCount = items.stream().mapToInt(item -> item.getItemStack().getAmount()).sum();
        if (itemCount != recipeItemCount) return null;
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecipeItem item : recipeItems) {
            item.reduceListWithApplyingItems(itemStacks);
        }
        return itemStacks.isEmpty() ? result.clone() : null;
    }

    public boolean isSemiFulfilled(Collection<Item> items) {
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecipeItem item : recipeItems) {
            item.reduceListWithApplyingItems(itemStacks);
        }
        return itemStacks.isEmpty();
    }

    public boolean wouldAcceptAnyOf(List<Item> items) {
        return items.stream().anyMatch(item -> recipeItems.stream().anyMatch(recipeItem -> recipeItem.matches(item.getItemStack())));
    }

    private static class RecipeItem {
        private final int amount;
        private final @Nullable Material material;
        private final @Nullable Integer customModel;
        private final @NotNull HashMap<Enchantment, Integer> enchantments = new HashMap<>();

        public RecipeItem(final int amount, final @Nullable Material material, final @Nullable Integer customModel, final @Nullable HashMap<Enchantment, Integer> enchantments) {
            this.amount = amount;
            this.material = material;
            this.customModel = customModel;
            if (enchantments != null) {
                this.enchantments.putAll(enchantments);
            }
        }

        boolean isFulfilledBy(final Collection<ItemStack> items) {
            int count = 0;
            for (final ItemStack item : items) {
                if (!matches(item)) continue;
                count += item.getAmount();
            }
            return count >= amount;
        }

        void reduceListWithApplyingItems(Collection<ItemStack> items) {
            int count = amount;
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                final ItemStack item = iterator.next();
                if (!matches(item)) continue;
                if (count >= item.getAmount()) {
                    count -= item.getAmount();
                    iterator.remove();
                    if (count == 0)
                        return;
                } else {
                    item.setAmount(item.getAmount() - count);
                    return;
                }
            }
        }

        private boolean matches(ItemStack item) {
            final ItemMeta meta = item.getItemMeta();
            if (!material.equals(item.getType()))
                return false;
            if (customModel != null && (!meta.hasCustomModelData() || meta.getCustomModelData() != customModel))
                return false;
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
                if (!enchantments.entrySet().stream().allMatch(entry -> entry.getValue().equals(enchantmentStorageMeta.getStoredEnchantLevel(entry.getKey())))) {
                    return false;
                }
            } else {
                if (!enchantments.entrySet().stream().allMatch(entry -> entry.getValue().equals(item.getEnchantmentLevel(entry.getKey())))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return String.format("%s (%s): custom model: %s; enchantments: %s", material.toString(), amount, customModel, enchantments.entrySet().stream().map(entry -> String.format("%s (%s)", entry.getKey(), entry.getValue())).collect(Collectors.joining(", ")));
        }
    }
}
