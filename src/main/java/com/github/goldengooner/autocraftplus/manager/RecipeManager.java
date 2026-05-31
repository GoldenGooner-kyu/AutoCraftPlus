package com.github.goldengooner.autocraftplus.manager;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RecipeManager {

    private final AutoCraftPlus plugin;
    private final File recipeFile;
    private YamlConfiguration recipeConfig;
    private final Map<String, AutoRecipe> recipes = new HashMap<>();

    public RecipeManager(AutoCraftPlus plugin) {
        this.plugin = plugin;
        this.recipeFile = new File(plugin.getDataFolder(), "recipe.yml");
        reload();
    }

    /**
     * Reloads recipe data from recipe.yml.
     */
    public void reload() {
        if (!recipeFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                recipeFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create recipe.yml file!", e);
            }
        }

        recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
        recipes.clear();

        ConfigurationSection section = recipeConfig.getConfigurationSection("recipes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String path = "recipes." + key;
                List<?> rawIngredients = recipeConfig.getList(path + ".ingredients");
                List<?> rawResults = recipeConfig.getList(path + ".results");

                List<ItemStack> ingredients = new ArrayList<>();
                List<ItemStack> results = new ArrayList<>();

                if (rawIngredients != null) {
                    for (Object obj : rawIngredients) {
                        if (obj instanceof ItemStack) {
                            ingredients.add((ItemStack) obj);
                        }
                    }
                }

                if (rawResults != null) {
                    for (Object obj : rawResults) {
                        if (obj instanceof ItemStack) {
                            results.add((ItemStack) obj);
                        }
                    }
                }

                if (!ingredients.isEmpty() && !results.isEmpty()) {
                    recipes.put(key.toLowerCase(), new AutoRecipe(key, ingredients, results));
                }
            }
        }
    }

    /**
     * Saves a new recipe to the registry and recipe.yml.
     */
    public void saveRecipe(String id, List<ItemStack> ingredients, List<ItemStack> results) {
        String key = id.toLowerCase();
        
        // Remove nulls and air
        ingredients.removeIf(item -> item == null || item.getType().isAir());
        results.removeIf(item -> item == null || item.getType().isAir());

        AutoRecipe recipe = new AutoRecipe(id, ingredients, results);
        recipes.put(key, recipe);

        recipeConfig.set("recipes." + id + ".ingredients", ingredients);
        recipeConfig.set("recipes." + id + ".results", results);

        saveFile();
    }

    /**
     * Deletes a recipe from the registry and recipe.yml.
     */
    public boolean deleteRecipe(String id) {
        String key = id.toLowerCase();
        if (!recipes.containsKey(key)) {
            return false;
        }

        recipes.remove(key);
        recipeConfig.set("recipes." + id, null);
        saveFile();
        return true;
    }

    /**
     * Gets a recipe by its ID.
     */
    public AutoRecipe getRecipe(String id) {
        return recipes.get(id.toLowerCase());
    }

    /**
     * Gets all registered recipes.
     */
    public Collection<AutoRecipe> getRecipes() {
        return recipes.values();
    }

    private void saveFile() {
        try {
            recipeConfig.save(recipeFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipes to recipe.yml!", e);
        }
    }

    /**
     * Strictly compares two items using Bukkit's isSimilar, ensuring Material, 
     * ItemMeta, Display Name, Lore, Enchantments, CustomModelData, and NBT tags are identical.
     */
    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;
        return item1.isSimilar(item2);
    }
}
