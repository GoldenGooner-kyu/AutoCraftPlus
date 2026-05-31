package com.github.goldengooner.autocraftplus.command;

import com.github.goldengooner.autocraftplus.AutoCraftPlus;
import com.github.goldengooner.autocraftplus.gui.RecipeGui;
import com.github.goldengooner.autocraftplus.gui.RecipeListGui;
import com.github.goldengooner.autocraftplus.model.AutoRecipe;
import com.github.goldengooner.autocraftplus.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoCraftCommand implements TabExecutor {

    private final AutoCraftPlus plugin;

    public AutoCraftCommand(AutoCraftPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("acp.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.getRecipeManager().reload();
                sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                break;

            case "create":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                    return true;
                }
                Player player = (Player) sender;
                if (!player.hasPermission("acp.admin")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.translate("&cUsage: /acp create <recipe_id>"));
                    return true;
                }
                String createId = args[1];
                if (!createId.matches("^[a-zA-Z0-9_-]+$")) {
                    player.sendMessage(ColorUtil.translate("&cInvalid ID! Use only alphanumeric characters, dashes, and underscores."));
                    return true;
                }
                
                // Open new recipe builder GUI
                RecipeGui createGui = new RecipeGui(createId, null);
                player.openInventory(createGui.getInventory());
                break;

            case "edit":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                    return true;
                }
                Player editPlayer = (Player) sender;
                if (!editPlayer.hasPermission("acp.admin")) {
                    editPlayer.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    editPlayer.sendMessage(ColorUtil.translate("&cUsage: /acp edit <recipe_id>"));
                    return true;
                }
                String editId = args[1];
                AutoRecipe existingRecipe = plugin.getRecipeManager().getRecipe(editId);
                if (existingRecipe == null) {
                    editPlayer.sendMessage(plugin.getConfigManager().getMessage("recipe-not-found", "%id%", editId));
                    return true;
                }

                // Open edit recipe GUI loaded with existing ingredients & results
                RecipeGui editGui = new RecipeGui(existingRecipe.getId(), existingRecipe);
                editPlayer.openInventory(editGui.getInventory());
                break;

            case "remove":
                if (!sender.hasPermission("acp.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.translate("&cUsage: /acp remove <recipe_id>"));
                    return true;
                }
                String removeId = args[1];
                boolean deleted = plugin.getRecipeManager().deleteRecipe(removeId);
                if (deleted) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("recipe-deleted", "%id%", removeId));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("recipe-not-found", "%id%", removeId));
                }
                break;

            case "list":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                    return true;
                }
                Player listPlayer = (Player) sender;
                if (!listPlayer.hasPermission("acp.use")) {
                    listPlayer.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                // Open active recipe list GUI
                RecipeListGui listGui = new RecipeListGui(plugin, listPlayer);
                listPlayer.openInventory(listGui.getInventory());
                break;

            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-args"));
                break;
        }

        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("<gradient:#00D2FF:#3A7BD5>&l&m========================================</gradient>"));
        sender.sendMessage(ColorUtil.translate("  &b&lAutoCraftPlus &7- v" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.translate("  &7Author: &f" + plugin.getDescription().getAuthors().get(0)));
        sender.sendMessage(ColorUtil.translate(""));
        sender.sendMessage(ColorUtil.translate("  &bCommands:"));
        sender.sendMessage(ColorUtil.translate("  &e/acp info &7- View plugin information"));
        sender.sendMessage(ColorUtil.translate("  &e/acp list &7- Open active auto-crafting recipes menu"));
        
        if (sender.hasPermission("acp.admin")) {
            sender.sendMessage(ColorUtil.translate("  &e/acp reload &7- Reload all configuration files"));
            sender.sendMessage(ColorUtil.translate("  &e/acp create <id> &7- Build a new auto-crafting recipe"));
            sender.sendMessage(ColorUtil.translate("  &e/acp edit <id> &7- Modify an existing auto-crafting recipe"));
            sender.sendMessage(ColorUtil.translate("  &e/acp remove <id> &7- Delete an existing auto-crafting recipe"));
        }
        sender.sendMessage(ColorUtil.translate("<gradient:#00D2FF:#3A7BD5>&l&m========================================</gradient>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("info");
            subCommands.add("list");
            
            if (sender.hasPermission("acp.admin")) {
                subCommands.add("reload");
                subCommands.add("create");
                subCommands.add("edit");
                subCommands.add("remove");
            }
            
            return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("remove"))) {
            if (sender.hasPermission("acp.admin")) {
                List<String> recipeIds = plugin.getRecipeManager().getRecipes().stream()
                        .map(AutoRecipe::getId)
                        .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[1], recipeIds, new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }
}
