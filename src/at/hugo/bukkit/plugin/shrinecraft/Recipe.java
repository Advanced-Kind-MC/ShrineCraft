package at.hugo.bukkit.plugin.shrinecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Recipe {

    private class RecepieItem {
        final int amount;
        final Material material;
        final Integer customModel;
        final String name;
        final List<String> lore;

        public RecepieItem(final int amount, final Material material, final Integer customModel, final String name,
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
                if (name != null && (!meta.hasDisplayName() || !meta.getDisplayName().equals(name)))
                    continue;
                if (lore != null && !lore.isEmpty() && (!meta.hasLore() || !meta.getLore().containsAll(lore)))
                    continue;
                count += item.getAmount();
            }
            return count >= amount;
        }
        void reduceListWithApplyingItems(Collection<ItemStack> items){
            int count = amount;
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                final ItemStack item = iterator.next();
                final ItemMeta meta = item.getItemMeta();
                if (!material.equals(item.getType()))
                    continue;
                if (customModel != null && meta.getCustomModelData() != customModel)
                    continue;
                if (name != null && (!meta.hasDisplayName() || !meta.getDisplayName().equals(name)))
                    continue;
                if (lore != null && !lore.isEmpty() && (!meta.hasLore() || !meta.getLore().containsAll(lore)))
                    continue;
                if(count >= item.getAmount()){
                    count -= item.getAmount();
                    iterator.remove();
                    if(count == 0)
                        return;
                } else {
                    item.setAmount(item.getAmount()-count);
                    return;
                }
            }
        }
        @Override
        public String toString() {
            return String.format("%s (%s): %s; %s", material.toString(), amount, name, customModel);
        }
    }

    private final LinkedList<RecepieItem> recepieItems = new LinkedList<>();
    private final ItemStack result;

    public Recipe(final ConfigurationSection recepieConfig) {
        final List<?> in = recepieConfig.getList("in");
        for (final Object object : in) {
            if (object instanceof String) {
                recepieItems.add(new RecepieItem(1, Material.matchMaterial((String) object), null, null, null));
                continue;
            }
            final ConfigurationSection configurationSection = Utils.objectToConfigurationSection(object);
            if (configurationSection == null) {
                Bukkit.getLogger().severe("Could not load recepie!");
                continue;
            }
            final Material material = Material.matchMaterial(configurationSection.getString("material"));
            if(material == null) {
                Bukkit.getLogger().severe("Unknown Material \""+configurationSection.getString("material")+"\" for your item inputs");
                continue;
            }
            final int amount = configurationSection.getInt("amount", 1);
            Integer customModel = configurationSection.getInt("customModel", -1);
            if (customModel < 0)
                customModel = null;
            final String name = configurationSection.getString("name", null);
            final List<String> lore = configurationSection.getStringList("lore");
            recepieItems.add(new RecepieItem(amount, material, customModel, name, lore));

        }
        
        final ConfigurationSection outConfig = Utils.objectToConfigurationSection(recepieConfig.get("out"));
        result = new ItemStack(Material.matchMaterial(outConfig.getString("material")));
        if(outConfig.isInt("amount"))
            result.setAmount(outConfig.getInt("amount"));
        ItemMeta meta = result.getItemMeta();
        if(outConfig.isString("name"))
            meta.setDisplayName(Utils.colorize(outConfig.getString("name")));
        if(outConfig.isList("lore"))
            meta.setLore(outConfig.getStringList("lore").stream().map(Utils::colorize).collect(Collectors.toList()));
        if(outConfig.isInt("customModel"))
            meta.setCustomModelData(outConfig.getInt("customModel"));
        result.setItemMeta(meta);

        for (RecepieItem item : recepieItems) {
            Bukkit.getLogger().info(item.toString());
        }
    }

    public ItemStack isFullfilledBy(final Collection<Item> items) {
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecepieItem item : recepieItems) {
            if(!item.isFulfilledBy(itemStacks))
                return null;
        }
		return result;
	}

	public boolean isSemiFulfilled(Collection<Item> items) {
        final Collection<ItemStack> itemStacks = Utils.condenseItemsToItemStacks(items);
        for (final RecepieItem item : recepieItems) {
           item.reduceListWithApplyingItems(itemStacks);
        }
		return itemStacks.isEmpty();
	}

}
