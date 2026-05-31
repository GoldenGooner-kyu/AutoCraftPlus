package com.github.goldengooner.autocraftplus.engine;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.manager.RecipeManager;
import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;

public class AutoCraftEngine implements Listener {

    private final AutoCraftPlus plugin;
    private final Map<UUID, Long> lastProcessTick = new HashMap<>();
    private long tickCounter = 0;

    public AutoCraftEngine(AutoCraftPlus plugin) {
        this.plugin = plugin;
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Start Periodic Tick task (runs every tick)
        Bukkit.getScheduler().runTaskTimer(plugin, this::runTickCheck, 1L, 1L);
    }

    /**
     * Periodically scans online players and processes auto-crafting based on permissions and speeds.
     */
    private void runTickCheck() {
        tickCounter++;
        
        if (!plugin.getConfigManager().isTriggerOnTick()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            int speed = getPlayerTickSpeed(player);
            if (speed <= 0) continue; // Trigger is disabled or speed is invalid

            long lastTick = lastProcessTick.getOrDefault(player.getUniqueId(), 0L);
            if (tickCounter - lastTick >= speed) {
                attemptCraft(player);
                lastProcessTick.put(player.getUniqueId(), tickCounter);
            }
        }
    }

    /**
     * Determines the player's tick speed based on the acp.speed.<tick> permission.
     * Evaluates all permissions and picks the lowest tick value (fastest speed).
     * Falls back to the default config speed if no permission is found.
     */
    public int getPlayerTickSpeed(Player player) {
        int speed = -1;

        for (PermissionAttachmentInfo attachmentInfo : player.getEffectivePermissions()) {
            String perm = attachmentInfo.getPermission();
            if (perm.startsWith("acp.speed.") && attachmentInfo.getValue()) {
                String suffix = perm.substring("acp.speed.".length());
                try {
                    int value = Integer.parseInt(suffix);
                    if (value > 0) {
                        if (speed == -1 || value < speed) {
                            speed = value;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (speed == -1) {
            speed = plugin.getConfigManager().getDefaultTickSpeed();
        }

        return speed;
    }

    /**
     * On-Pickup Trigger: Runs 1 tick later to scan inventory after item pickup.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfigManager().isTriggerOnPickup()) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            // Run 1 tick later to allow item to enter inventory
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    attemptCraft(player);
                }
            });
        }
    }

    /**
     * Core Transactional Crafting Method.
     * Evaluates all recipes, simulates deductions/additions on a cloned storage array,
     * and applies the final state atomically to avoid item loss or duplication.
     */
    public void attemptCraft(Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        
        // 1. Create a deep clone of the player's storage inventory in-memory
        ItemStack[] dummyStorage = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            dummyStorage[i] = storage[i] != null ? storage[i].clone() : null;
        }

        boolean modified = false;

        // 2. Scan all recipes
        for (AutoRecipe recipe : plugin.getRecipeManager().getRecipes()) {
            // Check permission
            if (!player.hasPermission("acp.recipe." + recipe.getId().toLowerCase()) && 
                !player.hasPermission("acp.recipe.*")) {
                continue;
            }

            // 3. Repeatedly craft as many of this recipe as the materials support
            while (true) {
                if (simulateCraft(dummyStorage, recipe)) {
                    modified = true;
                } else {
                    break;
                }
            }
        }

        // 4. If transaction was successful, commit back to actual inventory in a single call
        if (modified) {
            player.getInventory().setStorageContents(dummyStorage);
        }
    }

    /**
     * Simulates a single craft execution on the dummy inventory storage.
     * Returns true if ingredients were present and results were fit successfully.
     */
    private boolean simulateCraft(ItemStack[] storage, AutoRecipe recipe) {
        // Group recipe ingredients by item similarity to handle duplicate ingredient definitions
        Map<ItemStack, Integer> requiredGrouped = new HashMap<>();
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.getType().isAir()) continue;

            ItemStack matchKey = null;
            for (ItemStack key : requiredGrouped.keySet()) {
                if (RecipeManager.isSimilar(key, ingredient)) {
                    matchKey = key;
                    break;
                }
            }

            if (matchKey != null) {
                requiredGrouped.put(matchKey, requiredGrouped.get(matchKey) + ingredient.getAmount());
            } else {
                requiredGrouped.put(ingredient.clone(), ingredient.getAmount());
            }
        }

        // Check if dummy inventory has sufficient quantities of each ingredient
        for (Map.Entry<ItemStack, Integer> entry : requiredGrouped.entrySet()) {
            ItemStack requiredItem = entry.getKey();
            int requiredAmount = entry.getValue();

            int totalAvailable = 0;
            for (ItemStack storageItem : storage) {
                if (RecipeManager.isSimilar(storageItem, requiredItem)) {
                    totalAvailable += storageItem.getAmount();
                }
            }

            if (totalAvailable < requiredAmount) {
                return false; // Insufficient materials
            }
        }

        // Deduct materials from dummy inventory
        for (Map.Entry<ItemStack, Integer> entry : requiredGrouped.entrySet()) {
            ItemStack requiredItem = entry.getKey();
            int toDeduct = entry.getValue();

            for (int i = 0; i < storage.length; i++) {
                if (toDeduct <= 0) break;

                ItemStack storageItem = storage[i];
                if (RecipeManager.isSimilar(storageItem, requiredItem)) {
                    int amount = storageItem.getAmount();
                    if (amount > toDeduct) {
                        storageItem.setAmount(amount - toDeduct);
                        toDeduct = 0;
                    } else {
                        toDeduct -= amount;
                        storage[i] = null;
                    }
                }
            }
        }

        // Add results to dummy inventory (if they fit)
        if (canFitResults(storage, recipe.getResults())) {
            return true;
        }

        return false;
    }

    /**
     * Checks if results fit in the storage contents and places them.
     */
    private boolean canFitResults(ItemStack[] storage, List<ItemStack> results) {
        ItemStack[] testStorage = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            testStorage[i] = storage[i] != null ? storage[i].clone() : null;
        }

        for (ItemStack result : results) {
            if (result == null || result.getType().isAir()) continue;
            ItemStack toAdd = result.clone();
            int remaining = toAdd.getAmount();

            // First pass: stack with similar items
            for (int i = 0; i < testStorage.length; i++) {
                if (remaining <= 0) break;
                ItemStack slotItem = testStorage[i];
                if (RecipeManager.isSimilar(slotItem, toAdd)) {
                    int maxStack = slotItem.getMaxStackSize();
                    int currentAmount = slotItem.getAmount();
                    if (currentAmount < maxStack) {
                        int canAdd = maxStack - currentAmount;
                        if (remaining > canAdd) {
                            slotItem.setAmount(maxStack);
                            remaining -= canAdd;
                        } else {
                            slotItem.setAmount(currentAmount + remaining);
                            remaining = 0;
                        }
                    }
                }
            }

            // Second pass: place in empty slots
            for (int i = 0; i < testStorage.length; i++) {
                if (remaining <= 0) break;
                if (testStorage[i] == null || testStorage[i].getType().isAir()) {
                    int maxStack = toAdd.getMaxStackSize();
                    ItemStack newSlot = toAdd.clone();
                    if (remaining > maxStack) {
                        newSlot.setAmount(maxStack);
                        testStorage[i] = newSlot;
                        remaining -= maxStack;
                    } else {
                        newSlot.setAmount(remaining);
                        testStorage[i] = newSlot;
                        remaining = 0;
                    }
                }
            }

            if (remaining > 0) {
                return false; // Inventory overflow
            }
        }

        // Commit additions back to storage
        System.arraycopy(testStorage, 0, storage, 0, storage.length);
        return true;
    }

    /**
     * Clears tracked player sessions when they disconnect.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    public void cleanupPlayer(UUID uuid) {
        lastProcessTick.remove(uuid);
    }
}
