package me.juke.jukeChatFilter;

import me.juke.jukeChatFilter.commands.FilterCommand;
import me.juke.jukeChatFilter.commands.HistoryCommand;
import me.juke.jukeChatFilter.database.DatabaseManager;
import me.juke.jukeChatFilter.listeners.ChatListener;
import me.juke.jukeChatFilter.listeners.ResourceFilterListener;
import me.juke.jukeChatFilter.managers.FilterManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class JukeChatFilter extends JavaPlugin {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    private static JukeChatFilter instance;

    private DatabaseManager databaseManager;
    private FilterManager filterManager;
    private final Set<UUID> alertingStaff = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize Core Architecture Components
        this.databaseManager = new DatabaseManager(this);
        this.filterManager = new FilterManager(this);

        this.databaseManager.initializeDatabase();
        this.filterManager.loadConfigurations();

        // Register Command Handlers
        var mainCmd = getCommand("jukechatfilter");
        if (mainCmd != null) {
            FilterCommand exec = new FilterCommand(this);
            mainCmd.setExecutor(exec);
            mainCmd.setTabCompleter(exec);
        }

        var historyCmd = getCommand("chathistory");
        if (historyCmd != null) {
            historyCmd.setExecutor(new HistoryCommand(this));
        }

        // Register Performance-Optimized Event Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new ResourceFilterListener(this), this);
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.closeConnection();
        }
    }

    public static JukeChatFilter getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FilterManager getFilterManager() { return filterManager; }
    public Set<UUID> getAlertingStaff() { return alertingStaff; }
}