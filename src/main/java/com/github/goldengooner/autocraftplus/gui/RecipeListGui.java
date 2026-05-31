package com.github.goldengooner.autocraftplus.gui;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import com.github.goldengooner.autocraftplus.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeListGui implements InventoryHolder {

    private final AutoCraftPlus plugin;
    private final Inventory inventory;
    private final int page;

    public RecipeListGui(AutoCraftPlus plugin, Player player) {
        this(plugin, player, 0);
    }

    public RecipeListGui(AutoCraftPlus plugin, Player player, int page) {
        this.plugin = plugin;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.translate("&8Active Recipes - Page " + (page + 1)));

        setupGui(player);
    }

    private void setupGui(Player player) {
        List<AutoRecipe> allRecipes = new ArrayList<>(plugin.getRecipeManager().getRecipes());
        
        // 1. Populate recipes in slots 0 to 44 based on current page
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, allRecipes.size());
        NamespacedKey key = new NamespacedKey(plugin, "recipe_id");

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            AutoRecipe recipe = allRecipes.get(i);
            if (recipe.getResults().isEmpty()) continue;

            // Use first result item as the icon
            ItemStack icon = recipe.getResults().get(0).clone();
            ItemMeta meta = icon.getItemMeta();
            
            if (meta != null) {
                // Set custom display name
                meta.setDisplayName(ColorUtil.translate("&b&lRecipe: &e" + recipe.getId()));

                // Compile dynamic Lore list
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.translate("&8&m---------------------------------"));
                
                // Ingredients Section
                lore.add(ColorUtil.translate("&d&lIngredients Required:"));
                for (ItemStack ingredient : recipe.getIngredients()) {
                    lore.add(ColorUtil.translate("  &7- &e" + ingredient.getAmount() + "x &f" + getItemName(ingredient)));
                }
                
                lore.add("");

                // Results Section
                lore.add(ColorUtil.translate("&a&lResults Crafted:"));
                for (ItemStack result : recipe.getResults()) {
                    lore.add(ColorUtil.translate("  &7- &e" + result.getAmount() + "x &f" + getItemName(result)));
                }
                
                lore.add(ColorUtil.translate("&8&m---------------------------------"));

                // Admin instruction (if viewer has admin access)
                if (player.hasPermission("acp.admin")) {
                    lore.add(ColorUtil.translate("&c&l[Admin] Right-Click to Delete"));
                } else {
                    lore.add(ColorUtil.translate("&7You must have permission to craft this recipe."));
                }

                // Bind recipe ID into PersistentDataContainer
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, recipe.getId());
                
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }

            inventory.setItem(slotIndex++, icon);
        }

        // 2. Build bottom row divider panes (Row 6: slots 45-53)
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = grayPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(ColorUtil.translate("&7"));
            grayPane.setItemMeta(paneMeta);
        }
        for (int i = 45; i <= 53; i++) {
            if (i == 45 || i == 49 || i == 53) continue; // Skip navigation slots
            inventory.setItem(i, grayPane);
        }

        // 3. Close Button (Slot 49)
        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ColorUtil.translate("&c&lCLOSE MENU"));
            closeButton.setItemMeta(closeMeta);
        }
        inventory.setItem(49, closeButton);

        // 4. Previous Page Button (Slot 45)
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ColorUtil.translate("&a&l« Previous Page"));
                prevMeta.setLore(Collections.singletonList(ColorUtil.translate("&7Go to page &e" + page)));
                prevButton.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prevButton);
        } else {
            inventory.setItem(45, grayPane);
        }

        // 5. Next Page Button (Slot 53)
        if (endIndex < allRecipes.size()) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ColorUtil.translate("&a&lNext Page »"));
                nextMeta.setLore(Collections.singletonList(ColorUtil.translate("&7Go to page &e" + (page + 2))));
                nextButton.setItemMeta(nextMeta);
            }
            inventory.setItem(53, nextButton);
        } else {
            inventory.setItem(53, grayPane);
        }
    }

    /**
     * Translates an ItemStack's material into a beautiful Title Case string
     * or gets its customized display name if it exists.
     */
    public static String getItemName(ItemStack item) {
        if (item == null) return "";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }

        // Formats DIAMOND_SWORD -> Diamond Sword
        String materialName = item.getType().name().replace("_", " ").toLowerCase();
        String[] words = materialName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
