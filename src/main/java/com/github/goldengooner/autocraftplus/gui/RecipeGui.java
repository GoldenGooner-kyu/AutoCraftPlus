package com.github.goldengooner.autocraftplus.gui;

import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import com.github.goldengooner.autocraftplus.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class RecipeGui implements InventoryHolder {

    private final String recipeId;
    private final boolean isEdit;
    private final Inventory inventory;
    
    // Tracks if each slot contains an item placed by the player during this session
    private final boolean[] isPlayerItem = new boolean[54];

    public RecipeGui(String recipeId, AutoRecipe existingRecipe) {
        this.recipeId = recipeId;
        this.isEdit = existingRecipe != null;

        String title = ColorUtil.translate(isEdit ? "&8AutoCraft - Edit: &9" + recipeId : "&8AutoCraft - Create: &9" + recipeId);
        this.inventory = Bukkit.createInventory(this, 54, title);

        setupGui(existingRecipe);
    }

    private void setupGui(AutoRecipe recipe) {
        // 1. Fill Divider Row (Row 5: slots 36 to 44)
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = grayPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(ColorUtil.translate("&7"));
            grayPane.setItemMeta(paneMeta);
        }

        for (int i = 36; i <= 44; i++) {
            if (i == 40) continue; // Slot 40 is the Save Button
            inventory.setItem(i, grayPane);
        }

        // 2. Setup Save Button (Slot 40)
        ItemStack saveButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(ColorUtil.translate("&a&lSAVE RECIPE"));
            saveMeta.setLore(Collections.singletonList(ColorUtil.translate("&7Click to save the recipe and exit.")));
            saveButton.setItemMeta(saveMeta);
        }
        inventory.setItem(40, saveButton);

        // 3. Pre-populate existing recipe data if in edit mode
        if (isEdit && recipe != null) {
            // Fill ingredients (Slots 0 to 35 max)
            int ingIndex = 0;
            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingIndex >= 36) break;
                if (ingredient != null) {
                    inventory.setItem(ingIndex, ingredient.clone());
                    // Since it's pre-loaded, it's a virtual item
                    isPlayerItem[ingIndex] = false;
                }
                ingIndex++;
            }

            // Fill results (Slots 45 to 53 max)
            int resIndex = 45;
            for (ItemStack result : recipe.getResults()) {
                if (resIndex >= 54) break;
                if (result != null) {
                    inventory.setItem(resIndex, result.clone());
                    // Since it's pre-loaded, it's a virtual item
                    isPlayerItem[resIndex] = false;
                }
                resIndex++;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public boolean isEdit() {
        return isEdit;
    }

    public boolean isIngredientSlot(int slot) {
        return slot >= 0 && slot <= 35;
    }

    public boolean isResultSlot(int slot) {
        return slot >= 45 && slot <= 53;
    }

    public boolean isDividerSlot(int slot) {
        return slot >= 36 && slot <= 44 && slot != 40;
    }

    public boolean isSaveSlot(int slot) {
        return slot == 40;
    }

    public boolean isPlayerPlaced(int slot) {
        return slot >= 0 && slot < 54 && isPlayerItem[slot];
    }

    public void setPlayerPlaced(int slot, boolean isPlayer) {
        if (slot >= 0 && slot < 54) {
            isPlayerItem[slot] = isPlayer;
        }
    }
}
