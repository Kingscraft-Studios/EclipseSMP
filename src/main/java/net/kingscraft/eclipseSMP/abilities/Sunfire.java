package net.kingscraft.eclipseSMP.abilities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class Sunfire {

    private Sunfire() {
    }

    public static void ignite(Entity target, int fireTicks) {
        if (target instanceof LivingEntity living) {
            living.setFireTicks(Math.max(fireTicks, living.getFireTicks()));
        }
    }
}
