package net.kingscraft.eclipseSMP.eclipse;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class EclipseTotem implements Listener {

    private final EclipseSMP plugin;

    public EclipseTotem(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !ShardItem.isTotem(item)) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        // Tiny per-player anti-spam so one click can't fire twice.
        if (plugin.getCooldownManager().has(player.getUniqueId(), "totem")) {
            plugin.getMessages().actionBar(player, "totem.recharging", "&cThe Totem is still recharging...");
            return;
        }
        plugin.getCooldownManager().start(player.getUniqueId(), "totem", 1000);

        String error = plugin.getEclipseManager().attemptTrigger(player);
        if (error != null) {
            plugin.getMessages().send(player, error);
            return;
        }

        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            player.getInventory().setItem(hand, null);
        }
        plugin.getMessages().send(player, "totem.summoned", "&4☀ &cYou shatter the Totem. The Blood Eclipse is summoned! &4☾");
    }
}
