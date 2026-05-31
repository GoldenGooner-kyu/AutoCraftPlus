package com.github.goldengooner.autocraftplus.listener;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.gui.RecipeGui;
import com.github.goldengooner.autocraftplus.gui.RecipeListGui;
import com.github.goldengooner.autocraftplus.manager.RecipeManager;
import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiListener implements Listener {

    private final AutoCraftPlus plugin;

    public GuiListener(AutoCraftPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeListGui) {
            event.setCancelled(true);
            
            RecipeListGui gui = (RecipeListGui) event.getInventory().getHolder();
            Player player = (Player) event.getWhoClicked();
            int rawSlot = event.getRawSlot();
            
            if (rawSlot >= 0 && rawSlot < 54) {
                if (rawSlot == 49) {
                    player.closeInventory();
                    return;
                }

                if (rawSlot == 45) { // Previous Page arrow
                    if (gui.getPage() > 0) {
                        RecipeListGui newGui = new RecipeListGui(plugin, player, gui.getPage() - 1);
                        player.openInventory(newGui.getInventory());
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                    return;
                }

                if (rawSlot == 53) { // Next Page arrow
                    List<AutoRecipe> allRecipes = new ArrayList<>(plugin.getRecipeManager().getRecipes());
                    if ((gui.getPage() + 1) * 45 < allRecipes.size()) {
                        RecipeListGui newGui = new RecipeListGui(plugin, player, gui.getPage() + 1);
                        player.openInventory(newGui.getInventory());
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                    return;
                }
                
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    NamespacedKey key = new NamespacedKey(plugin, "recipe_id");
                    org.bukkit.inventory.meta.ItemMeta meta = clickedItem.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String recipeId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        
                        // Check if admin is trying to delete the recipe via Right Click
                        if (event.isRightClick() && player.hasPermission("acp.admin")) {
                            boolean deleted = plugin.getRecipeManager().deleteRecipe(recipeId);
                            if (deleted) {
                                player.sendMessage(plugin.getConfigManager().getMessage("recipe-deleted", "%id%", recipeId));
                                // Refresh list GUI on current page
                                RecipeListGui newGui = new RecipeListGui(plugin, player, gui.getPage());
                                player.openInventory(newGui.getInventory());
                                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
                            }
                        }
                    }
                }
            }
            return;
        }

        if (!(event.getInventory().getHolder() instanceof RecipeGui)) {
            return;
        }

        RecipeGui gui = (RecipeGui) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();

        // If clicking inside the custom Recipe GUI (slots 0-53)
        if (rawSlot >= 0 && rawSlot < 54) {
            // 1. Block any interaction with divider slots
            if (gui.isDividerSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            // 2. Handle click on the Save Button
            if (gui.isSaveSlot(rawSlot)) {
                event.setCancelled(true);
                handleSave(player, gui);
                return;
            }

            // 3. Handle interactions on Ingredient and Result slots
            if (gui.isIngredientSlot(rawSlot) || gui.isResultSlot(rawSlot)) {
                ItemStack currentItem = event.getCurrentItem();
                boolean isVirtual = (currentItem != null && !currentItem.getType().isAir() && !gui.isPlayerPlaced(rawSlot));

                if (isVirtual) {
                    // It's a virtual recipe item!
                    if (event.isShiftClick()) {
                        event.setCancelled(true); // Do not let them shift-click virtual items into their inventory
                        return;
                    }

                    ItemStack cursor = event.getCursor();
                    if (cursor == null || cursor.getType().isAir()) {
                        // Player wants to clear/delete the virtual item
                        event.setCancelled(true);
                        gui.getInventory().setItem(rawSlot, null);
                        gui.setPlayerPlaced(rawSlot, false);
                    } else {
                        // Player wants to replace the virtual item with their cursor item
                        event.setCancelled(true);
                        ItemStack placed = cursor.clone();
                        
                        if (event.isRightClick()) {
                            placed.setAmount(1);
                            cursor.setAmount(cursor.getAmount() - 1);
                            if (cursor.getAmount() <= 0) {
                                player.setItemOnCursor(null);
                            } else {
                                player.setItemOnCursor(cursor);
                            }
                        } else {
                            player.setItemOnCursor(null);
                        }
                        
                        gui.getInventory().setItem(rawSlot, placed);
                        gui.setPlayerPlaced(rawSlot, true);
                    }
                    return;
                }
            }
        }

        // Run the 0-tick scanner to update the player-placed tracking states for any slot changes
        runDelayedScanner(player, gui);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeListGui) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof RecipeGui)) {
            return;
        }

        RecipeGui gui = (RecipeGui) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < 54) {
                // Cancel drag if it touches borders, save button, or virtual slots
                if (gui.isDividerSlot(slot) || gui.isSaveSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
                
                ItemStack item = gui.getInventory().getItem(slot);
                if (item != null && !item.getType().isAir() && !gui.isPlayerPlaced(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        runDelayedScanner(player, gui);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecipeGui)) {
            return;
        }

        RecipeGui gui = (RecipeGui) event.getInventory().getHolder();
        Player player = (Player) event.getPlayer();

        // Safely return all player-placed items to the player
        for (int i = 0; i < 54; i++) {
            if (gui.isPlayerPlaced(i)) {
                ItemStack item = gui.getInventory().getItem(i);
                if (item != null && !item.getType().isAir()) {
                    gui.getInventory().setItem(i, null);
                    gui.setPlayerPlaced(i, false);
                    
                    // Return item to player inventory, drop on ground if full
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    private void handleSave(Player player, RecipeGui gui) {
        List<ItemStack> ingredients = new ArrayList<>();
        List<ItemStack> results = new ArrayList<>();

        // Extract ingredients
        for (int i = 0; i <= 35; i++) {
            ItemStack item = gui.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                ingredients.add(item.clone());
            }
        }

        // Extract results
        for (int i = 45; i <= 53; i++) {
            ItemStack item = gui.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                results.add(item.clone());
            }
        }

        if (ingredients.isEmpty() || results.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-save-empty"));
            return;
        }

        // Save recipe through manager
        plugin.getRecipeManager().saveRecipe(gui.getRecipeId(), ingredients, results);
        player.sendMessage(plugin.getConfigManager().getMessage("recipe-saved", "%id%", gui.getRecipeId()));
        
        // Close inventory (will trigger close event and return any remaining player items)
        player.closeInventory();
    }

    private void runDelayedScanner(Player player, RecipeGui gui) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == gui) {
                for (int i = 0; i < 54; i++) {
                    ItemStack item = gui.getInventory().getItem(i);
                    if (item == null || item.getType().isAir()) {
                        gui.setPlayerPlaced(i, false);
                    } else if (gui.isIngredientSlot(i) || gui.isResultSlot(i)) {
                        // If it is not empty, and was not marked as virtual initially, it is player-placed
                        if (!gui.isPlayerPlaced(i) && !isVirtualOrigin(gui, i, item)) {
                            gui.setPlayerPlaced(i, true);
                        }
                    }
                }
            }
        });
    }

    private boolean isVirtualOrigin(RecipeGui gui, int slot, ItemStack item) {
        // If we are in edit mode, and the item matches the original recipe item in this slot, it's virtual
        if (!gui.isEdit()) {
            return false;
        }
        
        // Since we populated slot with existing recipe index in setupGui:
        // We can check if it is similar to the loaded recipe items.
        // But to make it even simpler, if gui.isPlayerPlaced(slot) is false, it's virtual.
        // The scanner only marks a slot as player-placed if it is NOT already marked.
        // So once marked virtual, it stays virtual until deleted or replaced.
        // Therefore, we just need to verify that we do not overwrite a virtual item's state.
        return false;
    }
}
