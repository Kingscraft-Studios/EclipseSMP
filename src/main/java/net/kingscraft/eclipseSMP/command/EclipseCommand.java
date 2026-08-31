package net.kingscraft.eclipseSMP.command;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.eclipse.EclipsePhase;
import net.kingscraft.eclipseSMP.environment.LightState;
import net.kingscraft.eclipseSMP.environment.SunlightDetector;
import net.kingscraft.eclipseSMP.mace.MaceControl;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class EclipseCommand implements CommandExecutor, TabCompleter {

    private final EclipseSMP plugin;
    private final DebugCommands debugCommands;

    public EclipseCommand(EclipseSMP plugin) {
        this.plugin = plugin;
        this.debugCommands = new DebugCommands(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("top")) {
            return top(sender);
        }
        if (name.equals("deposit")) {
            return aliasDeposit(sender, args);
        }
        if (name.equals("withdraw")) {
            return aliasWithdraw(sender, args);
        }
        if (name.equals("recipes")) {
            if (!(sender instanceof Player player)) {
                tell(sender, "cmd.only-players-recipe", "&cOnly players can open the recipe book.");
                return true;
            }
            plugin.getRecipeBook().open(player);
            return true;
        }
        if (name.equals("grace")) {
            return grace(sender, args);
        }
        if (name.equals("end")) {
            return endTimer(sender, args);
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                tell(sender, "cmd.console-usage", "&7Use /eclipse <choose|status|shards|top|admin>");
                return true;
            }
            if (plugin.getProfileManager().get(player).hasAllegiance()) {
                plugin.getShardMenu().open(player);
            } else {
                plugin.getAllegianceGUI().open(player);
            }
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "choose" -> choose(sender);
            case "status" -> status(sender);
            case "shards" -> shards(sender, args);
            case "top" -> top(sender);
            case "admin" -> admin(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    // ---- aliases ----------------------------------------------------
    private boolean aliasDeposit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            tell(sender, "cmd.only-players", "&cOnly players can use this command.");
            return true;
        }
        return deposit(player, args.length >= 1 ? args[0] : null);
    }

    /** /grace [minutes|cancel] — disables PvP for a while. */
    private boolean grace(CommandSender sender, String[] args) {
        if (!sender.hasPermission("eclipse.smp.admin")) {
            tell(sender, "cmd.no-permission", "&cYou don't have permission.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            if (plugin.getSeasonManager().cancelGrace()) {
                plugin.getMessages().broadcast("grace.cancelled",
                        "&eGrace period cancelled. PvP is enabled.");
            } else {
                tell(sender, "grace.not-active", "&7No grace period is active.");
            }
            return true;
        }
        int minutes = plugin.getSettings().getSeasonGraceMinutes();
        if (args.length >= 1) {
            try {
                minutes = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                tell(sender, "cmd.invalid-amount", "&cInvalid amount.");
                return true;
            }
            if (minutes <= 0) {
                tell(sender, "cmd.invalid-amount", "&cInvalid amount.");
                return true;
            }
        }
        plugin.getSeasonManager().startGrace(minutes * 60_000L);
        return true;
    }

    /** /end [minutes] — schedules the opening of the End portals. */
    private boolean endTimer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("eclipse.smp.admin")) {
            tell(sender, "cmd.no-permission", "&cYou don't have permission.");
            return true;
        }
        if (!plugin.getSeasonManager().arePortalsLocked()) {
            tell(sender, "end.already-open", "&cThe End is already open.");
            return true;
        }
        int minutes = plugin.getSettings().getSeasonEndTimerMinutes();
        if (args.length >= 1) {
            try {
                minutes = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                tell(sender, "cmd.invalid-amount", "&cInvalid amount.");
                return true;
            }
            if (minutes <= 0) {
                tell(sender, "cmd.invalid-amount", "&cInvalid amount.");
                return true;
            }
        }
        plugin.getSeasonManager().scheduleEndOpening(minutes * 60_000L);
        return true;
    }

    private boolean aliasWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            tell(sender, "cmd.only-players", "&cOnly players can use this command.");
            return true;
        }
        return withdraw(player, args.length >= 1 ? args[0] : null);
    }

    // ---- player subcommands ------------------------------------------
    private boolean choose(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            tell(sender, "cmd.only-players", "&cOnly players can use this command.");
            return true;
        }
        plugin.getAllegianceGUI().open(player);
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            tell(sender, "cmd.only-players", "&cOnly players can use this command.");
            return true;
        }
        PlayerProfile profile = plugin.getProfileManager().get(player);
        EclipsePhase phase = plugin.getEclipseManager().getPhase();
        LightState state = plugin.getEclipseManager().isActive()
                ? LightState.ECLIPSE : SunlightDetector.resolve(player);

        String allegiance = profile.hasAllegiance()
                ? profile.getAllegiance().getSymbol() + " " + profile.getAllegiance().getDisplayName()
                : "&cNone";
        String phaseName = switch (phase) {
            case WARNING -> plugin.getMessages().msg("phase.status.warning", "&cWarning — eclipse incoming");
            case ACTIVE -> plugin.getMessages().msg("phase.status.active", "&4☀☾ BLOOD ECLIPSE ACTIVE");
            case COOLDOWN -> plugin.getMessages().msg("phase.cooldown", "&7Cooling down");
            default -> plugin.getMessages().msg("phase.waiting", "&aWaiting");
        };
        String next = (phase == EclipsePhase.WARNING || phase == EclipsePhase.ACTIVE)
                ? formatTime(plugin.getEclipseManager().getTimeUntilNextEclipseMillis())
                : plugin.getMessages().msg("time.unknown", "&c??");

        plugin.getMessages().send(player, "cmd.status.separator", "&7&m---------------------------------");
        plugin.getMessages().send(player, "cmd.status.header", "&6&lEclipse SMP &8- &7{0}", player.getName());
        plugin.getMessages().send(player, "cmd.status.allegiance", "&7Allegiance: &f{0}", allegiance);
        plugin.getMessages().send(player, "cmd.status.power", "&7Power state: &f{0}", state.getLabel());
        plugin.getMessages().send(player, "cmd.status.phase", "&7Eclipse phase: {0}", phaseName);
        plugin.getMessages().send(player, "cmd.status.next", "&7Next eclipse in: &f{0}", next);
        plugin.getMessages().send(player, "cmd.status.bank", "&7Shard bank: &d{0}", profile.getBank());
        plugin.getMessages().send(player, "cmd.status.earned", "&7Shards earned: &d{0}", profile.getTotalEarned());
        plugin.getMessages().send(player, "cmd.status.kills", "&7Kills: &c{0}", profile.getKills());
        plugin.getMessages().send(player, "cmd.status.separator", "&7&m---------------------------------");
        return true;
    }

    private boolean shards(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            tell(sender, "cmd.only-players", "&cOnly players can use this command.");
            return true;
        }
        PlayerProfile profile = plugin.getProfileManager().get(player);

        if (args.length < 2) {
            plugin.getMessages().send(player, "cmd.shards.help",
                    "&7You have &d{0} &7shards banked. &e/eclipse shards deposit [all|<n>]&7 | &e/eclipse shards withdraw <n>",
                    profile.getBank());
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "deposit" -> deposit(player, args.length >= 3 ? args[2] : null);
            case "withdraw" -> withdraw(player, args.length >= 3 ? args[2] : null);
            default -> {
                plugin.getMessages().send(player, "cmd.shards.usage",
                        "&cUsage: /eclipse shards <deposit|withdraw> [amount]");
                yield true;
            }
        };
    }

    private boolean deposit(Player player, String amount) {
        if (amount == null || amount.equalsIgnoreCase("all")) {
            int deposited = plugin.getShardManager().depositShards(player);
            plugin.getMessages().send(player, deposited > 0 ? "cmd.deposited" : "cmd.no-carried",
                    deposited > 0 ? "&aDeposited &d{0} Eclipse Shards&a." : "&cYou have no carried Eclipse Shards.",
                    deposited);
            return true;
        }
        int n;
        try {
            n = Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(player, "cmd.invalid-amount", "&cInvalid amount.");
            return true;
        }
        if (n <= 0) {
            plugin.getMessages().send(player, "cmd.invalid-amount", "&cInvalid amount.");
            return true;
        }
        int deposited = plugin.getShardManager().depositShards(player, n);
        plugin.getMessages().send(player, deposited > 0 ? "cmd.deposited" : "cmd.no-carried",
                deposited > 0 ? "&aDeposited &d{0} Eclipse Shards&a." : "&cYou have no carried Eclipse Shards.",
                deposited);
        return true;
    }

    private boolean withdraw(Player player, String amount) {
        int n = 1;
        if (amount != null) {
            try {
                n = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                plugin.getMessages().send(player, "cmd.invalid-amount", "&cInvalid amount.");
                return true;
            }
        }
        if (n <= 0) {
            plugin.getMessages().send(player, "cmd.invalid-amount", "&cInvalid amount.");
            return true;
        }
        if (plugin.getShardManager().withdrawShards(player, n)) {
            plugin.getMessages().send(player, "cmd.withdrew", "&aWithdrew &d{0} Eclipse Shards&a from your bank.", n);
        } else {
            plugin.getMessages().send(player, "cmd.not-enough", "&cYou don't have that many shards banked.");
        }
        return true;
    }

    private boolean top(CommandSender sender) {
        List<PlayerProfile> top = plugin.getProfileManager().getTop(10);
        if (top.isEmpty()) {
            tell(sender, "cmd.top.empty", "&7No shards have been collected yet. Wait for the Blood Eclipse!");
            return true;
        }
        tell(sender, "cmd.top.header", "&6&l☀☾ Top Eclipse Shard Collectors ☽☀");
        for (int i = 0; i < top.size(); i++) {
            PlayerProfile profile = top.get(i);
            String name = Bukkit.getOfflinePlayer(profile.getUuid()).getName();
            tell(sender, "cmd.top.entry", "&7{0}. &f{1} &7- &d{2} shards &7({3} kills)",
                    i + 1,
                    name == null ? profile.getUuid().toString().substring(0, 8) : name,
                    profile.getTotalEarned(), profile.getKills());
        }
        return true;
    }

    // ---- admin ------------------------------------------------------
    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("eclipse.smp.admin")) {
            tell(sender, "cmd.no-permission", "&cYou don't have permission.");
            return true;
        }
        if (args.length < 2) {
            tell(sender, "cmd.admin.header", "&6&lEclipse Admin Commands:");
            tell(sender, "cmd.admin.trigger", "&e/eclipse admin trigger &7- start the Blood Eclipse now");
            tell(sender, "cmd.admin.cancel", "&e/eclipse admin cancel &7- stop an ongoing eclipse");
            tell(sender, "cmd.admin.unban", "&e/eclipse admin unban <player> &7- end an elimination ban");
            tell(sender, "cmd.admin.mace", "&e/eclipse admin mace <show|n> &7- manage the Mace crafting budget");
            tell(sender, "cmd.admin.reload", "&e/eclipse admin reload &7- reload config");
            if (plugin.getSettings().isDebugEnabled()) {
                tell(sender, "cmd.admin.debug", "&e/eclipse admin debug &7- debug commands (debug.enabled)");
            }
            return true;
        }
        return switch (args[1].toLowerCase()) {
            case "trigger" -> trigger(sender);
            case "cancel" -> cancel(sender);
            case "unban" -> unban(sender, args.length >= 3 ? args[2] : null);
            case "mace" -> mace(sender, shift(args, 2));
            case "reload" -> reload(sender);
            case "debug" -> debug(sender, shift(args, 1));
            default -> {
                tell(sender, "cmd.admin.unknown",
                        "&cUnknown admin command. Use &e/eclipse admin trigger|cancel|unban|mace|reload{0}&c.",
                        plugin.getSettings().isDebugEnabled() ? "|debug" : "");
                yield true;
            }
        };
    }

    /** /eclipse admin mace [show|<n>] - inspect or adjust the Mace forging budget. */
    private boolean mace(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("show")) {
            maceStatus(sender);
            return true;
        }
        int delta;
        try {
            delta = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            tell(sender, "cmd.admin.mace.usage",
                    "&cUsage: /eclipse admin mace <show|n> &7(positive = mark as already forged, negative = refund budget)");
            return true;
        }
        plugin.getMaceControl().adjustUsed(delta);
        if (delta >= 0) {
            tell(sender, "cmd.admin.mace.added", "&aMarked &f{0} &aMace(s) as forged.", delta);
        } else {
            tell(sender, "cmd.admin.mace.refunded", "&aRefunded &f{0} &aMace(s) of budget.", -delta);
        }
        maceStatus(sender);
        return true;
    }

    private void maceStatus(CommandSender sender) {
        MaceControl control = plugin.getMaceControl();
        int used = control.usedCount();
        int budget = plugin.getSettings().getMaceMaxCrafted();
        String state = plugin.getSettings().isMaceControlEnabled() ? "&aenabled" : "&cdisabled";
        tell(sender, "cmd.admin.mace.header", "&6&l⚒ Mace Control &8(&f{0}&8)", state);
        tell(sender, "cmd.admin.mace.status",
                "&7Forged: &d{0}&7/&d{1} &8· &7Remaining: &d{2}",
                used, budget, Math.max(0, budget - used));
    }

    /** Ends an elimination ban early; mirrors what happens when the ban expires naturally. */
    private boolean unban(CommandSender sender, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            tell(sender, "cmd.admin.unban.usage", "&cUsage: /eclipse admin unban <player>");
            return true;
        }
        Player online = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(targetName);
        PlayerProfile profile = plugin.getProfileManager().getFresh(target.getUniqueId());

        int resetTo = plugin.getSettings().getEliminationResetTo();
        if (profile.getBannedUntil() <= 0 && profile.getBank() > plugin.getSettings().getEliminateAt()) {
            tell(sender, "cmd.admin.unban.not-banned", "&e{0} &7is not banned.", targetName);
            return true;
        }

        profile.setBannedUntil(0);
        profile.setBank(resetTo);
        plugin.getProfileManager().save(profile);
        tell(sender, "cmd.admin.unban.success",
                "&aUnbanned &f{0}&a. Their shard bank was reset to &d{1}&a.", targetName, resetTo);
        if (online != null) {
            plugin.getMessages().send(online, "elimination.ban-ended",
                    "&aYour elimination ban has ended. Your shard bank was reset to &d{0}&a.", resetTo);
        }
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (!plugin.getSettings().isDebugEnabled()) {
            tell(sender, "cmd.debug.disabled",
                    "&cDebug commands are disabled. Enable &e'debug.enabled' &cin config.yml.");
            return true;
        }
        if (!sender.hasPermission("eclipse.smp.admin")) {
            tell(sender, "cmd.no-permission", "&cYou don't have permission.");
            return true;
        }
        return debugCommands.handle(sender, args);
    }

    private boolean trigger(CommandSender sender) {
        if (plugin.getEclipseManager().getPhase() == EclipsePhase.ACTIVE
                || plugin.getEclipseManager().getPhase() == EclipsePhase.WARNING) {
            tell(sender, "cmd.eclipse-already", "&cAn eclipse is already underway.");
            return true;
        }
        String error = plugin.getEclipseManager().attemptAdminTrigger();
        if (error != null) {
            sender.sendMessage(plugin.getMessages().parse(error));
        }
        return true;
    }

    private boolean cancel(CommandSender sender) {
        plugin.getEclipseManager().cancel();
        tell(sender, "cmd.eclipse-cancelled", "&aEclipse cancelled.");
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getSettings().reload();
        plugin.getProfileManager().clearCache();
        tell(sender, "cmd.reloaded", "&aConfiguration reloaded.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        tell(sender, "cmd.help.header", "&6&lEclipse SMP Commands:");
        tell(sender, "cmd.help.usage", "&e/eclipse &7- open menu");
        tell(sender, "cmd.help.choose", "&e/eclipse choose &7- pick or switch allegiance");
        tell(sender, "cmd.help.status", "&e/eclipse status &7- your state & the eclipse phase");
        tell(sender, "cmd.help.shards", "&e/eclipse shards deposit|withdraw <n> &7- manage your bank");
        tell(sender, "cmd.help.top", "&e/eclipse top &7- shard leaderboard");
        tell(sender, "cmd.help.deposit", "&e/deposit [all|<n>] &7- deposit carried shards");
        tell(sender, "cmd.help.withdraw", "&e/withdraw [n] &7- withdraw shards from your bank");
        if (sender.hasPermission("eclipse.smp.admin")) {
            tell(sender, "cmd.help.admin", "&e/eclipse admin &7- admin controls");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("deposit") && args.length == 1) {
            return List.of("all").stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (name.equals("grace")) {
            return args.length == 1 && "cancel".toLowerCase().startsWith(args[0].toLowerCase())
                    ? List.of("cancel") : List.of();
        }
        if (name.equals("end")) {
            return List.of();
        }
        if (name.equals("top") || name.equals("withdraw") || name.equals("recipes")) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("choose", "status", "shards", "top", "admin"));
        } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("eclipse.smp.admin")) {
            if (args.length == 2) {
                completions.addAll(List.of("trigger", "cancel", "unban", "mace", "reload"));
                if (plugin.getSettings().isDebugEnabled()) {
                    completions.add("debug");
                }
            } else if (args.length >= 3 && args[1].equalsIgnoreCase("debug")
                    && plugin.getSettings().isDebugEnabled()) {
                completions.addAll(debugCommands.tabComplete(shift(args, 1)));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("unban")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("mace")) {
                completions.add("show");
            }
        } else if (args[0].equalsIgnoreCase("shards") && args.length == 2) {
            completions.addAll(List.of("deposit", "withdraw"));
        } else if (args[0].equalsIgnoreCase("shards")
                && args.length >= 3
                && args[1].equalsIgnoreCase("deposit")) {
            completions.add("all");
        }
        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }

    private static String[] shift(String[] args, int by) {
        int size = Math.max(0, args.length - by);
        String[] out = new String[size];
        System.arraycopy(args, by, out, 0, size);
        return out;
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

    private void tell(CommandSender sender, String legacy) {
        sender.sendMessage(plugin.getMessages().parse(legacy));
    }

    private void tell(CommandSender sender, String key, String fallback, Object... args) {
        sender.sendMessage(plugin.getMessages().parse(plugin.getMessages().msg(key, fallback, args)));
    }
}
