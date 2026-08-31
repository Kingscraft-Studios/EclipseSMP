package net.kingscraft.eclipseSMP.powers;

import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.eclipse.EclipseManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class EclipsePowers {

    private static final int EFFECT_DURATION = 45;

    private EclipsePowers() {
    }

    /** Combined ultra-state everyone gets during a Blood Eclipse. */
    public static List<PotionEffect> effects(Settings settings, Player player, int surge) {
        List<PotionEffect> effects = new ArrayList<>();
        int speed = settings.getEclipseSpeedLevel();
        int regen = settings.getEclipseRegenLevel();
        if (surge == EclipseManager.SURGE_SPEED) speed++;
        if (surge == EclipseManager.SURGE_REGEN) regen++;

        if (speed > 0) {
            effects.add(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, speed - 1));
        }
        if (regen > 0) {
            effects.add(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, regen - 1));
        }
        if (settings.isNightVision()) {
            effects.add(new PotionEffect(PotionEffectType.NIGHT_VISION, EFFECT_DURATION, 0));
        }
        return effects;
    }
}
