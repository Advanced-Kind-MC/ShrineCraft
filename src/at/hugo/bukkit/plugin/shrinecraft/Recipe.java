package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.advancedkind.plugin.utils.utils.ConfigUtils;
import com.jojodmo.customitems.api.CustomItemsAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Recipe {
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
            if (customModel != null && meta.getCustomModelData() != customModel)
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

    private final LinkedList<RecipeItem> recipeItems = new LinkedList<>();
    private final ItemStack result;

    public Recipe(final ConfigurationSection recipeConfig) {
        final List<?> in = recipeConfig.getList("in");
        for (final Object object : in) {
            if (object instanceof String) {
                recipeItems.add(new RecipeItem(1, ConfigUtils.getMaterial((String) object), null, null));
                continue;
            }
            final ConfigurationSection configurationSection = ConfigUtils.objectToConfigurationSection(object);
            if (configurationSection == null) {
                Bukkit.getLogger().severe("Could not load recipe!");
                continue;
            }
            final Material material = ConfigUtils.getMaterial(configurationSection, "material");
            if (material == null) {
                Bukkit.getLogger().severe("Unknown Material \"" + configurationSection.getString("material") + "\" for your item inputs");
                continue;
            }
            final int amount = configurationSection.getInt("amount", 1);
            final Integer customModel = ConfigUtils.getInteger(configurationSection, "customModel");
            final HashMap<Enchantment, Integer> enchantments = new HashMap<>();
            final ConfigurationSection enchantmentSection;
            if (configurationSection.isConfigurationSection("enchantments")) {
                enchantmentSection = configurationSection.getConfigurationSection("enchantments");
            } else if (configurationSection.isSet("enchantments")) {
                enchantmentSection = ConfigUtils.objectToConfigurationSection(configurationSection.get("enchantments"));
            } else {
                enchantmentSection = null;
            }

            if (enchantmentSection != null) {
                for (String enchantmentName : enchantmentSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(enchantmentName));
                    if (enchantment == null) {
                        JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().warning("Unknown enchantment: " + enchantmentName);
                    }
                    int level = enchantmentSection.getInt(enchantmentName);
                    enchantments.put(enchantment, level);
                }
            }
            RecipeItem recipeItem = new RecipeItem(amount, material, customModel, enchantments);
            recipeItems.add(recipeItem);
            JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().info(recipeItem.toString());

        }

        final ConfigurationSection outConfig = ConfigUtils.objectToConfigurationSection(recipeConfig.get("out"));
        if (outConfig.isString("CustomItems-item")) {
            final String customItemsItemName = outConfig.getString("CustomItems-item");
            if (!Bukkit.getPluginManager().isPluginEnabled("CustomItems")) {
                JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().warning("CustomItems not enabled or installed!");
            } else {
                ItemStack customItemItem = CustomItemsAPI.getCustomItem(customItemsItemName);
                if (customItemItem == null) {
                    JavaPlugin.getPlugin(ShrineCraftPlugin.class).getLogger().warning(String.format("Unknown CustomItems item \"%s\"!", customItemsItemName));
                } else {
                    result = customItemItem;
                    return;
                }
            }
        }

        result = new ItemStack(ConfigUtils.getMaterial(outConfig, "material"));
        if (outConfig.isInt("amount"))
            result.setAmount(outConfig.getInt("amount"));
        ItemMeta meta = result.getItemMeta();
        if (outConfig.isString("name"))
            meta.displayName(ConfigUtils.getComponent(outConfig, "name"));

        if (outConfig.isList("lore"))
            meta.lore(outConfig.getStringList("lore").stream().map(LegacyComponentSerializer.legacyAmpersand()::deserialize).collect(Collectors.toList()));
        if (outConfig.isInt("customModel"))
            meta.setCustomModelData(outConfig.getInt("customModel"));
        result.setItemMeta(meta);
    }

    public ItemStack isFulfilledBy(final Collection<Item> items) {
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecipeItem item : recipeItems) {
            if (!item.isFulfilledBy(itemStacks))
                return null;
        }
        return result;
    }

    public boolean isSemiFulfilled(Collection<Item> items) {
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecipeItem item : recipeItems) {
            item.reduceListWithApplyingItems(itemStacks);
        }
        return itemStacks.isEmpty();
    }

}
