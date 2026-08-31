package net.kingscraft.eclipseSMP.command;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.eclipse.EclipseManager;
import net.kingscraft.eclipseSMP.eclipse.EclipsePhase;
import net.kingscraft.eclipseSMP.environment.LightState;
import net.kingscraft.eclipseSMP.environment.SunlightDetector;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class DebugCommands {

    private final EclipseSMP plugin;

    public DebugCommands(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            help(sender);
            return true;
        }
        return switch (args[1].toLowerCase()) {
            case "info" -> info(sender);
            case "light" -> light(sender, args);
            case "phase" -> phase(sender, args);
            case "surge" -> surge(sender, args);
            case "shards" -> shards(sender, args);
            case "time" -> time(sender, args);
            case "webhook" -> webhook(sender);
            case "profiles" -> profiles(sender);
            default -> {
                help(sender);
                yield true;
            }
        };
    }

    private void help(CommandSender sender) {
        tell(sender, "&6&lEclipse Debug Commands:");
        tell(sender, "&e/eclipse admin debug info &7- internal state dump");
        tell(sender, "&e/eclipse admin debug light [player] &7- light state resolution");
        tell(sender, "&e/eclipse admin debug phase <warning|active|cooldown|idle> &7- force phase");
        tell(sender, "&e/eclipse admin debug surge <damage|speed|regen> [player] &7- set surge");
        tell(sender, "&e/eclipse admin debug shards give <player> <n> &7- grant shards");
        tell(sender, "&e/eclipse admin debug shards set <player> <n> &7- set bank");
        tell(sender, "&e/eclipse admin debug time <ticks> &7- set world time");
        tell(sender, "&e/eclipse admin debug webhook &7- send test embed");
        tell(sender, "&e/eclipse admin debug profiles &7- dump player profiles");
    }

    private boolean info(CommandSender sender) {
        EclipseManager em = plugin.getEclipseManager();
        tell(sender, "&6&lEclipse Info:");
        tell(sender, "&7Phase: &f" + em.getPhase().name());
        tell(sender, "&7Next eclipse in: &f" + formatTime(em.getTimeUntilNextEclipseMillis()));
        if (em.getPhase() == EclipsePhase.ACTIVE) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                tell(sender, "&7  Surge " + p.getName() + ": &f" + surgeName(em.getSurge(p.getUniqueId())));
            }
        } else {
            tell(sender, "&7Surges: &7none (not active)");
        }
        tell(sender, "&7Enabled worlds: &f" + String.join(", ", plugin.getSettings().getEnabledWorlds()));
        return true;
    }

    private boolean light(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            tell(sender, "&cSpecify a player: &e/eclipse admin debug light [player]");
            return true;
        }
        World world = target.getWorld();
        long time = world.getTime();
        LightState state = plugin.getEclipseManager().isActive()
                ? LightState.ECLIPSE : SunlightDetector.resolve(target);

        tell(sender, "&6&lLight state for &f" + target.getName() + "&6:");
        tell(sender, "&7World: &f" + world.getName()
                + " &7(" + world.getEnvironment().name() + ") &7Time: &f" + time
                + " &7Skylight: &f" + target.getLocation().getBlock().getLightFromSky());
        tell(sender, "&7Resolved: " + state.getLabel());
        return true;
    }

    private boolean phase(CommandSender sender, String[] args) {
        if (args.length < 3) {
            tell(sender, "&cUsage: &e/eclipse admin debug phase <warning|active|cooldown|idle>");
            return true;
        }
        EclipseManager em = plugin.getEclipseManager();
        return switch (args[2].toLowerCase()) {
            case "warning" -> {
                String error = em.attemptAdminTrigger();
                tell(sender, error == null ? "&aForcing WARNING phase." : error);
                yield true;
            }
            case "active" -> {
                String error = em.forceActive();
                tell(sender, error == null ? "&aForcing ACTIVE phase." : error);
                yield true;
            }
            case "cooldown" -> {
                if (em.getPhase() == EclipsePhase.ACTIVE || em.getPhase() == EclipsePhase.WARNING) {
                    em.cancel();
                    tell(sender, "&aEclipse ended. Manager is in COOLDOWN (auto-expires to IDLE in ~30s).");
                } else {
                    tell(sender, "&7Manager is already " + em.getPhase().name() + ". Nothing to end.");
                }
                yield true;
            }
            case "idle" -> {
                em.forceEnd();
                tell(sender, "&aForced into IDLE phase.");
                yield true;
            }
            default -> {
                tell(sender, "&cUnknown phase. Use warning|active|cooldown|idle.");
                yield true;
            }
        };
    }

    private boolean surge(CommandSender sender, String[] args) {
        if (args.length < 3) {
            tell(sender, "&cUsage: &e/eclipse admin debug surge <damage|speed|regen> [player]");
            return true;
        }
        int surge = switch (args[2].toLowerCase()) {
            case "damage" -> EclipseManager.SURGE_DAMAGE;
            case "speed" -> EclipseManager.SURGE_SPEED;
            case "regen" -> EclipseManager.SURGE_REGEN;
            default -> -1;
        };
        if (surge < 0) {
            tell(sender, "&cUnknown surge. Use damage|speed|regen.");
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 4 ? args[3] : null);
        if (target == null) {
            tell(sender, "&cSpecify a player: &e/eclipse admin debug surge <type> [player]");
            return true;
        }
        plugin.getEclipseManager().setSurge(target.getUniqueId(), surge);
        tell(sender, "&aSet " + target.getName() + "'s surge to &f" + surgeName(surge)
                + "&a (active during the next eclipse).");
        return true;
    }

    private boolean shards(CommandSender sender, String[] args) {
        if (args.length < 5) {
            tell(sender, "&cUsage: &e/eclipse admin debug shards give <player> <n> &7| &e/eclipse admin debug shards set <player> <n>");
            return true;
        }
        String action = args[2].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            tell(sender, "&cPlayer &f" + args[3] + " &cis not online.");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            tell(sender, "&cInvalid amount: " + args[4]);
            return true;
        }
        if (amount < 0) {
            tell(sender, "&cAmount cannot be negative.");
            return true;
        }

        PlayerProfile profile = plugin.getProfileManager().get(target);
        if (action.equals("give")) {
            plugin.getShardManager().giveShards(target, amount);
            profile.addEarned(amount);
            plugin.getProfileManager().save(profile);
            tell(sender, "&aGave &f" + target.getName() + " &d" + amount + " Eclipse Shards&a.");
        } else if (action.equals("set")) {
            profile.addBank(amount - profile.getBank());
            plugin.getProfileManager().save(profile);
            tell(sender, "&aSet &f" + target.getName() + "&a's shard bank to &d" + amount + "&a.");
        } else {
            tell(sender, "&cUnknown shards action. Use give or set.");
        }
        return true;
    }

    private boolean time(CommandSender sender, String[] args) {
        if (args.length < 3) {
            tell(sender, "&cUsage: &e/eclipse admin debug time <ticks>");
            return true;
        }
        long ticks;
        try {
            ticks = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            tell(sender, "&cInvalid time: " + args[2]);
            return true;
        }
        for (String name : plugin.getSettings().getEnabledWorlds()) {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                world.setTime(ticks);
            }
        }
        tell(sender, "&aSet world time to &f" + ticks + " &aticks in all enabled worlds.");
        return true;
    }

    private boolean webhook(CommandSender sender) {
        plugin.getWebhook().sendTest();
        if (!plugin.getSettings().isWebhookEnabled()) {
            tell(sender, "&eWebhook is disabled in config; no embed was sent.");
        } else {
            tell(sender, "&aTest embed sent. Check your Discord channel.");
        }
        return true;
    }

    private boolean profiles(CommandSender sender) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            tell(sender, "&7No players online.");
            return true;
        }
        tell(sender, "&6&lOnline profiles:");
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getProfileManager().get(p);
            String a = profile.hasAllegiance()
                    ? profile.getAllegiance().getDisplayName() + " " + profile.getAllegiance().getSymbol()
                    : "&cNone";
            tell(sender, "&7  " + p.getName() + " | " + a
                    + " | bank &d" + profile.getBank()
                    + " | earned &d" + profile.getTotalEarned()
                    + " | kills &c" + profile.getKills()
                    + " | switches &7" + profile.getSwitches());
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name != null) {
            return Bukkit.getPlayerExact(name);
        }
        return sender instanceof Player p ? p : null;
    }

    public List<String> tabComplete(String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            list.addAll(List.of("info", "light", "phase", "surge", "shards", "time", "webhook", "profiles"));
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "phase" -> list.addAll(List.of("warning", "active", "cooldown", "idle"));
                case "surge" -> list.addAll(List.of("damage", "speed", "regen"));
                case "shards" -> list.addAll(List.of("give", "set"));
                case "light" -> Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            }
        } else if (args.length == 4) {
            if ((args[1].equalsIgnoreCase("shards") && (args[2].equalsIgnoreCase("give") || args[2].equalsIgnoreCase("set")))
                    || args[1].equalsIgnoreCase("surge")) {
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            }
        }
        return list.stream().filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }

    private void tell(CommandSender sender, String legacy) {
        sender.sendMessage(plugin.getMessages().parse(legacy));
    }

    private static String surgeName(int surge) {
        return switch (surge) {
            case EclipseManager.SURGE_DAMAGE -> "Damage";
            case EclipseManager.SURGE_SPEED -> "Speed";
            case EclipseManager.SURGE_REGEN -> "Regen";
            default -> "Unknown";
        };
    }

    private static String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
