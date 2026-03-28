package org.blood.bloodJail;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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

    public PrisonRecord jailPlayer(Player target, String jailedBy, Duration duration, String reason) {
        long now = System.currentTimeMillis();
        PrisonRecord record = new PrisonRecord(
                target.getUniqueId(),
                target.getName(),
                jailedBy,
                reason,
                now,
                duration.toMillis(),
                target.getLocation().clone()
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
            if (record.getPlayerName().toLowerCase(Locale.ROOT).equals(lower)) {
                return record;
            }
        }
        return null;
    }

    public Collection<PrisonRecord> getAllRecords() {
        return records.values();
    }
}

