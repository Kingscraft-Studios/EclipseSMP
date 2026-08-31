package net.kingscraft.eclipseSMP.season;

import net.kingscraft.eclipseSMP.EclipseSMP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Season-wide controls: the PvP grace period (/grace) and the End portal
 * opening timer (/end). Both timers live in save.yml so they survive restarts.
 */
public final class SeasonManager implements Listener {

    /** Grace countdown checkpoints (millis remaining) that get announced. */
    private static final long[] GRACE_CHECKPOINTS = {600_000L, 300_000L, 180_000L, 60_000L, 30_000L, 10_000L};

    private static final String KEY_GRACE_UNTIL = "grace-until";
    private static final String KEY_PORTAL_TIMER = "end-open-at";
    private static final String KEY_PORTALS_OPENED = "end-portals-opened";

    private final EclipseSMP plugin;
    private long graceUntil;
    private long portalOpenAt;
    private boolean portalsOpened;
    private int graceCheckpointIndex;
    private long lastEndMinuteBucket = -1;
    private long lastEndSecondAnnounced = -1;
    private BukkitTask tickTask;

    public SeasonManager(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        boolean startOpen = plugin.getSettings().isEndPortalsStartOpen();
        portalsOpened = startOpen || plugin.getSaveStore().getLong(KEY_PORTALS_OPENED, 0) == 1;
        portalOpenAt = Math.max(0L, plugin.getSaveStore().getLong(KEY_PORTAL_TIMER, 0));
        graceUntil = Math.max(0L, plugin.getSaveStore().getLong(KEY_GRACE_UNTIL, 0));
        graceCheckpointIndex = firstCheckpointAfter(graceUntil - System.currentTimeMillis());

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);

        // The server was down when the timer expired: open right away.
        if (!portalsOpened && portalOpenAt > 0 && portalOpenAt <= System.currentTimeMillis()) {
            openPortals();
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    // ---- grace ------------------------------------------------------

    public void startGrace(long durationMillis) {
        graceUntil = System.currentTimeMillis() + durationMillis;
        graceCheckpointIndex = firstCheckpointAfter(durationMillis);
        plugin.getSaveStore().set(KEY_GRACE_UNTIL, graceUntil);
        plugin.getMessages().broadcast("grace.started",
                "&a☀ &fPvP is disabled! &7Grace period active for &f{0}&7.", formatTime(durationMillis));
    }

    /** Ends an active grace period early. Returns false when none was running. */
    public boolean cancelGrace() {
        if (graceUntil <= 0) return false;
        graceUntil = 0;
        plugin.getSaveStore().set(KEY_GRACE_UNTIL, 0);
        return true;
    }

    public boolean isPvpBlocked() {
        return graceUntil > System.currentTimeMillis();
    }

    // ---- end portals ------------------------------------------------

    public boolean arePortalsLocked() {
        return !portalsOpened;
    }

    public void scheduleEndOpening(long delayMillis) {
        portalOpenAt = System.currentTimeMillis() + delayMillis;
        lastEndMinuteBucket = minuteBucket(delayMillis);
        lastEndSecondAnnounced = -1;
        plugin.getSaveStore().set(KEY_PORTAL_TIMER, portalOpenAt);
        plugin.getMessages().broadcast("end.timer-started",
                "&5☀ &fThe End Portals open in &f{0}&7!", formatTime(delayMillis));
    }

    public void openPortals() {
        portalsOpened = true;
        portalOpenAt = 0;
        plugin.getSaveStore().set(KEY_PORTALS_OPENED, 1);
        plugin.getSaveStore().set(KEY_PORTAL_TIMER, 0);

        plugin.getMessages().broadcast("end.opened",
                "&d☀☾ &fThe End Portals are OPEN! &5☽☀");
        plugin.getMessages().titleAll(
                plugin.getMessages().msg("end.opened", "&dThe End is OPEN!"),
                plugin.getMessages().msg("end.subtitle", "&7Claim the dragon's hoard"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        }
    }

    // ---- ticking ----------------------------------------------------

    private void tick() {
        long now = System.currentTimeMillis();

        if (graceUntil > 0) {
            if (now >= graceUntil) {
                graceUntil = 0;
                plugin.getSaveStore().set(KEY_GRACE_UNTIL, 0);
                plugin.getMessages().broadcast("grace.expired",
                        "&c⚔ &fGrace period over — PvP is now enabled!");
            } else {
                long remaining = graceUntil - now;
                while (graceCheckpointIndex < GRACE_CHECKPOINTS.length
                        && remaining <= GRACE_CHECKPOINTS[graceCheckpointIndex]) {
                    plugin.getMessages().broadcast("grace.broadcast",
                            "&7Grace period ends in &f{0}&7.",
                            formatTime(GRACE_CHECKPOINTS[graceCheckpointIndex]));
                    graceCheckpointIndex++;
                }
            }
        }

        if (!portalsOpened && portalOpenAt > 0) {
            long remaining = portalOpenAt - now;
            if (remaining <= 0) {
                openPortals();
                return;
            }
            long secondsLeft = (remaining + 999) / 1000;
            if (secondsLeft <= 10) {
                if (secondsLeft != lastEndSecondAnnounced) {
                    lastEndSecondAnnounced = secondsLeft;
                    plugin.getMessages().broadcast("end.countdown",
                            "&5☾ &fThe End opens in &f{0}&7", formatTime(remaining));
                }
            } else {
                long bucket = minuteBucket(remaining);
                if (bucket != lastEndMinuteBucket) {
                    lastEndMinuteBucket = bucket;
                    plugin.getMessages().broadcast("end.countdown",
                            "&5☾ &fThe End opens in &f{0}&7", formatTime(remaining));
                }
            }
        }
    }

    private static long minuteBucket(long millis) {
        return (millis + 59_999) / 60_000;
    }

    private static int firstCheckpointAfter(long millisRemaining) {
        int index = 0;
        while (index < GRACE_CHECKPOINTS.length && millisRemaining <= GRACE_CHECKPOINTS[index]) {
            index++;
        }
        return index;
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

    // ---- listeners --------------------------------------------------

    /** Blocks all player-vs-player damage while the grace period runs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!isPvpBlocked()) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || !(event.getEntity() instanceof Player victim)) return;
        if (attacker.equals(victim)) return;
        if (!plugin.getSettings().isWorldEnabled(victim.getWorld().getName())) return;

        event.setCancelled(true);
        plugin.getMessages().actionBar(attacker, "pvp.grace-blocked",
                "&cPvP is disabled during the grace period!");
    }

    /** Keeps players out of the End while it is sealed. */
    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerPortalEvent.TeleportCause.END_PORTAL) return;
        if (arePortalsLocked()) {
            event.setCancelled(true);
            plugin.getMessages().send(event.getPlayer(), "end.locked-portal",
                    "&cThe End is sealed! It opens in &f{0}&c.",
                    portalOpenAt > 0 ? formatTime(portalOpenAt - System.currentTimeMillis())
                            : plugin.getMessages().msg("time.unknown", "&c??"));
        }
    }

    /** Prevents inserting Eyes of Ender into frames while the End is sealed. */
    @EventHandler(ignoreCancelled = true)
    public void onFrameInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.END_PORTAL_FRAME) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;
        if (arePortalsLocked()) {
            event.setCancelled(true);
            plugin.getMessages().actionBar(event.getPlayer(), "end.locked-portal",
                    "&cThe End is sealed!");
        }
    }

    private static Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
