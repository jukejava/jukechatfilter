package me.juke.jukeChatFilter.commands;

import me.juke.jukeChatFilter.JukeChatFilter;
import me.juke.jukeChatFilter.managers.FilterManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class HistoryCommand implements CommandExecutor {
    private final JukeChatFilter plugin;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public HistoryCommand(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jukechatfilter.history")) {
            sender.sendMessage(JukeChatFilter.MM.deserialize(
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.no-permission", "")
            ));
            return true;
        }

        List<FilterManager.HistoryEntry> history = plugin.getFilterManager().getChatHistory();
        if (history.isEmpty()) {
            sender.sendMessage(JukeChatFilter.MM.deserialize("<gray>No violation activities logged inside this session container context yet.</gray>"));
            return true;
        }

        sender.sendMessage(JukeChatFilter.MM.deserialize("<gradient:#ff5555:#ffaa00><b>📋 Recent Filter Violations History:</b></gradient>"));

        synchronized (history) {
            // Displays last 10 session logs sequentially
            for (int i = Math.max(0, history.size() - 10); i < history.size(); i++) {
                FilterManager.HistoryEntry entry = history.get(i);
                String timeStr = sdf.format(new Date(entry.timestamp()));

                sender.sendMessage(JukeChatFilter.MM.deserialize(
                        "<dark_gray>[" + timeStr + "]</dark_gray> <gold>" + entry.playerName() + "</gold> " +
                                "<gray><i>(" + entry.type() + ")</i></gray>: Flagged: <red>" + entry.word() + "</red><newline>" +
                                "  <dark_gray>└ Text:</dark_gray> <white>" + entry.fullMessage() + "</white>"
                ));
            }
        }
        return true;
    }
}