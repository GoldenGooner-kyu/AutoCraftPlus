package com.github.goldengooner.autocraftplus.listener;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

public class BlockBreakListener implements Listener {

    private final AutoCraftPlus plugin;

    public BlockBreakListener(AutoCraftPlus plugin) {
        this.plugin = plugin;
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 1. Process Auto-Pickup if enabled in config.yml
        if (plugin.getConfigManager().isEnableAutoPickup()) {
            // AutoPickup is only active for players in Survival mode
            if (player.getGameMode() == GameMode.SURVIVAL) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                
                // Get correct drops based on active block, tool, enchantments (Fortune/Silk Touch), and player attributes
                Collection<ItemStack> drops = block.getDrops(tool, player);
                boolean addedAny = false;

                for (ItemStack drop : drops) {
                    if (drop == null || drop.getType().isAir()) continue;

                    // Add items directly to player storage inventory
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
                    
                    // Drop any overflow items that don't fit at block location
                    if (!overflow.isEmpty()) {
                        for (ItemStack overflowItem : overflow.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), overflowItem);
                        }
                    }
                    addedAny = true;
                }

                if (addedAny) {
                    // Turn off natural block drop items (XP and sounds still play naturally)
                    event.setDropItems(false);
                }
            }
        }

        // 2. Compatibility Integration:
        // Schedule a 1-tick delay scan. This captures inventory changes immediately 
        // after the block is broken, working flawlessly whether our built-in AutoPickup 
        // is active OR a third-party plugin handles auto-pickup.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getAutoCraftEngine().attemptCraft(player);
            }
        });
    }
}
