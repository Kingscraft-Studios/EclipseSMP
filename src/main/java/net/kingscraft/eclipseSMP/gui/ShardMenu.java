package net.kingscraft.eclipseSMP.gui;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.eclipse.EclipsePhase;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ShardMenu implements Listener {

    public static final String TITLE = "Eclipse SMP";

    private final EclipseSMP plugin;

    public ShardMenu(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane());
        }
        inv.setItem(0, allegianceItem(player));
        inv.setItem(1, bankItem(player));
        inv.setItem(2, withdrawItem());
        inv.setItem(4, statusItem(player));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        if (clicked.getType() == ShardItem.shardMaterial()) {
            int deposited = plugin.getShardManager().depositShards(player);
            if (deposited > 0) {
                plugin.getMessages().send(player, "shards.deposited",
                        "&aDeposited &d{0} Eclipse Shards &ainto your bank.", deposited);
            } else {
                plugin.getMessages().send(player, "shards.no-carried", "&cYou have no carried Eclipse Shards to deposit.");
            }
            open(player);
            return;
        }

        switch (clicked.getType()) {
            case SUNFLOWER, CLOCK, BARRIER -> {
                player.closeInventory();
                plugin.getAllegianceGUI().open(player);
            }
            case EMERALD -> {
                if (plugin.getShardManager().withdrawShards(player, 1)) {
                    plugin.getMessages().send(player, "shards.withdrew",
                            "&aWithdrew &d{0} Eclipse Shard &afrom your bank.", 1);
                } else {
                    plugin.getMessages().send(player, "shards.bank-empty", "&cYour bank is empty.");
                }
                open(player);
            }
            case COMPASS -> open(player);
            default -> {
            }
        }
    }

    private ItemStack allegianceItem(Player player) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        ItemStack item;
        String name;
        List<String> lore;
        if (profile.hasAllegiance()) {
            Allegiance a = profile.getAllegiance();
            item = new ItemStack(a == Allegiance.SOL ? Material.SUNFLOWER : Material.CLOCK);
            name = (a == Allegiance.SOL ? "&6&l☀ Sol" : "&8&l☾ Luna");
            lore = List.of(
                    color("&7Your allegiance: &f" + a.getDisplayName() + " " + a.getSymbol()),
                    color("&7Click to change allegiance.")
            );
        } else {
            item = new ItemStack(Material.BARRIER);
            name = "&cNo Allegiance";
            lore = List.of(color("&7Click to choose Sol or Luna!"));
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack bankItem(Player player) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        ItemStack item = new ItemStack(ShardItem.shardMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&d&lShard Bank"));
        meta.setLore(List.of(
                color("&7Banked: &d" + profile.getBank()),
                color("&7Total earned: &d" + profile.getTotalEarned()),
                color("&7Kills: &c" + profile.getKills()),
                color("&7Click to deposit all carried shards.")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack withdrawItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&a&lWithdraw Shard"));
        meta.setLore(List.of(
                color("&7Withdraw &f1&7 shard from your bank."),
                color("&7For more: &f/withdraw <amount>")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack statusItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&6&lEclipse Status"));
        EclipsePhase phase = plugin.getEclipseManager().getPhase();
        String phaseName = switch (phase) {
            case WARNING -> plugin.getMessages().msg("phase.warning", "&cWarning — incoming!");
            case ACTIVE -> plugin.getMessages().msg("phase.active", "&4ACTIVE");
            case COOLDOWN -> plugin.getMessages().msg("phase.cooldown", "&7Cooling down");
            default -> plugin.getMessages().msg("phase.waiting", "&aWaiting");
        };
        long millis = plugin.getEclipseManager().getTimeUntilNextEclipseMillis();
        String next = (phase == EclipsePhase.WARNING || phase == EclipsePhase.ACTIVE)
                ? formatTime(millis) : plugin.getMessages().msg("time.unknown", "&c??");
        meta.setLore(List.of(
                color("&7Phase: " + phaseName),
                color("&7Next eclipse in: &f" + next),
                color("&7Click to refresh.")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private ItemStack pane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static String color(String s) {
        return Settings.color(s);
    }
}
