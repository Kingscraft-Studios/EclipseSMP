package net.kingscraft.eclipseSMP.mace;

import net.kingscraft.eclipseSMP.EclipseSMP;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-wide Mace budget: only {@code max-crafted} Maces can ever be
 * handcrafted, tracked via save.yml so it survives restarts. Automated
 * Crafters are always blocked from producing Maces.
 */
public final class MaceControl implements Listener {

    private static final String KEY_CRAFTED = "maces-crafted";
    private static final long CRAFTER_WARN_COOLDOWN_MS = 30_000L;

    private final EclipseSMP plugin;
    private final Map<String, Long> crafterWarnings = new HashMap<>();

    public MaceControl(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    /** Hand crafting: consumes budget, denies once spent. */
    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getSettings().isMaceControlEnabled()) return;
        ItemStack result = resultOf(event.getRecipe());
        if (result == null || result.getType() != Material.MACE) return;

        int budget = plugin.getSettings().getMaceMaxCrafted();
        int used = usedCount();
        if (used >= budget) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                plugin.getMessages().send(player, "mace.cap-reached",
                        "&cNo more Maces can be crafted! &7(&d{0}&7/&d{1}&7 already forged)",
                        used, budget);
            }
            return;
        }

        plugin.getSaveStore().set(KEY_CRAFTED, used + 1);
        plugin.getMessages().broadcast("mace.forged",
                "&6⚒ &f{0} &7forged a Mace! &7(&d{1}&7/&d{2}&7 budget used)",
                event.getWhoClicked().getName(), used + 1, budget);
    }

    /** Auto-Crafters may never produce Maces; nearby players get told why. */
    @EventHandler(ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (!plugin.getSettings().isMaceControlEnabled()) return;
        ItemStack result = event.getResult();
        if (result == null || result.getType() != Material.MACE) return;

        event.setCancelled(true);
        warnNearby(event.getBlock());
    }

    private void warnNearby(Block block) {
        String where = block.getWorld().getName() + ":"
                + block.getX() + "," + block.getY() + "," + block.getZ();
        long now = System.currentTimeMillis();
        Long last = crafterWarnings.get(where);
        if (last != null && now - last < CRAFTER_WARN_COOLDOWN_MS) return;
        crafterWarnings.put(where, now);

        plugin.getLogger().warning("Blocked an automated Mace craft at " + where);
        for (Player player : block.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(block.getLocation()) <= 16 * 16) {
                plugin.getMessages().send(player, "mace.crafter-denied",
                        "&cCrafting a Mace via a Crafter is not allowed! Forge it by hand.");
            }
        }
    }

    /** Current number of forged Maces. */
    public int usedCount() {
        return (int) Math.max(0, plugin.getSaveStore().getLong(KEY_CRAFTED, 0));
    }

    /** Admin adjustment of the forged counter (negative values refund budget). */
    public void adjustUsed(int delta) {
        plugin.getSaveStore().set(KEY_CRAFTED, Math.max(0, usedCount() + delta));
    }

    private static ItemStack resultOf(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) return shaped.getResult();
        if (recipe instanceof ShapelessRecipe shapeless) return shapeless.getResult();
        return null;
    }
}
