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
    private long remainingMillis;
    private Location arrestLocation;
    private boolean captureArrestOnJoin;

    public PrisonRecord(UUID playerId, String playerName, String jailedBy, String reason,
                        long jailedAtMillis, long remainingMillis, Location arrestLocation,
                        boolean captureArrestOnJoin) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.jailedBy = jailedBy;
        this.reason = reason;
        this.jailedAtMillis = jailedAtMillis;
        this.remainingMillis = Math.max(0L, remainingMillis);
        this.arrestLocation = arrestLocation == null ? null : arrestLocation.clone();
        this.captureArrestOnJoin = captureArrestOnJoin;
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

    public long getRemainingMillisStored() {
        return remainingMillis;
    }

    public Location getArrestLocation() {
        return arrestLocation == null ? null : arrestLocation.clone();
    }

    public boolean shouldCaptureArrestOnJoin() {
        return captureArrestOnJoin;
    }

    public long getRemainingMillis(long ignoredNowMillis) {
        return Math.max(0L, remainingMillis);
    }

    public boolean isExpired(long ignoredNowMillis) {
        return remainingMillis <= 0L;
    }

    public long decreaseRemainingMillis(long millis) {
        if (millis <= 0L) {
            return remainingMillis;
        }

        remainingMillis = Math.max(0L, remainingMillis - millis);
        return remainingMillis;
    }

    public void setArrestLocation(Location location) {
        this.arrestLocation = location == null ? null : location.clone();
    }

    public void setCaptureArrestOnJoin(boolean captureArrestOnJoin) {
        this.captureArrestOnJoin = captureArrestOnJoin;
    }

    public void writeTo(ConfigurationSection section) {
        section.set("playerName", playerName);
        section.set("jailedBy", jailedBy);
        section.set("reason", reason);
        section.set("jailedAt", jailedAtMillis);
        section.set("remainingMillis", remainingMillis);
        section.set("captureArrestOnJoin", captureArrestOnJoin);

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
        String playerName = section.getString("playerName", "неизвестно");
        String jailedBy = section.getString("jailedBy", "неизвестно");
        String reason = section.getString("reason", "без причины");
        long jailedAt = section.getLong("jailedAt", System.currentTimeMillis());
        long remainingMillis;
        if (section.contains("remainingMillis")) {
            remainingMillis = Math.max(0L, section.getLong("remainingMillis", 0L));
        } else {
            long releaseAt = section.getLong("releaseAt", jailedAt);
            remainingMillis = Math.max(0L, releaseAt - System.currentTimeMillis());
        }

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

        boolean captureArrestOnJoin = section.getBoolean("captureArrestOnJoin", arrestLocation == null);
        return new PrisonRecord(playerId, playerName, jailedBy, reason, jailedAt, remainingMillis, arrestLocation,
                captureArrestOnJoin);
    }
}

