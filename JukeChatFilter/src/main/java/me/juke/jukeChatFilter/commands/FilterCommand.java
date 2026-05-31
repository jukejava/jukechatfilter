package me.juke.jukeChatFilter.commands;

import me.juke.jukeChatFilter.JukeChatFilter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class FilterCommand implements CommandExecutor, TabCompleter {
    private final JukeChatFilter plugin;

    public FilterCommand(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("jukechatfilter.admin")) {
                sender.sendMessage(JukeChatFilter.MM.deserialize(prefix + plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getFilterManager().loadConfigurations();
            sender.sendMessage(JukeChatFilter.MM.deserialize(prefix + plugin.getConfig().getString("messages.config-reloaded", "")));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("alertstoggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This configuration modifier command must be executed by an active player node connection.");
                return true;
            }
            if (!player.hasPermission("jukechatfilter.alerts")) {
                player.sendMessage(JukeChatFilter.MM.deserialize(prefix + plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }

            UUID uuid = player.getUniqueId();
            if (plugin.getAlertingStaff().contains(uuid)) {
                plugin.getAlertingStaff().remove(uuid);
                player.sendMessage(JukeChatFilter.MM.deserialize(prefix + plugin.getConfig().getString("messages.alerts-disabled", "")));
            } else {
                plugin.getAlertingStaff().add(uuid);
                player.sendMessage(JukeChatFilter.MM.deserialize(prefix + plugin.getConfig().getString("messages.alerts-enabled", "")));
            }
            return true;
        }

        sender.sendMessage(JukeChatFilter.MM.deserialize("<gray>Usage: /jcf reload | /jcf alertstoggle</gray>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "alertstoggle").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}