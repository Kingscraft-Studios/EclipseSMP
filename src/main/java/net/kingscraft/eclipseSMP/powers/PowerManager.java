package net.kingscraft.eclipseSMP.powers;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.abilities.Sunfire;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.eclipse.EclipseManager;
import net.kingscraft.eclipseSMP.environment.LightState;
import net.kingscraft.eclipseSMP.environment.SunlightDetector;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PowerManager implements Listener {

    private final EclipseSMP plugin;
    private final Settings settings;
    private final Map<UUID, Set<PotionEffectType>> applied = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public PowerManager(EclipseSMP plugin) {
        this.plugin = plugin;
        this.settings = plugin.getSettings();
        startTicker();
    }

    private void startTicker() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!settings.isWorldEnabled(p.getWorld().getName())) continue;
                    updatePlayer(p);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        clearAll();
    }

    public void endEclipse() {
        clearAll();
    }

    private void clearAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Set<PotionEffectType> types = applied.remove(p.getUniqueId());
            if (types == null) continue;
            for (PotionEffectType type : types) {
                p.removePotionEffect(type);
            }
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    // ---- tick -------------------------------------------------------
    private void updatePlayer(Player player) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        boolean eclipse = plugin.getEclipseManager().isActive();
        LightState state = resolveState(player);
        boolean fullSet = plugin.getShardManager().armorSetPieces(player) >= 4;
        int tierBonus = Math.min(4, plugin.getShardManager().armorTotalTier(player) / 5);

        List<PotionEffect> effects = new ArrayList<>();

        if (!profile.hasAllegiance()) {
            plugin.getMessages().actionBar(player, "power.no-allegiance", "&cChoose your allegiance: &e/eclipse choose");
        } else if (eclipse) {
            int surge = plugin.getEclipseManager().getSurge(player.getUniqueId());
            effects.addAll(EclipsePowers.effects(settings, player, surge));
            if (fullSet) {
                effects.add(new PotionEffect(PotionEffectType.REGENERATION, 45,
                        settings.getArmorSetRegenLevel() - 1
                                + Math.min(tierBonus, settings.getArmorSetRegenTierCap())));
                effects.add(new PotionEffect(PotionEffectType.SPEED, 45,
                        settings.getArmorSetSpeedLevel() - 1 + tierBonus));
            }
            String surgeName = switch (surge) {
                case EclipseManager.SURGE_DAMAGE -> plugin.getMessages().msg("surge.damage", "&4Damage Surge");
                case EclipseManager.SURGE_SPEED -> plugin.getMessages().msg("surge.speed", "&bSpeed Surge");
                default -> plugin.getMessages().msg("surge.regen", "&dRegen Surge");
            };
            plugin.getMessages().actionBar(player, "power.eclipse",
                    "&4☀☾ &cBlood Eclipse &7| {0} {1}",
                    profile.getAllegiance().getSymbol(), surgeName);
        } else if (profile.getAllegiance() == Allegiance.SOL) {
            if (!(state == LightState.DARKNESS && fullSet)) {
                effects.addAll(SolPowers.effects(settings, state));
            }
            if (fullSet && state == LightState.SUNLIGHT) {
                effects.add(new PotionEffect(PotionEffectType.REGENERATION, 45,
                        settings.getArmorSetRegenLevel() - 1
                                + Math.min(tierBonus, settings.getArmorSetRegenTierCap())));
                effects.add(new PotionEffect(PotionEffectType.SPEED, 45,
                        settings.getArmorSetSpeedLevel() - 1 + tierBonus));
                if (settings.getSolSetStrengthLevel() > 0) {
                    effects.add(new PotionEffect(PotionEffectType.STRENGTH, 45,
                            settings.getSolSetStrengthLevel() - 1));
                }
            }
            plugin.getMessages().actionBar(player, "power.sol", "&e☀ Sol &7| {0}", state.getLabel());
        } else {
            effects.addAll(LunaPowers.effects(settings, state));
            if (fullSet && state == LightState.DARKNESS) {
                effects.add(new PotionEffect(PotionEffectType.SPEED, 45,
                        settings.getLunaDarknessSpeedLevel() - 1 + tierBonus));
                if (settings.getLunaSetStrengthLevel() > 0) {
                    effects.add(new PotionEffect(PotionEffectType.STRENGTH, 45,
                            settings.getLunaSetStrengthLevel() - 1));
                }
            }
            plugin.getMessages().actionBar(player, "power.luna", "&8☾ Luna &7| {0}", state.getLabel());
        }

        applyEffects(player, effects);
    }

    private void applyEffects(Player player, List<PotionEffect> effects) {
        UUID uuid = player.getUniqueId();
        Set<PotionEffectType> wanted = new HashSet<>();
        for (PotionEffect effect : effects) {
            wanted.add(effect.getType());
            PotionEffect current = player.getPotionEffect(effect.getType());
            if (current == null || current.getAmplifier() < effect.getAmplifier() || current.getDuration() < 40) {
                player.addPotionEffect(effect);
            }
        }
        Set<PotionEffectType> previous = applied.get(uuid);
        if (previous != null) {
            for (PotionEffectType type : previous) {
                if (!wanted.contains(type)) {
                    player.removePotionEffect(type);
                }
            }
        }
        applied.put(uuid, wanted);
    }

    private LightState resolveState(Player player) {
        if (plugin.getEclipseManager().isActive()) return LightState.ECLIPSE;
        return SunlightDetector.resolve(player);
    }

    // ---- damage -----------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        boolean enabledWorld = settings.isWorldEnabled(event.getEntity().getWorld().getName());
        Player attacker = resolveAttacker(event.getDamager());

        if (attacker != null && enabledWorld) {
            PlayerProfile profile = plugin.getProfileManager().get(attacker);
            if (profile.hasAllegiance()) {
                LightState state = resolveState(attacker);
                applyOutgoing(event, attacker, profile.getAllegiance(), state);
            }
        }

        if (event.getEntity() instanceof Player victim && enabledWorld) {
            applyIncoming(event, victim);
        }
    }

    /** Resolves the attacking player, following projectile shots back to their shooter. */
    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void applyOutgoing(EntityDamageByEntityEvent event, Player attacker,
                               Allegiance allegiance, LightState state) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        ItemStack held = attacker.getInventory().getItemInMainHand();
        String kind = ShardItem.gearKind(held);
        String side = ShardItem.sideOf(held);

        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (event.getEntity() instanceof LivingEntity target && ShardItem.KIND_AXE.equals(kind)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        settings.getAxeSlownessTicks(), 0));
            }

            if (state == LightState.ECLIPSE) {
                double dmg = event.getDamage() * settings.getEclipseDamageMult();
                if (plugin.getEclipseManager().getSurge(attacker.getUniqueId()) == EclipseManager.SURGE_DAMAGE) {
                    dmg *= settings.getEclipseSurgeDamageMult();
                }
                event.setDamage(dmg);
                Sunfire.ignite(event.getEntity(), settings.getSolFireTicks() - 20);
            } else if (allegiance == Allegiance.SOL) {
                if (state == LightState.SUNLIGHT) {
                    double dmg = event.getDamage() * settings.getSolSunlightDamage();
                    Sunfire.ignite(event.getEntity(), settings.getSolFireTicks());
                    if (ShardItem.KIND_BLADE.equals(kind) && ShardItem.SIDE_SOL.equals(side)) {
                        dmg *= settings.getSolBladeSunlightMultiplier();
                        Sunfire.ignite(event.getEntity(), settings.getSolFireTicks() + 40);
                    } else if (ShardItem.KIND_AXE.equals(kind) && ShardItem.SIDE_SOL.equals(side)) {
                        dmg *= settings.getSolAxeSunlightMultiplier();
                    }
                    event.setDamage(dmg);
                } else if (state == LightState.DARKNESS) {
                    event.setDamage(event.getDamage() * settings.getSolDarknessDamage());
                }
            } else if (state == LightState.DARKNESS) {
                double dmg = event.getDamage() * settings.getLunaDarknessDamage();
                boolean crit = event.getEntity() instanceof Player victim && isBehind(attacker, victim);
                if (crit) {
                    dmg *= settings.getLunaCritMultiplier();
                }
                if (ShardItem.KIND_BLADE.equals(kind) && ShardItem.SIDE_LUNA.equals(side)) {
                    double heal = dmg * settings.getLunaLifestealPercent();
                    double max = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                    attacker.setHealth(Math.min(max, attacker.getHealth() + heal));
                    attacker.getWorld().spawnParticle(Particle.HEART,
                            attacker.getLocation().add(0, 1, 0), 4, 0.4, 0.4, 0.4, 0);
                }
                event.setDamage(dmg);
            }
        } else if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            double dmg = event.getDamage();
            if (state == LightState.ECLIPSE) {
                dmg *= settings.getEclipseDamageMult();
                if (plugin.getEclipseManager().getSurge(attacker.getUniqueId())
                        == EclipseManager.SURGE_DAMAGE) {
                    dmg *= settings.getEclipseSurgeDamageMult();
                }
            } else if (state == LightState.SUNLIGHT && allegiance == Allegiance.SOL) {
                dmg *= settings.getSolSunlightDamage();
            } else if (state == LightState.DARKNESS && allegiance == Allegiance.LUNA) {
                dmg *= settings.getLunaDarknessDamage();
            }
            if (ShardItem.KIND_BOW.equals(kind)) {
                if (ShardItem.SIDE_SOL.equals(side) && state == LightState.SUNLIGHT) {
                    dmg *= settings.getSolBowSunlightMultiplier();
                } else if (ShardItem.SIDE_LUNA.equals(side) && state == LightState.DARKNESS) {
                    dmg *= settings.getLunaBowDarknessMultiplier();
                }
                dmg += plugin.getShardManager().weaponDamageBonus(held);
            }
            event.setDamage(dmg);
        }
    }

    private void applyIncoming(EntityDamageByEntityEvent event, Player victim) {
        PlayerProfile profile = plugin.getProfileManager().get(victim);
        LightState state = resolveState(victim);
        boolean fullSet = plugin.getShardManager().armorSetPieces(victim) >= 4;

        if (profile.hasAllegiance()
                && profile.getAllegiance() == Allegiance.LUNA
                && state == LightState.SUNLIGHT
                && !fullSet) {
            event.setDamage(event.getDamage() * settings.getLunaSunlightIncoming());
        }

        if (state == LightState.DARKNESS || state == LightState.ECLIPSE) {
            double reduction = plugin.getShardManager().armorReduction(victim);
            if (reduction > 0) {
                event.setDamage(event.getDamage() * (1 - reduction));
            }
        }
    }

    private boolean isBehind(Player attacker, Player victim) {
        double maxAngle = settings.getLunaBackstabAngle();
        Vector toAttacker = attacker.getLocation().toVector()
                .subtract(victim.getLocation().toVector()).setY(0);
        if (toAttacker.lengthSquared() < 0.0001) return false;
        toAttacker.normalize();
        Vector facing = victim.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.0001) return false;
        facing.normalize();
        double dot = Math.max(-1, Math.min(1, facing.dot(toAttacker)));
        double angle = Math.toDegrees(Math.acos(dot));
        return angle > 180 - maxAngle;
    }

    // ---- eclipse bow ignite -----------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof LivingEntity target)) return;
        if (target instanceof ArmorStand) return;
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;

        ItemStack held = shooter.getInventory().getItemInMainHand();
        if (!ShardItem.KIND_BOW.equals(ShardItem.gearKind(held))) return;
        String side = ShardItem.sideOf(held);
        LightState state = resolveState(shooter);

        if (ShardItem.SIDE_SOL.equals(side)
                && (state == LightState.SUNLIGHT || state == LightState.ECLIPSE)) {
            target.setFireTicks(settings.getBowFireTicks());
        } else if (ShardItem.SIDE_LUNA.equals(side) && state == LightState.ECLIPSE) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    settings.getLunaBowEclipseSlownessTicks(),
                    settings.getLunaBowEclipseSlownessLevel() - 1));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Set<PotionEffectType> types = applied.remove(uuid);
        if (types != null) {
            for (PotionEffectType type : types) {
                player.removePotionEffect(type);
            }
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        plugin.getCooldownManager().removeAll(uuid);
    }
}
