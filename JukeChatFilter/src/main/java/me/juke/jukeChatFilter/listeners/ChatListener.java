package me.juke.jukeChatFilter.listeners;

import me.juke.jukeChatFilter.JukeChatFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatListener implements Listener {
    private final JukeChatFilter plugin;

    public ChatListener(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    // Set to LOWEST priority to intercept raw text before formatting engines like LPC change it
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (!plugin.getFilterManager().checkCooldown(player)) {
            e.setCancelled(true);
            return;
        }

        if (!plugin.getConfig().getBoolean("modules.chat.enabled", true)) return;
        if (player.hasPermission("jukechatfilter.bypass.chat")) return;

        String msg = e.getMessage();
        String flagged = plugin.getFilterManager().scanAndFindBlockedWord(msg);

        if (flagged != null) {
            e.setCancelled(true);
            e.setMessage(""); // Secondary fail-safe clean clear

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getFilterManager().handleViolation(player, flagged, msg, "Chat")
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("modules.commands.enabled", true)) return;
        Player player = e.getPlayer();
        if (player.hasPermission("jukechatfilter.bypass.commands")) return;

        String messageRaw = e.getMessage().substring(1);
        String[] parts = messageRaw.split(" ", 2);
        String commandRoot = parts[0].toLowerCase();

        var monitored = plugin.getConfig().getStringList("modules.commands.monitored-commands");
        if (!monitored.contains(commandRoot) || parts.length < 2) return;

        String argumentPayload = parts[1];
        String flagged = plugin.getFilterManager().scanAndFindBlockedWord(argumentPayload);

        if (flagged != null) {
            e.setCancelled(true);
            plugin.getFilterManager().handleViolation(player, flagged, e.getMessage(), "Command");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.getAlertingStaff().remove(e.getPlayer().getUniqueId());
        plugin.getFilterManager().getVolatileViolations().remove(e.getPlayer().getUniqueId());
    }
}