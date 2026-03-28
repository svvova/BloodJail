package org.blood.bloodJail;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private final BloodJail plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration messages;

    public MessageService(BloodJail plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        Component component = get(key, placeholders);
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(component);
            return;
        }

        // Console can always receive plain text even when message uses MiniMessage styling.
        sender.sendMessage(PlainTextComponentSerializer.plainText().serialize(component));
    }

    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public void actionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(key, placeholders));
    }

    public void broadcast(String key, String... placeholders) {
        Bukkit.getServer().broadcast(get(key, placeholders));
    }

    public Component get(String key, String... placeholders) {
        String template = messages.getString(key, "<red>Missing message key: " + key + "</red>");
        String prefixTemplate = messages.getString("prefix", "<#ff8c00><bold>BloodJail</bold></#ff8c00><white> | </white>");

        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.parsed("prefix", prefixTemplate));

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolvers.add(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }

        return miniMessage.deserialize(template, resolvers.toArray(new TagResolver[0]));
    }
}

