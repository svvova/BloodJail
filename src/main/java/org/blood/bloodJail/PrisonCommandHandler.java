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

    public PrisonCommandHandler(BloodJail plugin, PrisonManager prisonManager) {
        this.plugin = plugin;
        this.prisonManager = prisonManager;
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
            default:
                return false;
        }
    }

    private boolean handlePrison(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloodjail.command.prison")) {
            sender.sendMessage("[BloodJail] You do not have permission.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("[BloodJail] Usage: /prison <player> <time> <reason>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("[BloodJail] Player is not online.");
            return true;
        }

        if (prisonManager.isJailed(target.getUniqueId())) {
            sender.sendMessage("[BloodJail] This player is already in jail.");
            return true;
        }

        Duration duration = TimeUtil.parseDuration(args[1]);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            sender.sendMessage("[BloodJail] Invalid time format. Example: 1m, 10m, 1h, 2d, 1h30m");
            return true;
        }

        Location jailLocation = plugin.getJailLocation();
        if (jailLocation == null) {
            sender.sendMessage("[BloodJail] Jail world is not loaded. Check config.yml.");
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        String jailedBy = sender.getName();

        prisonManager.jailPlayer(target, jailedBy, duration, reason);
        target.teleport(jailLocation);

        String formatted = TimeUtil.formatCompactDuration(duration.toMillis());
        Bukkit.broadcastMessage("[BloodJail] " + target.getName() + " was jailed by " + jailedBy
                + " for " + formatted + ". Reason: " + reason);
        target.sendMessage("[BloodJail] You are jailed for " + formatted + ". Reason: " + reason);

        return true;
    }

    private boolean handleUnprison(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloodjail.command.unprison")) {
            sender.sendMessage("[BloodJail] You do not have permission.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("[BloodJail] Usage: /unprison <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("[BloodJail] Player is not online.");
            return true;
        }

        if (!prisonManager.isJailed(target.getUniqueId())) {
            sender.sendMessage("[BloodJail] This player is not jailed.");
            return true;
        }

        plugin.releasePlayer(target, "manual release by " + sender.getName(), true);
        sender.sendMessage("[BloodJail] Player was released.");
        return true;
    }

    private boolean handleCheckPrison(CommandSender sender, String[] args) {
        Player targetPlayer = null;

        if (args.length == 0) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage("[BloodJail] Usage: /checkprison <player>");
                return true;
            }
        } else {
            if (!sender.hasPermission("bloodjail.command.checkprison.others")) {
                sender.sendMessage("[BloodJail] You do not have permission to check other players.");
                return true;
            }
            targetPlayer = Bukkit.getPlayerExact(args[0]);
        }

        if (targetPlayer != null) {
            PrisonRecord record = prisonManager.getRecord(targetPlayer.getUniqueId());
            if (record == null) {
                sender.sendMessage("[BloodJail] " + targetPlayer.getName() + " is not jailed.");
                return true;
            }
            sendRecordInfo(sender, record);
            return true;
        }

        // Offline fallback by stored name when player is not currently online.
        PrisonRecord record = prisonManager.findByPlayerName(args[0]);
        if (record == null) {
            sender.sendMessage("[BloodJail] Player is not jailed.");
            return true;
        }

        sendRecordInfo(sender, record);
        return true;
    }

    private void sendRecordInfo(CommandSender sender, PrisonRecord record) {
        long remaining = record.getRemainingMillis(System.currentTimeMillis());
        sender.sendMessage("[BloodJail] Player: " + record.getPlayerName());
        sender.sendMessage("[BloodJail] Jailed by: " + record.getJailedBy());
        sender.sendMessage("[BloodJail] Reason: " + record.getReason());
        if (remaining <= 0L) {
            sender.sendMessage("[BloodJail] Remaining: 0s (waiting for login to release)");
        } else {
            sender.sendMessage("[BloodJail] Remaining: " + TimeUtil.formatCompactDuration(remaining));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1) {
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

