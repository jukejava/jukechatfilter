package me.juke.jukeChatFilter.database;

import me.juke.jukeChatFilter.JukeChatFilter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class DatabaseManager {
    private final JukeChatFilter plugin;
    private Connection connection;

    public DatabaseManager(JukeChatFilter plugin) {
        this.plugin = plugin;
    }

    public synchronized void initializeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) return;

            File dbFile = new File(plugin.getDataFolder(), "storage.db");
            if (!dbFile.exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS filter_violations (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "violations INT DEFAULT 0" +
                            ");")) {
                ps.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage connection layer:", e);
        }
    }

    public synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error gracefully terminating connection metrics state:", e);
        }
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException ignored) {}
        return connection;
    }

    public CompletableFuture<Integer> getViolations(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT violations FROM filter_violations WHERE uuid = ?;";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("violations");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error performing dynamic violation database query:", e);
            }
            return 0;
        });
    }

    public CompletableFuture<Integer> incrementViolations(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String upsert = "INSERT INTO filter_violations(uuid, violations) VALUES(?, 1) " +
                    "ON CONFLICT(uuid) DO UPDATE SET violations = violations + 1;";
            String select = "SELECT violations FROM filter_violations WHERE uuid = ?;";

            try (PreparedStatement psUpsert = getConnection().prepareStatement(upsert);
                 PreparedStatement psSelect = getConnection().prepareStatement(select)) {

                psUpsert.setString(1, uuid);
                psUpsert.executeUpdate();

                psSelect.setString(1, uuid);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) return rs.getInt("violations");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error computing persistent data increment logic:", e);
            }
            return 0;
        });
    }

    public void clearViolations(String uuid) {
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM filter_violations WHERE uuid = ?;";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing row purge processing instructions:", e);
            }
        });
    }
}