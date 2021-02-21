package at.hugo.bukkit.plugin.shrinecraft;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class Utils {
    private Utils() { }
    @SuppressWarnings("unchecked")
    public static final ConfigurationSection objectToConfigurationSection(final Object object) {
        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Bukkit.getLogger().warning("couldn't parse Config of type: " + object.getClass().getSimpleName());
            return null;
        }
    }

    public static final Collection<ItemStack> condenseItemsToItemStacks(final Collection<Item> items) {
        final LinkedList<ItemStack> itemStacks = new LinkedList<>();
        for (final Item item : items) {
            ItemStack itemStack = item.getItemStack().clone();
            for (final ItemStack i : itemStacks) {
                if(i.isSimilar(itemStack)) {
                    i.setAmount(i.getAmount()+itemStack.getAmount());
                    itemStack = null;
                    break;
                }
            }
            if(itemStack != null)
                itemStacks.add(itemStack);

        }
        return itemStacks;
    }

    /**
     * This method will translate color codes (e.g. &a) in the specified message. It will also translate hex color codes (e.g. &#123456).
     *
     * @param message the message which should have colour codes translated
     * @return the translated string
     */
    public static final String colorize(final String message) {
        return ChatColor.translateAlternateColorCodes('&', translateHexColorCodes("&#", "", message));
    }
    
    /**
     * This method will translate HEX colour codes (e.g. 123456) in the specified message.
     * Full credit to Sullivan_Bognar and imDaniX on SpigotMC for creating this method.
     *
     * @param startTag what the tag should begin with - '&#' is recommended
     * @param endTag   what the tag should end with - '' (nothing) is recommended
     * @param message  the message that should be translated
     * @return the translated string
     */
    public static final String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}
