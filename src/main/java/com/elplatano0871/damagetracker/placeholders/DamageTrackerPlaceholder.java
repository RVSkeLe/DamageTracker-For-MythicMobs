package com.elplatano0871.damagetracker.placeholders;

import com.elplatano0871.damagetracker.DamageTracker;
import com.elplatano0871.damagetracker.managers.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Placeholder expansion for the DamageTracker plugin.
 */
public class DamageTrackerPlaceholder extends PlaceholderExpansion {
    private final DamageTracker plugin;
    private final DatabaseManager databaseManager;

    /**
     * Constructor for DamageTrackerPlaceholder.
     *
     * @param plugin The main plugin instance.
     */
    public DamageTrackerPlaceholder(DamageTracker plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Gets the identifier for this placeholder expansion.
     *
     * @return The identifier string.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "dt";
    }

    /**
     * Gets the author of this placeholder expansion.
     *
     * @return The author string.
     */
    @Override
    public @NotNull String getAuthor() {
        return "ElPlatano0871";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Handles placeholder requests.
     *
     * @param player The player for whom the placeholder is being requested.
     * @param identifier The identifier of the placeholder.
     * @return The value of the placeholder.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.startsWith("damagetop_")) {
            String bossName = identifier.substring("damagetop_".length());
            return databaseManager.getFormattedLeaderboard(bossName);
        }

        String[] parts = identifier.split("_", 2);
        if (parts.length != 2) return null;

        String category = parts[0];
        String type = parts[1];

        switch (category) {
            case "top" -> {
                Integer index = parseIndex(type);
                if (index == null) return null;

                List<Map.Entry<UUID, Double>> topThree = getTopEntries(3);
                if (index < 0 || index >= topThree.size()) return "N/A";

                Map.Entry<UUID, Double> entry = topThree.get(index);
                return type.endsWith("name")
                        ? Optional.ofNullable(Bukkit.getOfflinePlayer(entry.getKey()).getName()).orElse("Unknown")
                        : String.format("%.2f", entry.getValue());
            }

            case "player" -> {
                if (player == null) return null;
                UUID playerId = player.getUniqueId();
                Map<UUID, Double> totalDamageMap = getTotalDamageMap();

                return switch (type) {
                    case "damage" -> String.format("%.2f", totalDamageMap.getOrDefault(playerId, 0.0));
                    case "position" -> {
                        List<Map.Entry<UUID, Double>> topThree = getTopEntries(3);
                        int pos = 1;
                        for (Map.Entry<UUID, Double> entry : topThree) {
                            if (entry.getKey().equals(playerId)) {
                                yield String.valueOf(pos);
                            }
                            pos++;
                        }
                        yield "N/A";
                    }
                    default -> null;
                };
            }

            default -> {
                return null;
            }
        }
    }

    private Map<UUID, Double> getTotalDamageMap() {
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        plugin.getAllDamageData().values().forEach(damageMap ->
                damageMap.forEach((uuid, damage) ->
                        totalDamageMap.merge(uuid, damage, Double::sum)));
        return totalDamageMap;
    }

    private List<Map.Entry<UUID, Double>> getTopEntries(int limit) {
        return getTotalDamageMap().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    private Integer parseIndex(String type) {
        if (!(type.endsWith("name") || type.endsWith("damage"))) return null;
        try {
            return Integer.parseInt(type.replaceAll("[^0-9]", "")) - 1;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
