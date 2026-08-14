package net.kingscraft.eclipseSMP.powers;

import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.environment.LightState;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class LunaPowers {

    private static final int EFFECT_DURATION = 45;

    private LunaPowers() {
    }

    /** Effects for a Luna player based on their environment. */
    public static List<PotionEffect> effects(Settings settings, LightState state) {
        List<PotionEffect> effects = new ArrayList<>();
        if (state == LightState.DARKNESS && settings.getLunaDarknessSpeedLevel() > 0) {
            effects.add(new PotionEffect(PotionEffectType.SPEED,
                    EFFECT_DURATION, settings.getLunaDarknessSpeedLevel() - 1));
        }
        return effects;
    }
}
