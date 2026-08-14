package net.kingscraft.eclipseSMP.allegiance;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class InventorySideListener implements Listener {

    private final EclipseSMP plugin;

    public InventorySideListener(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!hasAllegiance(player)) return;
        convert(event.getCurrentItem(), allegianceOf(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!hasAllegiance(player)) return;
        for (ItemStack item : event.getNewItems().values()) {
            convert(item, allegianceOf(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasAllegiance(player)) return;
        convert(event.getItem().getItemStack(), allegianceOf(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getProfileManager().convertGear(event.getPlayer());
    }

    private boolean hasAllegiance(Player player) {
        return plugin.getProfileManager().get(player).hasAllegiance();
    }

    private Allegiance allegianceOf(Player player) {
        return plugin.getProfileManager().get(player).getAllegiance();
    }

    private void convert(ItemStack item, Allegiance allegiance) {
        if (item == null || !ShardItem.isGear(item)) return;
        String side = ShardItem.sideOf(item);
        if (side == null) return;
        String target = allegiance == Allegiance.SOL ? ShardItem.SIDE_SOL : ShardItem.SIDE_LUNA;
        if (target.equals(side)) return;
        item.setItemMeta(ShardItem.withSide(item, target).getItemMeta());
    }
}
