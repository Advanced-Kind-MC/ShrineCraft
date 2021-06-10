package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.advancedkind.plugin.utils.utils.ConfigUtils;
import com.jojodmo.customitems.api.CustomItemsAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Recipe {
    private static class RecipeItem {
        final int amount;
        final Material material;
        final Integer customModel;
        final String name;
        final List<String> lore;

        public RecipeItem(final int amount, final Material material, final Integer customModel, final String name,
                          final List<String> lore) {
            this.amount = amount;
            this.material = material;
            this.customModel = customModel;
            this.name = name;
            this.lore = lore;
        }

        boolean isFulfilledBy(final Collection<ItemStack> items) {
            int count = 0;
            for (final ItemStack item : items) {
                final ItemMeta meta = item.getItemMeta();
                if (!material.equals(item.getType()))
                    continue;
                if (customModel != null && meta.getCustomModelData() != customModel)
                    continue;
                if (name != null && (!meta.hasDisplayName() || !PlainComponentSerializer.plain().serialize(meta.displayName()).equals(name)))
                    continue;
                if (lore != null && !lore.isEmpty() && (!meta.hasLore() || !meta.lore().stream().map(PlainComponentSerializer.plain()::serialize).collect(Collectors.toList()).containsAll(lore)))
                    continue;
                count += item.getAmount();
            }
            return count >= amount;
        }

        void reduceListWithApplyingItems(Collection<ItemStack> items) {
            int count = amount;
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                final ItemStack item = iterator.next();
                final ItemMeta meta = item.getItemMeta();
                if (!material.equals(item.getType()))
                    continue;
                if (customModel != null && meta.getCustomModelData() != customModel)
                    continue;
                if (name != null && (!meta.hasDisplayName() || !PlainComponentSerializer.plain().serialize(meta.displayName()).equals(name)))
                    continue;
                if (lore != null && !lore.isEmpty() && (!meta.hasLore() || !meta.lore().stream().map(PlainComponentSerializer.plain()::serialize).collect(Collectors.toList()).containsAll(lore)))
                    continue;
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

        @Override
        public String toString() {
            return String.format("%s (%s): %s; %s", material.toString(), amount, name, customModel);
        }
    }

    private final LinkedList<RecipeItem> recipeItems = new LinkedList<>();
    private final ItemStack result;

    public Recipe(final ConfigurationSection recipeConfig) {
        final List<?> in = recipeConfig.getList("in");
        for (final Object object : in) {
            if (object instanceof String) {
                recipeItems.add(new RecipeItem(1, ConfigUtils.getMaterial((String) object), null, null, null));
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
            Integer customModel = configurationSection.getInt("customModel", -1);
            if (customModel < 0)
                customModel = null;
            final String name = configurationSection.getString("name", null);
            final List<String> lore = configurationSection.getStringList("lore");
            recipeItems.add(new RecipeItem(amount, material, customModel, name, lore));

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
