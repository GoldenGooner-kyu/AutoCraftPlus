package com.github.goldengooner.autocraftplus.model;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class AutoRecipe {

    private final String id;
    private final List<ItemStack> ingredients;
    private final List<ItemStack> results;

    public AutoRecipe(String id, List<ItemStack> ingredients, List<ItemStack> results) {
        this.id = id;
        this.ingredients = ingredients;
        this.results = results;
    }

    public String getId() {
        return id;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public List<ItemStack> getResults() {
        return results;
    }
}
