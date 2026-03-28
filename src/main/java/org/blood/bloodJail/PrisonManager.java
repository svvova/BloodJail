package org.blood.bloodJail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrisonManager {

    private final BloodJail plugin;
    private final File storageFile;
    private final Map<UUID, PrisonRecord> records = new ConcurrentHashMap<>();

    public PrisonManager(BloodJail plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "prisoners.yml");
    }

    public void load() {
        records.clear();
        boolean migratedLegacyData = false;

        if (!storageFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = yaml.getConfigurationSection("prisoners");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                if (!section.contains("remainingMillis") && section.contains("releaseAt")) {
                    migratedLegacyData = true;
                }
                PrisonRecord record = PrisonRecord.readFrom(uuid, section);
                records.put(uuid, record);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID keys.
            }
        }

        if (migratedLegacyData) {
            save();
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for prisoners.yml");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PrisonRecord> entry : records.entrySet()) {
            ConfigurationSection section = yaml.createSection("prisoners." + entry.getKey());
            entry.getValue().writeTo(section);
        }

        try {
            yaml.save(storageFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save prisoners.yml: " + ex.getMessage());
        }
    }

    public PrisonRecord jailPlayer(OfflinePlayer target, String jailedBy, Duration duration, String reason) {
        Location arrestLocation = null;
        boolean captureOnJoin = true;

        if (target.isOnline() && target.getPlayer() != null) {
            arrestLocation = target.getPlayer().getLocation().clone();
            captureOnJoin = false;
        }

        PrisonRecord record = new PrisonRecord(
                target.getUniqueId(),
                target.getName() != null ? target.getName() : "unknown",
                jailedBy,
                reason,
                System.currentTimeMillis(),
                duration.toMillis(),
                arrestLocation,
                captureOnJoin
        );

        records.put(target.getUniqueId(), record);
        save();
        return record;
    }

    public PrisonRecord unjail(UUID uuid) {
        PrisonRecord removed = records.remove(uuid);
        if (removed != null) {
            save();
        }
        return removed;
    }

    public boolean isJailed(UUID uuid) {
        return records.containsKey(uuid);
    }

    public PrisonRecord getRecord(UUID uuid) {
        return records.get(uuid);
    }

    public PrisonRecord findByPlayerName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (PrisonRecord record : records.values()) {
            if (record.getPlayerName() != null && record.getPlayerName().toLowerCase(Locale.ROOT).equals(lower)) {
                return record;
            }
        }
        return null;
    }

    public OfflinePlayer findOfflineByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().toLowerCase(Locale.ROOT).equals(lower)) {
                return player;
            }
        }
        return null;
    }

    public Collection<PrisonRecord> getAllRecords() {
        return records.values();
    }
}

