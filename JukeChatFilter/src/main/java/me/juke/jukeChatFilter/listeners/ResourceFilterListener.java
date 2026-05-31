package me.juke.jukeChatFilter.listeners;

import me.juke.jukeChatFilter.JukeChatFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ResourceFilterListener implements Listener {
    private final JukeChatFilter plugin;

    public ResourceFilterListener(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e) {
        if (!plugin.getConfig().getBoolean("modules.signs.enabled", true)) return;
        Player player = e.getPlayer();
        if (player.hasPermission("jukechatfilter.bypass.signs")) return;

        StringBuilder completeSignText = new StringBuilder();
        for (String line : e.getLines()) {
            if (line != null && !line.isEmpty()) {
                completeSignText.append(" ").append(line);
            }
        }

        String content = completeSignText.toString().trim();
        String flagged = plugin.getFilterManager().scanAndFindBlockedWord(content);

        if (flagged != null) {
            e.setCancelled(true);

            for (int i = 0; i < 4; i++) {
                e.setLine(i, "");
            }

            plugin.getFilterManager().handleViolation(player, flagged, content, "Sign");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent e) {
        if (!plugin.getConfig().getBoolean("modules.books.enabled", true)) return;
        Player player = e.getPlayer();
        if (player.hasPermission("jukechatfilter.bypass.books")) return;

        var meta = e.getNewBookMeta();
        StringBuilder bookPayload = new StringBuilder();
        if (meta.hasTitle()) bookPayload.append(meta.getTitle()).append(" ");
        for (var page : meta.pages()) {
            bookPayload.append(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(page)).append(" ");
        }

        String context = bookPayload.toString().trim();
        String flagged = plugin.getFilterManager().scanAndFindBlockedWord(context);

        if (flagged != null) {
            e.setCancelled(true);
            plugin.getFilterManager().handleViolation(player, flagged, context, "Book");
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        if (!plugin.getConfig().getBoolean("modules.anvils.enabled", true)) return;
        ItemStack result = e.getResult();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName()) return;

        String name = meta.getDisplayName();
        String flagged = plugin.getFilterManager().scanAndFindBlockedWord(name);

        if (flagged != null) {
            e.setResult(null);
            if (e.getView().getPlayer() instanceof Player p) {
                if (p.hasPermission("jukechatfilter.bypass.anvils")) return;
                plugin.getFilterManager().handleViolation(p, flagged, name, "Anvil");
            }
        }
    }
}