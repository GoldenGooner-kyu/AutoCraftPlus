package com.github.goldengooner.autocraftplus.manager;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final AutoCraftPlus plugin;
    private FileConfiguration config;

    private boolean triggerOnPickup;
    private boolean triggerOnTick;
    private boolean enableAutoPickup;
    private int defaultTickSpeed;

    private String prefix;
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(AutoCraftPlus plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reloads configuration from disk and updates local caches.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Load Settings
        this.triggerOnPickup = config.getBoolean("settings.trigger-on-pickup", true);
        this.triggerOnTick = config.getBoolean("settings.trigger-on-tick", true);
        this.enableAutoPickup = config.getBoolean("settings.enable-auto-pickup", true);
        this.defaultTickSpeed = config.getInt("settings.default-tick-speed", 20);

        // Load Messages
        this.prefix = ColorUtil.translate(config.getString("messages.prefix", "&7[&bAutoCraftPlus&7] "));
        this.messages.clear();
        
        if (config.getConfigurationSection("messages") != null) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                if (key.equals("prefix")) continue;
                String value = config.getString("messages." + key, "");
                this.messages.put(key, ColorUtil.translate(value));
            }
        }
    }

    public boolean isTriggerOnPickup() {
        return triggerOnPickup;
    }

    public boolean isTriggerOnTick() {
        return triggerOnTick;
    }

    public boolean isEnableAutoPickup() {
        return enableAutoPickup;
    }

    public int getDefaultTickSpeed() {
        return defaultTickSpeed;
    }

    /**
     * Gets a message from config, translates placeholders and prepends prefix if requested.
     *
     * @param key          the message key in config
     * @param placeholders alternating placeholder keys and values (e.g., "%id%", "sword_epic")
     * @return the formatted, colored message
     */
    public String getMessage(String key, String... placeholders) {
        String msg = messages.get(key);
        if (msg == null) {
            msg = config.getString("messages." + key);
            if (msg == null) {
                return ColorUtil.translate("&cMissing message: " + key);
            }
            msg = ColorUtil.translate(msg);
            messages.put(key, msg);
        }

        // Apply placeholders
        if (placeholders != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        return prefix + msg;
    }
}
