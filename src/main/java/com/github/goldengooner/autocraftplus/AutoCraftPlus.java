package com.github.goldengooner.autocraftplus;

import com.github.goldengooner.autocraftplus.command.AutoCraftCommand;
import com.github.goldengooner.autocraftplus.engine.AutoCraftEngine;
import com.github.goldengooner.autocraftplus.gui.RecipeGui;
import com.github.goldengooner.autocraftplus.listener.GuiListener;
import com.github.goldengooner.autocraftplus.manager.ConfigManager;
import com.github.goldengooner.autocraftplus.manager.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoCraftPlus extends JavaPlugin {

    private ConfigManager configManager;
    private RecipeManager recipeManager;
    private AutoCraftEngine autoCraftEngine;
    private GuiListener guiListener;
    private com.github.goldengooner.autocraftplus.listener.BlockBreakListener blockBreakListener;

    @Override
    public void onEnable() {
        // 1. Load configuration and recipes
        this.configManager = new ConfigManager(this);
        this.recipeManager = new RecipeManager(this);

        // 2. Load the auto-crafting engine
        this.autoCraftEngine = new AutoCraftEngine(this);

        // 3. Register GUI events listener
        this.guiListener = new GuiListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // 3.5. Register BlockBreak AutoPickup listener
        this.blockBreakListener = new com.github.goldengooner.autocraftplus.listener.BlockBreakListener(this);

        // 4. Register commands & tab completer
        AutoCraftCommand commandExecutor = new AutoCraftCommand(this);
        getCommand("autocraft").setExecutor(commandExecutor);
        getCommand("autocraft").setTabCompleter(commandExecutor);

        // 5. Initialize bStats Metrics
        new org.bstats.bukkit.Metrics(this, 31724);

        getLogger().info("AutoCraftPlus has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Safe Close: Force close any active editing GUI sessions to return player items safely
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof RecipeGui) {
                player.closeInventory();
            }
        }

        getLogger().info("AutoCraftPlus has been successfully disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public AutoCraftEngine getAutoCraftEngine() {
        return autoCraftEngine;
    }
}
