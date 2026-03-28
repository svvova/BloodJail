package org.blood.bloodJail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PrisonCommandHandler implements CommandExecutor, TabCompleter {

    private final BloodJail plugin;
    private final PrisonManager prisonManager;
    private final MessageService messages;

    public PrisonCommandHandler(BloodJail plugin, PrisonManager prisonManager) {
        this.plugin = plugin;
        this.prisonManager = prisonManager;
        this.messages = plugin.getMessageService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "prison":
                return handlePrison(sender, args);
            case "unprison":
                return handleUnprison(sender, args);
            case "checkprison":
                return handleCheckPrison(sender, args);
            case "setprison":
                return handleSetPrison(sender);
            default:
                return false;
        }
    }

    private boolean handlePrison(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloodjail.command.prison")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }

        if (args.length < 3) {
            messages.send(sender, "usage.prison");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-offline");
            return true;
        }

        if (prisonManager.isJailed(target.getUniqueId())) {
            messages.send(sender, "prison.already-jailed");
            return true;
        }

        Duration duration = TimeUtil.parseDuration(args[1]);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            messages.send(sender, "errors.invalid-time");
            return true;
        }

        Location jailLocation = plugin.getJailLocation();
        if (jailLocation == null) {
            messages.send(sender, "errors.jail-world-not-loaded");
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        String jailedBy = sender.getName();

        prisonManager.jailPlayer(target, jailedBy, duration, reason);
        target.teleport(jailLocation);

        String formatted = TimeUtil.formatCompactDuration(duration.toMillis());
        messages.broadcast("prison.broadcast", "player", target.getName(), "jailed-by", jailedBy, "time", formatted, "reason", reason);
        messages.send(target, "prison.personal", "time", formatted, "reason", reason);

        return true;
    }

    private boolean handleUnprison(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloodjail.command.unprison")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }

        if (args.length < 1) {
            messages.send(sender, "usage.unprison");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-offline");
            return true;
        }

        if (!prisonManager.isJailed(target.getUniqueId())) {
            messages.send(sender, "unprison.not-jailed");
            return true;
        }

        plugin.releasePlayer(target, "досрочно освобожден администратором " + sender.getName(), true);
        messages.send(sender, "unprison.done", "player", target.getName());
        return true;
    }

    private boolean handleCheckPrison(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloodjail.command.checkprison")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }

        if (args.length != 1) {
            messages.send(sender, "usage.checkprison");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer != null) {
            PrisonRecord onlineRecord = prisonManager.getRecord(targetPlayer.getUniqueId());
            if (onlineRecord == null) {
                messages.send(sender, "check.not-jailed", "player", targetPlayer.getName());
                return true;
            }
            sendRecordInfo(sender, onlineRecord);
            return true;
        }

        // Offline fallback by stored name when player is not currently online.
        PrisonRecord record = prisonManager.findByPlayerName(args[0]);
        if (record == null) {
            messages.send(sender, "check.not-jailed", "player", args[0]);
            return true;
        }

        sendRecordInfo(sender, record);
        return true;
    }

    private boolean handleSetPrison(CommandSender sender) {
        if (!sender.hasPermission("bloodjail.command.setprison")) {
            messages.send(sender, "errors.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            messages.send(sender, "setprison.only-player");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();

        plugin.getConfig().set("jail.world", location.getWorld() != null ? location.getWorld().getName() : "world");
        plugin.getConfig().set("jail.x", location.getX());
        plugin.getConfig().set("jail.y", location.getY());
        plugin.getConfig().set("jail.z", location.getZ());
        plugin.getConfig().set("jail.yaw", location.getYaw());
        plugin.getConfig().set("jail.pitch", location.getPitch());
        plugin.saveConfig();

        messages.send(sender, "setprison.done");
        return true;
    }

    private void sendRecordInfo(CommandSender sender, PrisonRecord record) {
        long remaining = record.getRemainingMillis(System.currentTimeMillis());
        messages.send(sender, "check.info.player", "player", record.getPlayerName());
        messages.send(sender, "check.info.jailed-by", "jailed-by", record.getJailedBy());
        messages.send(sender, "check.info.reason", "reason", record.getReason());
        if (remaining <= 0L) {
            messages.send(sender, "check.info.remaining-expired");
        } else {
            messages.send(sender, "check.info.remaining", "time", TimeUtil.formatCompactDuration(remaining));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && ("prison".equals(cmd) || "unprison".equals(cmd) || "checkprison".equals(cmd))) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if ("prison".equals(cmd) && args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            Collections.addAll(suggestions, "1m", "10m", "1h", "2d", "1h30m");
            String partial = args[1].toLowerCase(Locale.ROOT);
            return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

