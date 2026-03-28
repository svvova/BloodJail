package org.blood.bloodJail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public class PrisonRecord {

    private final UUID playerId;
    private final String playerName;
    private final String jailedBy;
    private final String reason;
    private final long jailedAtMillis;
    private final long releaseAtMillis;
    private final Location arrestLocation;

    public PrisonRecord(UUID playerId, String playerName, String jailedBy, String reason,
                        long jailedAtMillis, long releaseAtMillis, Location arrestLocation) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.jailedBy = jailedBy;
        this.reason = reason;
        this.jailedAtMillis = jailedAtMillis;
        this.releaseAtMillis = releaseAtMillis;
        this.arrestLocation = arrestLocation == null ? null : arrestLocation.clone();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getJailedBy() {
        return jailedBy;
    }

    public String getReason() {
        return reason;
    }

    public long getJailedAtMillis() {
        return jailedAtMillis;
    }

    public long getReleaseAtMillis() {
        return releaseAtMillis;
    }

    public Location getArrestLocation() {
        return arrestLocation == null ? null : arrestLocation.clone();
    }

    public long getRemainingMillis(long nowMillis) {
        return Math.max(0L, releaseAtMillis - nowMillis);
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= releaseAtMillis;
    }

    public void writeTo(ConfigurationSection section) {
        section.set("playerName", playerName);
        section.set("jailedBy", jailedBy);
        section.set("reason", reason);
        section.set("jailedAt", jailedAtMillis);
        section.set("releaseAt", releaseAtMillis);

        if (arrestLocation != null && arrestLocation.getWorld() != null) {
            section.set("arrest.world", arrestLocation.getWorld().getName());
            section.set("arrest.x", arrestLocation.getX());
            section.set("arrest.y", arrestLocation.getY());
            section.set("arrest.z", arrestLocation.getZ());
            section.set("arrest.yaw", arrestLocation.getYaw());
            section.set("arrest.pitch", arrestLocation.getPitch());
        }
    }

    public static PrisonRecord readFrom(UUID playerId, ConfigurationSection section) {
        String playerName = section.getString("playerName", "unknown");
        String jailedBy = section.getString("jailedBy", "unknown");
        String reason = section.getString("reason", "no reason");
        long jailedAt = section.getLong("jailedAt", System.currentTimeMillis());
        long releaseAt = section.getLong("releaseAt", jailedAt);

        String worldName = section.getString("arrest.world");
        Location arrestLocation = null;
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = section.getDouble("arrest.x", 0.0D);
                double y = section.getDouble("arrest.y", 0.0D);
                double z = section.getDouble("arrest.z", 0.0D);
                float yaw = (float) section.getDouble("arrest.yaw", 0.0D);
                float pitch = (float) section.getDouble("arrest.pitch", 0.0D);
                arrestLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }

        return new PrisonRecord(playerId, playerName, jailedBy, reason, jailedAt, releaseAt, arrestLocation);
    }
}

