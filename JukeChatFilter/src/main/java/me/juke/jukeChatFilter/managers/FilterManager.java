package me.juke.jukeChatFilter.managers;

import me.juke.jukeChatFilter.JukeChatFilter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class FilterManager {
    private final JukeChatFilter plugin;

    private final List<Pattern> blockedPatterns = new ArrayList<>();
    private final Set<String> whitelist = new HashSet<>();
    private final Map<UUID, Long> chatCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> volatileViolations = new ConcurrentHashMap<>();

    private final List<HistoryEntry> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private int maxPunishmentThreshold = 0;

    public FilterManager(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    public void loadConfigurations() {
        blockedPatterns.clear();
        whitelist.clear();
        chatCooldowns.clear();
        volatileViolations.clear();
        maxPunishmentThreshold = 0;

        List<String> blocked = plugin.getConfig().getStringList("blocked-words");
        for (String word : blocked) {
            String regex = Pattern.quote(word.toLowerCase());
            blockedPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }

        List<String> allowed = plugin.getConfig().getStringList("whitelisted-words");
        for (String word : allowed) {
            whitelist.add(word.toLowerCase());
        }

        var section = plugin.getConfig().getConfigurationSection("punishments.thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int val = Integer.parseInt(key);
                    if (val > maxPunishmentThreshold) {
                        maxPunishmentThreshold = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public String scanAndFindBlockedWord(String text) {
        if (!plugin.getConfig().getBoolean("enabled", true) || text == null || text.isEmpty()) return null;

        String cleanText = text.toLowerCase();
        for (String whiteWord : whitelist) {
            cleanText = cleanText.replace(whiteWord, "");
        }

        for (Pattern pattern : blockedPatterns) {
            var matcher = pattern.matcher(cleanText);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    public boolean checkCooldown(Player player) {
        if (!plugin.getConfig().getBoolean("anti-spam.enabled", true)) return true;
        if (player.hasPermission("jukechatfilter.bypass.cooldown")) return true;

        long now = System.currentTimeMillis();
        long lastChat = chatCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long delta = now - lastChat;
        long cooldownLimit = plugin.getConfig().getLong("anti-spam.cooldown-ms", 1500);

        if (delta < cooldownLimit) {
            double remainingSeconds = (cooldownLimit - delta) / 1000.0;
            String timeFormatted = String.format("%.1f", remainingSeconds);

            // Safe fallbacks to prevent empty/blank message processing
            String prefix = plugin.getConfig().getString("messages.prefix", "<red>[JukeFilter]</red> ");
            String textRaw = plugin.getConfig().getString("messages.cooldown-warning", "Please slow down! Wait %time%s.");
            String actionBarRaw = plugin.getConfig().getString("messages.cooldown-actionbar", "Anti-Spam Cooldown: %time%s");

            textRaw = textRaw.replace("%time%", timeFormatted);
            actionBarRaw = actionBarRaw.replace("%time%", timeFormatted);

            if (!textRaw.isEmpty()) {
                player.sendMessage(JukeChatFilter.MM.deserialize(prefix + textRaw));
            }
            if (!actionBarRaw.isEmpty()) {
                player.sendActionBar(JukeChatFilter.MM.deserialize(actionBarRaw));
            }

            return false;
        }

        chatCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    public void handleViolation(Player player, String detectedWord, String fullContext, String moduleType) {
        // Safe fallback configurations
        String prefix = plugin.getConfig().getString("messages.prefix", "<red>[JukeFilter]</red> ");
        String warning = plugin.getConfig().getString("messages.player-warning", "Your message was blocked.");
        String actionBar = plugin.getConfig().getString("messages.player-actionbar", "<red>Word Blocked!</red>");
        String staffAlertRaw = plugin.getConfig().getString("messages.staff-alert", "<red>[Alert]</red> %player% triggered %type% filter.");

        chatHistory.add(new HistoryEntry(player.getName(), moduleType, detectedWord, fullContext, System.currentTimeMillis()));
        if (chatHistory.size() > 100) {
            chatHistory.remove(0);
        }

        player.sendMessage(JukeChatFilter.MM.deserialize(prefix + warning));
        player.sendActionBar(JukeChatFilter.MM.deserialize(actionBar));

        if (plugin.getConfig().getBoolean("violation-sound.enabled", true)) {
            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("violation-sound.name", "BLOCK_NOTE_BLOCK_BASS"));
                float vol = (float) plugin.getConfig().getDouble("violation-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("violation-sound.pitch", 0.5);
                player.playSound(player.getLocation(), sound, vol, pitch);
            } catch (Exception ignored) {}
        }

        String formattedAlert = staffAlertRaw
                .replace("%player%", player.getName())
                .replace("%type%", moduleType)
                .replace("%word%", detectedWord)
                .replace("%message%", fullContext);
        Component staffAlertComponent = JukeChatFilter.MM.deserialize(formattedAlert);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("jukechatfilter.alerts") && plugin.getAlertingStaff().contains(online.getUniqueId())) {
                online.sendMessage(staffAlertComponent);
            }
        }

        boolean persistent = plugin.getConfig().getBoolean("punishments.persistent-violations", true);
        String uuidStr = player.getUniqueId().toString();

        if (persistent) {
            plugin.getDatabaseManager().incrementViolations(uuidStr).thenAccept(total ->
                    Bukkit.getScheduler().runTask(plugin, () -> evaluateThreshold(player, total))
            );
        } else {
            int current = volatileViolations.getOrDefault(player.getUniqueId(), 0) + 1;
            volatileViolations.put(player.getUniqueId(), current);
            evaluateThreshold(player, current);
        }
    }

    private void evaluateThreshold(Player player, int total) {
        int targetLookup = total;
        if (maxPunishmentThreshold > 0 && total > maxPunishmentThreshold) {
            targetLookup = maxPunishmentThreshold;
        }

        String commandRule = plugin.getConfig().getString("punishments.thresholds." + targetLookup);
        if (commandRule != null && !commandRule.isEmpty()) {
            String executableCmd = commandRule.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executableCmd);
        }
    }

    public List<HistoryEntry> getChatHistory() { return chatHistory; }
    public Map<UUID, Integer> getVolatileViolations() { return volatileViolations; }

    public record HistoryEntry(String playerName, String type, String word, String fullMessage, long timestamp) {}
}