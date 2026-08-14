package net.kingscraft.eclipseSMP.powers;

import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.environment.LightState;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class SolPowers {

    private static final int EFFECT_DURATION = 45;

    private SolPowers() {
    }

    /** Effects for a Sol player based on their environment. */
    public static List<PotionEffect> effects(Settings settings, LightState state) {
        List<PotionEffect> effects = new ArrayList<>();
        switch (state) {
            case SUNLIGHT -> {
                effects.add(new PotionEffect(PotionEffectType.REGENERATION,
                        EFFECT_DURATION, settings.getSolSunlightRegenLevel() - 1));
                if (settings.getSolSunlightSpeedLevel() > 0) {
                    effects.add(new PotionEffect(PotionEffectType.SPEED,
                            EFFECT_DURATION, settings.getSolSunlightSpeedLevel() - 1));
                }
            }
            case DARKNESS -> {
                effects.add(new PotionEffect(PotionEffectType.SLOWNESS,
                        EFFECT_DURATION, settings.getSolDarknessSlownessLevel() - 1));
                effects.add(new PotionEffect(PotionEffectType.WEAKNESS,
                        EFFECT_DURATION, settings.getSolDarknessWeaknessLevel() - 1));
            }
            default -> {
            }
        }
        return effects;
    }
}
