package net.kingscraft.eclipseSMP.shards;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public final class AnvilUpgrades implements Listener {

    private final EclipseSMP plugin;

    public AnvilUpgrades(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;

        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (right == null || !ShardItem.isShard(right)) {
            return;
        }
        if (left == null || left.getType().isAir() || !ShardItem.isGear(left)) {
            event.setResult(null);
            return;
        }

        int currentTier = ShardItem.tierOf(left);
        int maxTier = plugin.getSettings().getMaxTier();
        if (currentTier >= maxTier) {
            // Already maxed — leave the result slot empty so the anvil
            // arrow shows the 'cannot combine' X instead of a chat message.
            event.setResult(null);
            return;
        }

        int tiers = Math.min(right.getAmount(), maxTier - currentTier);
        event.setResult(ShardItem.withTier(left, currentTier + tiers));
        inv.setRepairCost(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (event.getSlot() != 2) return;
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = inv.getItem(2);
        if (result == null || !ShardItem.isGear(result)) return;

        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || !ShardItem.isGear(left)) return;

        int currentTier = ShardItem.tierOf(left);
        int newTier = ShardItem.tierOf(result);
        int gained = newTier - currentTier;
        if (gained <= 0) return;

        int shards = right == null ? 0 : right.getAmount();
        if (shards < gained) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "anvil.need-shards",
                    "&cYou need &d{0} Eclipse Shards&c to upgrade.", gained);
            return;
        }
        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "anvil.cursor-full", "&cEmpty your cursor first.");
            return;
        }

        event.setCancelled(true);

        if (shards == gained) {
            inv.setItem(1, null);
        } else {
            ItemStack leftover = right.clone();
            leftover.setAmount(shards - gained);
            inv.setItem(1, leftover);
        }
        inv.setItem(0, null);
        inv.setItem(2, null);
        player.getOpenInventory().setCursor(result);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        plugin.getMessages().actionBar(player, "anvil.upgraded", "&aUpgraded to &4Tier {0}&a!", newTier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getCurrentItem();
        if (result == null || !ShardItem.isGear(result)) return;

        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (!profile.hasAllegiance()) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "anvil.choose-allegiance",
                    "&cChoose an allegiance before crafting Eclipse gear: &e/eclipse choose");
            return;
        }

        Allegiance side = profile.getAllegiance();
        String sideKey = side == Allegiance.SOL ? ShardItem.SIDE_SOL : ShardItem.SIDE_LUNA;
        ItemStack rebuilt = result;
        if (!sideKey.equals(ShardItem.sideOf(rebuilt))) {
            rebuilt = ShardItem.withSide(rebuilt, sideKey);
        }
        ItemStack base = baseItem(event.getInventory().getMatrix());
        if (base != null && base.getType() != rebuilt.getType()) {
            rebuilt = ShardItem.withMaterial(rebuilt, base.getType());
        }
        if (rebuilt != result) {
            event.setCurrentItem(rebuilt);
        }
    }

    /** The single non-shard ingredient in the crafting matrix — the base piece being transformed. */
    private static ItemStack baseItem(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (ShardItem.isShard(item)) continue;
            return item;
        }
        return null;
    }
}
