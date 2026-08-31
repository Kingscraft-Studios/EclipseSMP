package net.kingscraft.eclipseSMP;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Settings {

    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    // ---- worlds -----------------------------------------------------
    public Set<String> getEnabledWorlds() {
        return cfg.getStringList("enabled-worlds").stream().collect(Collectors.toSet());
    }

    public boolean isWorldEnabled(String worldName) {
        return getEnabledWorlds().contains(worldName);
    }

    // ---- eclipse ----------------------------------------------------
    public boolean isEclipseEnabled() {
        return cfg.getBoolean("eclipse.enabled", true);
    }

    public long getEclipseIntervalMillis() {
        return cfg.getLong("eclipse.interval-minutes", 180) * 60_000L;
    }

    public long getEclipseJitterMillis() {
        return cfg.getLong("eclipse.interval-jitter-minutes", 120) * 60_000L;
    }

    public int getEclipseWarningSeconds() {
        return cfg.getInt("eclipse.warning-seconds", 60);
    }

    public int getEclipseTriggerWarningSeconds() {
        return cfg.getInt("eclipse.trigger-warning-seconds", 15);
    }

    public int getEclipseDurationSeconds() {
        return cfg.getInt("eclipse.duration-seconds", 1800);
    }

    public long getEclipseTriggerCooldownMillis() {
        return cfg.getLong("eclipse.trigger-cooldown-minutes", 60) * 60_000L;
    }

    public boolean isFreezeTime() {
        return cfg.getBoolean("eclipse.freeze-time", true);
    }

    public boolean isStorm() {
        return cfg.getBoolean("eclipse.storm", true);
    }

    public boolean isNightVision() {
        return cfg.getBoolean("eclipse.night-vision", true);
    }

    public String getEclipseTitleWarning() {
        return color(cfg.getString("eclipse.title-warning", "&c☀ &fThe Eclipse Approaches &c☾"));
    }

    public String getEclipseTitleActive() {
        return color(cfg.getString("eclipse.title-active", "&4☀☾ &cBLOOD ECLIPSE &4☽☀"));
    }

    public String getEclipseTitleEnd() {
        return color(cfg.getString("eclipse.title-end", "&a☀ &fThe Eclipse has lifted &a☾"));
    }

    public String getEclipseBarWarning() {
        return color(cfg.getString("eclipse.bossbar-warning", "&c☀ Blood Eclipse incoming ☾"));
    }

    public String getEclipseBarActive() {
        return color(cfg.getString("eclipse.bossbar-active", "&4☀☾ &cBLOOD ECLIPSE ACTIVE &4☽☀"));
    }

    public BarColor getEclipseBarColor() {
        try {
            return BarColor.valueOf(cfg.getString("eclipse.bossbar-color", "RED"));
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }

    public BarStyle getEclipseBarStyle() {
        try {
            return BarStyle.valueOf(cfg.getString("eclipse.bossbar-style", "SEGMENTED_12"));
        } catch (IllegalArgumentException e) {
            return BarStyle.SEGMENTED_12;
        }
    }

    // ---- sol --------------------------------------------------------
    public double getSolSunlightDamage() {
        return cfg.getDouble("sol.sunlight.damage-multiplier", 1.3);
    }

    public int getSolSunlightRegenLevel() {
        return cfg.getInt("sol.sunlight.regeneration-level", 2);
    }

    public int getSolSunlightSpeedLevel() {
        return cfg.getInt("sol.sunlight.speed-level", 1);
    }

    public int getSolFireTicks() {
        return cfg.getInt("sol.sunlight.fire-ticks", 60);
    }

    public double getSolDarknessDamage() {
        return cfg.getDouble("sol.darkness.damage-multiplier", 0.8);
    }

    public int getSolDarknessSlownessLevel() {
        return cfg.getInt("sol.darkness.slowness-level", 1);
    }

    public int getSolDarknessWeaknessLevel() {
        return cfg.getInt("sol.darkness.weakness-level", 1);
    }

    // ---- luna -------------------------------------------------------
    public double getLunaDarknessDamage() {
        return cfg.getDouble("luna.darkness.damage-multiplier", 1.3);
    }

    public int getLunaDarknessSpeedLevel() {
        return cfg.getInt("luna.darkness.speed-level", 2);
    }

    public double getLunaCritMultiplier() {
        return cfg.getDouble("luna.darkness.crit-multiplier", 1.35);
    }

    public double getLunaBackstabAngle() {
        return cfg.getDouble("luna.darkness.backstab-angle", 60.0);
    }

    public double getLunaSunlightIncoming() {
        return cfg.getDouble("luna.sunlight.incoming-damage-multiplier", 1.5);
    }

    public boolean isLunaDashEnabled() {
        return cfg.getBoolean("luna.dash.enabled", true);
    }

    public int getLunaDashCooldownSeconds() {
        return cfg.getInt("luna.dash.cooldown-seconds", 15);
    }

    public int getLunaDashInvisibilitySeconds() {
        return cfg.getInt("luna.dash.invisibility-seconds", 3);
    }

    public boolean isLunaDashHideEquipment() {
        return cfg.getBoolean("luna.dash.hide-equipment", true);
    }

    public boolean isLunaDashHideEquipmentSelf() {
        return cfg.getBoolean("luna.dash.hide-equipment-self", true);
    }

    public double getLunaDashPower() {
        return cfg.getDouble("luna.dash.power", 1.4);
    }

    public double getLunaDashHeight() {
        return cfg.getDouble("luna.dash.height", 0.35);
    }

    public int getLunaDashWindowMs() {
        return cfg.getInt("luna.dash.double-tap-window-ms", 400);
    }

    public int getLunaDashMicroCooldownMs() {
        return cfg.getInt("luna.dash.micro-cooldown-ms", 200);
    }

    // ---- sol flare (mirror of the luna dash) -----------------------
    public boolean isSolFlareEnabled() {
        return cfg.getBoolean("sol.flare.enabled", true);
    }

    public int getSolFlareCooldownSeconds() {
        return cfg.getInt("sol.flare.cooldown-seconds", 15);
    }

    public double getSolFlareRadius() {
        return cfg.getDouble("sol.flare.radius", 4.0);
    }

    public int getSolFlareFireTicks() {
        return cfg.getInt("sol.flare.fire-ticks", 60);
    }

    public double getSolFlareKnockbackPower() {
        return cfg.getDouble("sol.flare.knockback-power", 1.2);
    }

    public double getSolFlareDamage() {
        return cfg.getDouble("sol.flare.damage", 6.0);
    }

    public int getSolFlareWindowMs() {
        return cfg.getInt("sol.flare.double-tap-window-ms", 400);
    }

    public int getSolFlareMicroCooldownMs() {
        return cfg.getInt("sol.flare.micro-cooldown-ms", 200);
    }

    // ---- eclipse powers --------------------------------------------
    public double getEclipseDamageMult() {
        return cfg.getDouble("eclipse-powers.damage-multiplier", 1.5);
    }

    public int getEclipseSpeedLevel() {
        return cfg.getInt("eclipse-powers.speed-level", 2);
    }

    public int getEclipseRegenLevel() {
        return cfg.getInt("eclipse-powers.regeneration-level", 2);
    }

    public double getEclipseSurgeDamageMult() {
        return cfg.getDouble("eclipse-powers.surge-damage-multiplier", 1.25);
    }

    // ---- shards -----------------------------------------------------
    public String getShardMaterial() {
        return cfg.getString("shards.item-material", "AMETHYST_SHARD");
    }

    public int getShardModelData() {
        return cfg.getInt("shards.custom-model-data", 14000);
    }

    public int getShardBaseDrop() {
        return cfg.getInt("shards.base-drop", 2);
    }

    public int getShardBonusPerGear() {
        return cfg.getInt("shards.bonus-per-gear", 1);
    }

    public int getShardMaxDrop() {
        return cfg.getInt("shards.max-drop", 6);
    }

    public int getShardAlignmentBonus() {
        return cfg.getInt("shards.alignment-bonus", 1);
    }

    public int getStartingBank() {
        return cfg.getInt("shards.starting-bank", 5);
    }

    public int getCarriedDropPercent() {
        return cfg.getInt("shards.carried-drop-percent", 100);
    }

    public int getNaturalDeathLoss() {
        return cfg.getInt("shards.natural-death-loss", 1);
    }

    public int getNonEclipsePvpLoss() {
        return cfg.getInt("shards.non-eclipse-pvp-loss", 1);
    }

    public int getEliminateAt() {
        return cfg.getInt("shards.eliminate-at", -5);
    }

    /** How long after hitting someone a player can still be credited as killer on their death. */
    public long getCombatTagMillis() {
        return cfg.getLong("shards.combat-tag-seconds", 60) * 1000L;
    }

    public boolean isShardRecipeEnabled() {
        return cfg.getBoolean("shards.recipe.enabled", true);
    }

    /** The shard recipe is only usable while bank + carried shards stay BELOW this. */
    public int getShardRecipeMaxOwned() {
        return cfg.getInt("shards.recipe.max-owned", 3);
    }

    public int getShardRecipeYield() {
        return cfg.getInt("shards.recipe.yield", 1);
    }

    /** Minimum time between two shard crafts per player (chest-stash proof). */
    public long getShardRecipeCooldownMillis() {
        return cfg.getLong("shards.recipe.cooldown-minutes", 30) * 60_000L;
    }

    /** Total shards one player can ever forge with this recipe. */
    public int getShardRecipeLifetimeCap() {
        return cfg.getInt("shards.recipe.lifetime-cap", 16);
    }

    /** Shard forging costs as MATERIAL:COUNT entries (max 9 grid slots). */
    public List<String> getShardRecipeIngredients() {
        return cfg.getStringList("shards.recipe.ingredients");
    }

    // ---- mace control -----------------------------------------------
    public boolean isMaceControlEnabled() {
        return cfg.getBoolean("mace-control.enabled", true);
    }

    public int getMaceMaxCrafted() {
        return cfg.getInt("mace-control.max-crafted", 2);
    }

    // ---- vanilla recipe tweaks --------------------------------------
    public boolean isGoldenAppleTweakEnabled() {
        return cfg.getBoolean("vanilla-tweaks.golden-apple", true);
    }

    public boolean isCobwebTweakEnabled() {
        return cfg.getBoolean("vanilla-tweaks.cobweb", true);
    }

    public boolean isAnvilTweakEnabled() {
        return cfg.getBoolean("vanilla-tweaks.anvil", true);
    }

    public boolean isTotemTweakEnabled() {
        return cfg.getBoolean("vanilla-tweaks.totem", true);
    }

    // ---- season (/grace and /end) -----------------------------------
    public int getSeasonGraceMinutes() {
        return cfg.getInt("season.grace-minutes", 15);
    }

    public int getSeasonEndTimerMinutes() {
        return cfg.getInt("season.end-timer-minutes", 30);
    }

    public boolean isEndPortalsStartOpen() {
        return cfg.getBoolean("season.end-portals-start-open", false);
    }

    // ---- elimination ------------------------------------------------
    public boolean isEliminationEnabled() {
        return cfg.getBoolean("elimination.enabled", true);
    }

    public long getEliminationBanMillis() {
        return cfg.getLong("elimination.ban-minutes", 1440) * 60_000L;
    }

    public int getEliminationResetTo() {
        return cfg.getInt("elimination.reset-to", 0);
    }

    // ---- gear -------------------------------------------------------
    public int getMaxTier() {
        return cfg.getInt("gear.max-tier", 5);
    }

    /** Whether netherite pieces are accepted in Eclipse gear crafting recipes. */
    public boolean isNetheriteEnabled() {
        return cfg.getBoolean("gear.netherite.enabled", true);
    }

    public double getBladeBonusPerTier() {
        return cfg.getDouble("gear.blade.bonus-per-tier", 1.0);
    }

    public double getAxeBonusPerTier() {
        return cfg.getDouble("gear.axe.bonus-per-tier", 1.5);
    }

    public int getAxeSlownessTicks() {
        return cfg.getInt("gear.axe.slowness-ticks", 40);
    }

    public double getBowBonusPerTier() {
        return cfg.getDouble("gear.bow.bonus-per-tier", 0.75);
    }

    public int getBowFireTicks() {
        return cfg.getInt("gear.bow.fire-ticks", 60);
    }

    // ---- gear allegiance abilities ---------------------------------
    public double getLunaLifestealPercent() {
        return cfg.getDouble("gear.abilities.luna-lifesteal-percent", 0.35);
    }

    public double getSolBladeSunlightMultiplier() {
        return cfg.getDouble("gear.abilities.sol-blade-multiplier", 1.15);
    }

    public double getSolAxeSunlightMultiplier() {
        return cfg.getDouble("gear.abilities.sol-axe-multiplier", 1.2);
    }

    public double getSolBowSunlightMultiplier() {
        return cfg.getDouble("gear.abilities.sol-bow-multiplier", 1.15);
    }

    public double getLunaBowDarknessMultiplier() {
        return cfg.getDouble("gear.abilities.luna-bow-multiplier", 1.3);
    }

    public int getLunaBowEclipseSlownessTicks() {
        return cfg.getInt("gear.abilities.luna-bow-eclipse-slowness-ticks", 60);
    }

    public int getLunaBowEclipseSlownessLevel() {
        return cfg.getInt("gear.abilities.luna-bow-eclipse-slowness-level", 2);
    }

    /** Damage reduction per total armor tier, while in darkness/eclipse. */
    public double getArmorReductionPerTier() {
        return cfg.getDouble("gear.armor.reduction-per-tier", 0.02);
    }

    public int getArmorSetRegenLevel() {
        return cfg.getInt("gear.armor.set-bonus.regeneration-level", 1);
    }

    /** Caps how far the full-set regen bonus scales with total armor tier. */
    public int getArmorSetRegenTierCap() {
        return cfg.getInt("gear.armor.set-bonus.regen-tier-cap", 2);
    }

    public int getArmorSetSpeedLevel() {
        return cfg.getInt("gear.armor.set-bonus.speed-level", 1);
    }

    public int getSolSetStrengthLevel() {
        return cfg.getInt("gear.armor.set-bonus.sol-strength-level", 1);
    }

    public int getLunaSetStrengthLevel() {
        return cfg.getInt("gear.armor.set-bonus.luna-strength-level", 1);
    }

    // ---- choose -----------------------------------------------------
    public int getFreeSwitches() {
        return cfg.getInt("choose.free-switches", 1);
    }

    public int getSwitchCost() {
        return cfg.getInt("choose.switch-cost", 20);
    }

    // ---- discord webhook -------------------------------------------
    public boolean isWebhookEnabled() {
        return cfg.getBoolean("discord-webhook.enabled", false);
    }

    // ---- debug ------------------------------------------------------
    public boolean isDebugEnabled() {
        return cfg.getBoolean("debug.enabled", false);
    }

    public String getWebhookUrl() {
        return cfg.getString("discord-webhook.url", "");
    }

    public String getWebhookUsername() {
        return cfg.getString("discord-webhook.username", "EclipseSMP");
    }

    public int getWebhookColor() {
        return cfg.getInt("discord-webhook.color", 0xFF5555);
    }

    public String getWebhookRoleId() {
        return cfg.getString("discord-webhook.role-id", "");
    }

    // ---- messages ---------------------------------------------------
    public String getMessage(String key, String fallback) {
        return color(cfg.getString("messages." + key, fallback));
    }

    public List<String> getStringList(String path, List<String> fallback) {
        List<String> list = cfg.getStringList(path);
        return list.isEmpty() ? fallback : list.stream().map(Settings::color).toList();
    }

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', hexAmpersand(s));
    }

    /**
     * Converts {@code &#RRGGBB} hex codes into the legacy form
     * ({@code §x§R§R§G§G§B§B}) that {@link ChatColor} and Adventure
     * understand, leaving everything else untouched.
     */
    private static String hexAmpersand(String s) {
        StringBuilder out = new StringBuilder(s.length() + 32);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 8 <= s.length() && s.charAt(i + 1) == '#'
                    && s.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                String hex = s.substring(i + 2, i + 8);
                out.append('§').append('x');
                for (int j = 0; j < 6; j++) {
                    out.append('§').append(hex.charAt(j));
                }
                i += 7;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
