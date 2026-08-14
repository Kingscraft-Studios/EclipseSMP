package net.kingscraft.eclipseSMP.eclipse;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameRule;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class EclipseManager implements Listener {

    public static final int SURGE_DAMAGE = 0;
    public static final int SURGE_SPEED = 1;
    public static final int SURGE_REGEN = 2;

    private final EclipseSMP plugin;
    private final Random random = new Random();

    private EclipsePhase phase = EclipsePhase.IDLE;
    private long phaseStartedAt;
    private long phaseDurationMillis;
    private long nextNaturalAt;
    private long lastEclipseEndAt;

    private final Map<UUID, Integer> surges = new HashMap<>();
    private final Map<String, WorldBackup> worldBackups = new HashMap<>();

    private BossBar bar;
    private BukkitTask scheduleTask;
    private BukkitTask tickTask;
    private BukkitTask particleTask;

    private record WorldBackup(Boolean daylightCycle, long time, boolean storm, boolean thunder) {
    }

    public EclipseManager(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        createBar();
        scheduleNextNatural();
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateBar();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void shutdown() {
        if (phase == EclipsePhase.ACTIVE || phase == EclipsePhase.WARNING) {
            endEclipse();
        }
        cancelTask(scheduleTask);
        cancelTask(tickTask);
        cancelTask(particleTask);
        if (bar != null) bar.removeAll();
    }

    // ---- scheduling -------------------------------------------------
    private void scheduleNextNatural() {
        Settings settings = plugin.getSettings();
        long jitter = random.nextLong(0, settings.getEclipseJitterMillis() + 1);
        long delayMillis = settings.getEclipseIntervalMillis() + jitter;
        nextNaturalAt = System.currentTimeMillis() + delayMillis;
        long ticks = delayMillis / 1000 * 20;

        cancelTask(scheduleTask);
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (phase == EclipsePhase.IDLE || phase == EclipsePhase.COOLDOWN) {
                beginWarning(settings.getEclipseWarningSeconds());
            }
        }, Math.max(1, ticks));
    }

    /** Returns null on success, otherwise a reason message. */
    public String attemptTrigger(Player player) {
        Settings settings = plugin.getSettings();
        if (phase != EclipsePhase.IDLE && phase != EclipsePhase.COOLDOWN) {
            return plugin.getMessages().msg("trigger.underway", "&cThe Blood Eclipse is already underway.");
        }
        if (!settings.isWorldEnabled(player.getWorld().getName())) {
            return plugin.getMessages().msg("trigger.world-disabled", "&cEclipses cannot be triggered in this world.");
        }
        if (System.currentTimeMillis() - lastEclipseEndAt < settings.getEclipseTriggerCooldownMillis()) {
            long remaining = (settings.getEclipseTriggerCooldownMillis() - (System.currentTimeMillis() - lastEclipseEndAt)) / 60_000;
            return plugin.getMessages().msg("trigger.cooldown",
                    "&cThe Blood Eclipse is still cooling down. &7{0}m until it can be summoned again.",
                    Math.max(1, remaining));
        }
        beginWarning(settings.getEclipseTriggerWarningSeconds());
        return null;
    }

    /** Admin-forced trigger; returns null on success, otherwise a reason. */
    public String attemptAdminTrigger() {
        if (phase != EclipsePhase.IDLE && phase != EclipsePhase.COOLDOWN) {
            return plugin.getMessages().msg("trigger.underway", "&cAn eclipse is already underway.");
        }
        beginWarning(plugin.getSettings().getEclipseWarningSeconds());
        return null;
    }

    private void beginWarning(int seconds) {
        phase = EclipsePhase.WARNING;
        phaseStartedAt = System.currentTimeMillis();
        phaseDurationMillis = seconds * 1000L;
        cancelTask(scheduleTask);

        plugin.getMessages().broadcast("eclipse.warning.broadcast", "&4☀ &fThe sky begins to darken... &4☾");
        plugin.getMessages().titleAll(plugin.getSettings().getEclipseTitleWarning(),
                plugin.getMessages().msg("eclipse.warning.subtitle", "&7Find shelter or prepare to fight"));
        plugin.getWebhook().sendWarning(seconds);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (bar != null) bar.addPlayer(p);
        }

        long ticks = seconds * 20L;
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, this::beginActive, Math.max(1, ticks));
    }

    private void beginActive() {
        phase = EclipsePhase.ACTIVE;
        phaseStartedAt = System.currentTimeMillis();
        phaseDurationMillis = plugin.getSettings().getEclipseDurationSeconds() * 1000L;

        plugin.getShardManager().onEclipseStart();
        freezeWorlds();
        assignSurges();

        plugin.getMessages().broadcast("eclipse.active.broadcast",
                "&4☀☾ &cThe BLOOD ECLIPSE is here! &4☽☀ &7PvP now drops Eclipse Shards. Death is twice as costly.");
        plugin.getMessages().titleAll(plugin.getSettings().getEclipseTitleActive(),
                plugin.getMessages().msg("eclipse.active.subtitle", "&7☀ Shards drop on PvP kills ☾"));
        plugin.getWebhook().sendActive(plugin.getSettings().getEclipseDurationSeconds());

        startParticles();

        long ticks = phaseDurationMillis / 1000 * 20;
        cancelTask(scheduleTask);
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, this::endEclipse, Math.max(1, ticks));
    }

    public void endEclipse() {
        cancelTask(particleTask);
        restoreWorlds();
        plugin.getPowerManager().endEclipse();
        plugin.getShardManager().onEclipseEnd();

        long dropped = plugin.getShardManager().getDroppedThisEclipse();
        if (dropped > 0) {
            plugin.getMessages().broadcast("eclipse.ended.dropped",
                    "&a☀ &fThe Blood Eclipse has lifted. &d{0} &fEclipse Shards were claimed.", dropped);
        } else {
            plugin.getMessages().broadcast("eclipse.ended.none", "&a☀ &fThe Blood Eclipse has lifted.");
        }
        plugin.getMessages().titleAll(plugin.getSettings().getEclipseTitleEnd(),
                plugin.getMessages().msg("eclipse.ended.subtitle", "&7The sun and moon part ways"));
        plugin.getWebhook().sendEnded(dropped);

        surges.clear();
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }

        lastEclipseEndAt = System.currentTimeMillis();
        phase = EclipsePhase.COOLDOWN;
        phaseDurationMillis = 30_000L;
        phaseStartedAt = System.currentTimeMillis();

        if (!plugin.isEnabled()) {
            phase = EclipsePhase.IDLE;
            return;
        }

        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            phase = EclipsePhase.IDLE;
            if (plugin.getSettings().isEclipseEnabled()) {
                scheduleNextNatural();
            }
        }, 600L);
    }

    public void cancel() {
        if (phase != EclipsePhase.WARNING && phase != EclipsePhase.ACTIVE) return;
        cancelTask(scheduleTask);
        endEclipse();
    }

    // ---- debug helpers ----------------------------------------------
    /** Debug: jump straight to the active phase without a warning. */
    public String forceActive() {
        if (phase == EclipsePhase.ACTIVE) {
            return plugin.getMessages().msg("trigger.force-already", "&cEclipse is already active.");
        }
        if (phase == EclipsePhase.WARNING) {
            cancelTask(scheduleTask);
        }
        beginActive();
        return null;
    }

    /** Debug: tear everything down and return to IDLE immediately. */
    public void forceEnd() {
        if (phase == EclipsePhase.ACTIVE || phase == EclipsePhase.WARNING) {
            cancelTask(scheduleTask);
            endEclipse();
            cancelTask(scheduleTask);
        }
        phase = EclipsePhase.IDLE;
        if (plugin.getSettings().isEclipseEnabled()) {
            scheduleNextNatural();
        }
    }

    /** Debug: override a player's eclipse surge. */
    public void setSurge(UUID uuid, int surge) {
        surges.put(uuid, surge);
    }

    // ---- world handling ---------------------------------------------
    private void freezeWorlds() {
        Settings settings = plugin.getSettings();
        worldBackups.clear();
        for (String name : settings.getEnabledWorlds()) {
            World world = Bukkit.getWorld(name);
            if (world == null) continue;
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            Boolean daylight = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            boolean storm = world.hasStorm();
            boolean thunder = world.isThundering();
            worldBackups.put(name, new WorldBackup(daylight, world.getTime(), storm, thunder));

            try {
                if (settings.isFreezeTime()) {
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setTime(18000);
                }
                if (settings.isStorm()) {
                    world.setStorm(true);
                    world.setThundering(true);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to freeze world '" + name + "': " + ex.getMessage());
            }
        }
    }

    private void restoreWorlds() {
        for (Map.Entry<String, WorldBackup> entry : worldBackups.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null) continue;
            WorldBackup backup = entry.getValue();
            if (plugin.getSettings().isFreezeTime() && backup.daylightCycle() != null) {
                world.setTime(backup.time());
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, backup.daylightCycle());
            }
            if (plugin.getSettings().isStorm()) {
                world.setStorm(backup.storm());
                world.setThundering(backup.thunder());
            }
        }
        worldBackups.clear();
    }

    // ---- surges -----------------------------------------------------
    private void assignSurges() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getSettings().isWorldEnabled(p.getWorld().getName())) {
                surges.put(p.getUniqueId(), random.nextInt(3));
            }
        }
    }

    public int getSurge(UUID uuid) {
        return surges.getOrDefault(uuid, SURGE_DAMAGE);
    }

    // ---- particles --------------------------------------------------
    private void startParticles() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getSettings().isWorldEnabled(p.getWorld().getName())) continue;
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 0.5, 0),
                            8, 1.2, 1.2, 1.2, 0, new Particle.DustOptions(Color.RED, 1));
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // ---- boss bar ---------------------------------------------------
    private void createBar() {
        Settings settings = plugin.getSettings();
        bar = Bukkit.createBossBar(
                settings.getEclipseBarWarning(),
                settings.getEclipseBarColor(),
                settings.getEclipseBarStyle());
        bar.setVisible(false);
    }

    private void updateBar() {
        if (bar == null) return;
        long now = System.currentTimeMillis();
        if (phase == EclipsePhase.WARNING || phase == EclipsePhase.ACTIVE) {
            long remaining = phaseStartedAt + phaseDurationMillis - now;
            double progress = Math.max(0, Math.min(1, remaining / (double) phaseDurationMillis));
            bar.setTitle(phase == EclipsePhase.WARNING
                    ? plugin.getSettings().getEclipseBarWarning()
                    : plugin.getSettings().getEclipseBarActive());
            bar.setProgress(progress);
            bar.setVisible(true);
        } else {
            bar.setVisible(false);
        }
    }

    // ---- status -----------------------------------------------------
    public boolean isActive() {
        return phase == EclipsePhase.ACTIVE;
    }

    public boolean isWarning() {
        return phase == EclipsePhase.WARNING;
    }

    public EclipsePhase getPhase() {
        return phase;
    }

    public long getTimeUntilNextEclipseMillis() {
        return switch (phase) {
            case WARNING, ACTIVE -> phaseStartedAt + phaseDurationMillis - System.currentTimeMillis();
            default -> nextNaturalAt - System.currentTimeMillis();
        };
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (bar != null && phase != EclipsePhase.IDLE) {
            bar.addPlayer(player);
        }
        if (phase == EclipsePhase.ACTIVE && plugin.getSettings().isWorldEnabled(player.getWorld().getName())) {
            surges.put(player.getUniqueId(), random.nextInt(3));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        surges.remove(event.getPlayer().getUniqueId());
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) task.cancel();
    }
}
