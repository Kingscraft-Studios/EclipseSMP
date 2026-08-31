package net.kingscraft.eclipseSMP.allegiance;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ProfileManager implements Listener {

    private final EclipseSMP plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final File dataFolder;
    private BukkitTask autosaveTask;

    public ProfileManager(EclipseSMP plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "profiles");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /** Periodically persists cached profiles so a crash loses at most a few minutes of changes. */
    public void startAutosave() {
        autosaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            saveAll(new ArrayList<>(profiles.values()));
        }, 6000L, 6000L);
    }

    public void shutdown() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = get(player.getUniqueId());

        if (!profile.hasAllegiance()) {
            scheduleAllegiancePrompt(player);
        }
    }

    private void scheduleAllegiancePrompt(Player player) {
        UUID uuid = player.getUniqueId();
        int[] attempts = {0};
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || get(uuid).hasAllegiance() || attempts[0]++ >= 60) {
                if (holder[0] != null) holder[0].cancel();
                return;
            }
            if (!isAuthenticated(player)) {
                return;
            }
            plugin.getAllegianceGUI().open(player);
            holder[0].cancel();
        }, 40L, 20L);
    }

    /**
     * Checks whether an AuthMe player has logged in. Uses reflection so AuthMe
     * is never a hard dependency: when it is not installed, or the API differs,
     * this returns true (no gating).
     */
    private static boolean isAuthenticated(Player player) {
        try {
            Class<?> apiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Method isAuthenticated = apiClass.getMethod("isAuthenticated", Player.class);
            Object api = getInstance.invoke(null);
            if (api == null) {
                return false;
            }
            return (Boolean) isAuthenticated.invoke(api, player);
        } catch (Throwable ignored) {
            return true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PlayerProfile profile = profiles.remove(event.getPlayer().getUniqueId());
        if (profile != null) {
            save(profile);
        }
    }

    public PlayerProfile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::load);
    }

    public PlayerProfile get(Player player) {
        return get(player.getUniqueId());
    }

    /** Loads a profile straight from disk, bypassing (and evicting) the in-memory cache. */
    public PlayerProfile getFresh(UUID uuid) {
        profiles.remove(uuid);
        return get(uuid);
    }

    /** Saves online players' cached profiles, then drops the whole cache so manual file edits take effect. */
    public void clearCache() {
        for (UUID uuid : profiles.keySet()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                save(profiles.get(uuid));
            }
        }
        profiles.clear();
    }

    /**
     * Applies an allegiance selection/switch with the configured cost.
     * Returns null on success, otherwise an error message.
     */
    public String switchTo(Player player, Allegiance to) {
        PlayerProfile profile = get(player);
        if (profile.hasAllegiance() && profile.getAllegiance() == to) {
            return plugin.getMessages().msg("choose.already-follower",
                    "&eYou are already a follower of {0}.", to.getDisplayName());
        }

        boolean hadAllegiance = profile.hasAllegiance();
        if (hadAllegiance && profile.getSwitches() >= plugin.getSettings().getFreeSwitches()) {
            int cost = plugin.getSettings().getSwitchCost();
            if (!profile.removeBank(cost)) {
                return plugin.getMessages().msg("choose.insufficient",
                        "&cYou need &d{0} Eclipse Shards &cin your bank to switch allegiances.", cost);
            }
        }

        profile.setAllegiance(to);
        if (hadAllegiance) {
            profile.addSwitch();
        }
        save(profile);
        convertGear(player);

        plugin.getMessages().send(player, "choose.help.header",
                "&6☀☾ &7Quick tips for your new path:");
        plugin.getMessages().send(player, "choose.help.guide",
                " &e/eclipse &7- main menu with your &fPowers Guide");
        plugin.getMessages().send(player, "choose.help.status",
                " &e/eclipse status &7- your power state & eclipse timer");
        plugin.getMessages().send(player, "choose.help.choose",
                " &e/eclipse choose &7- switch allegiance anytime (1st free, then &d{0}&7 shards)",
                plugin.getSettings().getSwitchCost());
        plugin.getMessages().send(player, "choose.help.recipes",
                " &e/recipes &7- craft Eclipse gear (crafts to your allegiance)");
        plugin.getMessages().send(player, "choose.help.shards",
                " &e/eclipse shards deposit|withdraw &7- manage your shard bank");
        plugin.getMessages().send(player, "choose.help.aliases",
                " &e/deposit [all|n] &7· &e/withdraw [n] &7- quick bank shortcuts");
        return null;
    }

    /** Reforges every Eclipse gear item in the player's inventory to match their allegiance. */
    public void convertGear(Player player) {
        PlayerProfile profile = get(player);
        if (!profile.hasAllegiance()) return;
        String side = profile.getAllegiance() == Allegiance.SOL ? ShardItem.SIDE_SOL : ShardItem.SIDE_LUNA;
        boolean changed = false;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !ShardItem.isGear(item)) continue;
            if (side.equals(ShardItem.sideOf(item))) continue;
            inv.setItem(i, ShardItem.withSide(item, side));
            changed = true;
        }
        if (changed) {
            plugin.getMessages().send(player, "choose.reforged",
                    "&7Your Eclipse gear has been reforged to {0}.", profile.getAllegiance().getDisplayName());
        }
    }

    private PlayerProfile load(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerProfile profile = new PlayerProfile(uuid);
        if (file.exists()) {
            profile.load(YamlConfiguration.loadConfiguration(file));
        } else {
            profile.setBank(plugin.getSettings().getStartingBank());
        }
        return profile;
    }

    public void save(PlayerProfile profile) {
        File file = new File(dataFolder, profile.getUuid() + ".yml");
        File tmp = new File(dataFolder, profile.getUuid() + ".yml.tmp");
        YamlConfiguration yaml = new YamlConfiguration();
        profile.save(yaml);
        try {
            yaml.save(tmp);
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            tmp.delete();
            plugin.getLogger().warning("Could not save profile " + profile.getUuid() + ": " + e.getMessage());
        }
    }

    public void saveAll(List<PlayerProfile> list) {
        list.forEach(this::save);
    }

    public List<PlayerProfile> getTop(int amount) {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();
        return java.util.Arrays.stream(files)
                .map(f -> {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(f.getName().replace(".yml", ""));
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                    PlayerProfile p = load(uuid);
                    return p.hasAllegiance() ? p : null;
                })
                .filter(p -> p != null)
                .sorted(Comparator.comparingInt(PlayerProfile::getTotalEarned).reversed())
                .limit(amount)
                .collect(Collectors.toList());
    }
}
