package net.kingscraft.eclipseSMP.environment;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class SunlightDetector {

    private static final int SUNLIGHT_SKY = 13;
    private static final int DARK_SKY = 4;

    private SunlightDetector() {
    }

    /**
     * Resolves the light state at the player's current position.
     * Nether/End are always darkness. Direct sunlight requires an open
     * sky above and strong skylight during daytime.
     */
    public static LightState resolve(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return LightState.DARKNESS;
        }

        long time = world.getTime();
        if (time < 0 || time >= 13000) {
            return LightState.DARKNESS;
        }

        Block block = player.getLocation().getBlock();
        int skyLight = block.getLightFromSky();

        if (isSkyVisible(player) && skyLight >= SUNLIGHT_SKY) {
            return LightState.SUNLIGHT;
        }
        if (skyLight <= DARK_SKY) {
            return LightState.DARKNESS;
        }
        return LightState.SHADOW;
    }

    private static boolean isSkyVisible(Player player) {
        World world = player.getWorld();
        Block eye = player.getEyeLocation().getBlock();
        int y = eye.getY() + 1;
        int maxY = world.getMaxHeight();
        int x = eye.getX();
        int z = eye.getZ();
        for (int i = y; i < maxY; i++) {
            Block above = world.getBlockAt(x, i, z);
            if (!above.isPassable()) {
                return false;
            }
        }
        return true;
    }
}
