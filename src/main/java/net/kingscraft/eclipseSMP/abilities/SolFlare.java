package net.kingscraft.eclipseSMP.abilities;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.environment.LightState;
import net.kingscraft.eclipseSMP.environment.SunlightDetector;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SolFlare implements Listener {

    private final EclipseSMP plugin;
    private final Map<UUID, Deque<Long>> presses = new ConcurrentHashMap<>();

    public SolFlare(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        Settings settings = plugin.getSettings();

        if (!settings.isSolFlareEnabled()) return;
        if (!settings.isWorldEnabled(player.getWorld().getName())) return;
        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (!profile.hasAllegiance() || profile.getAllegiance() != Allegiance.SOL) return;

        boolean eclipse = plugin.getEclipseManager().isActive();
        LightState state = eclipse ? LightState.ECLIPSE : SunlightDetector.resolve(player);
        if (state != LightState.SUNLIGHT && state != LightState.ECLIPSE) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (plugin.getCooldownManager().has(uuid, "flare")) {
            long remaining = plugin.getCooldownManager().remainingMillis(uuid, "flare") / 1000;
            plugin.getMessages().actionBar(player, "flare.ready", "&6☀ &7Solar Flare ready in &f{0}&7s", remaining);
            return;
        }

        Deque<Long> list = presses.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long window = settings.getSolFlareWindowMs();
        while (!list.isEmpty() && now - list.peekFirst() > window) {
            list.pollFirst();
        }

        boolean flared = false;
        Long last = list.peekLast();
        if (last != null) {
            long gap = now - last;
            if (gap >= settings.getSolFlareMicroCooldownMs() && gap <= window) {
                flared = true;
            }
        }
        list.addLast(now);

        if (flared) {
            list.clear();
            doFlare(player, settings);
        }
    }

    private void doFlare(Player player, Settings settings) {
        UUID uuid = player.getUniqueId();
        plugin.getCooldownManager().start(uuid, "flare", settings.getSolFlareCooldownSeconds() * 1000L);

        Location loc = player.getLocation();
        double radius = settings.getSolFlareRadius();
        Collection<LivingEntity> targets = loc.getWorld().getNearbyEntitiesByType(
                LivingEntity.class, loc, radius, radius, radius,
                e -> !e.equals(player) && !(e instanceof ArmorStand));

        double damage = settings.getSolFlareDamage();
        for (LivingEntity target : targets) {
            target.setFireTicks(Math.max(settings.getSolFlareFireTicks(), target.getFireTicks()));
            if (damage > 0) {
                target.damage(damage, player);
            }
            Vector push = target.getLocation().toVector().subtract(loc.toVector());
            push.setY(0.4);
            if (push.lengthSquared() > 0.0001) {
                push.normalize().multiply(settings.getSolFlareKnockbackPower());
            }
            target.setVelocity(push);
        }

        player.getWorld().spawnParticle(Particle.FLAME, loc, 80, radius, 0.5, radius, 0.05);
        player.getWorld().spawnParticle(Particle.LAVA, loc, 40, radius, 0.5, radius, 0.02);
        player.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);

        plugin.getMessages().actionBar(player, "flare.fired",
                "&6☀ &7Solar Flare &fready in &7{0}s", settings.getSolFlareCooldownSeconds());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        presses.remove(event.getPlayer().getUniqueId());
    }
}
