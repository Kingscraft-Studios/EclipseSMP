package net.kingscraft.eclipseSMP;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Messages {

    private final Settings settings;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public Messages(Settings settings) {
        this.settings = settings;
    }

    public String prefix() {
        return settings.getMessage("prefix", "&8[&cEclipse&8] &r");
    }

    public Component parse(String legacy) {
        return serializer.deserialize(Settings.color(legacy));
    }

    public String msg(String key, String fallback, Object... args) {
        String message = settings.getMessage(key, fallback);
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return message;
    }

    public void send(Player player, String legacy) {
        player.sendMessage(parse(prefix() + legacy));
    }

    public void send(Player player, String key, String fallback, Object... args) {
        player.sendMessage(parse(prefix() + msg(key, fallback, args)));
    }

    public void broadcast(String key, String fallback, Object... args) {
        Bukkit.broadcast(parse(prefix() + msg(key, fallback, args)));
    }

    public void title(Player player, String title, String subtitle, int in, int stay, int out) {
        player.sendTitle(Settings.color(title), Settings.color(subtitle), in, stay, out);
    }

    public void titleAll(String title, String subtitle) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            title(p, title, subtitle, 10, 80, 20);
        }
    }

    public void actionBar(Player player, String key, String fallback, Object... args) {
        player.sendActionBar(parse(msg(key, fallback, args)));
    }
}
