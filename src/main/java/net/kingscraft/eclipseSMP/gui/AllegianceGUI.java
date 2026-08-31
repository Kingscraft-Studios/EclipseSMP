package net.kingscraft.eclipseSMP.gui;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AllegianceGUI implements Listener {

    public static final String CHOOSE_TITLE = "Choose your Allegiance";
    public static final String SWITCH_TITLE = "Switch Allegiance";
    public static final String CONFIRM_TITLE = "Confirm Allegiance Switch";

    private final EclipseSMP plugin;
    private final Map<UUID, Allegiance> pending = new HashMap<>();

    public AllegianceGUI(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        pending.remove(player.getUniqueId());
        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (profile.hasAllegiance()) {
            openSwitch(player, profile);
        } else {
            openChoose(player);
        }
    }

    private void openChoose(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, CHOOSE_TITLE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane());
        }
        inv.setItem(4, costInfoItem(null));
        inv.setItem(11, allegianceItem(Allegiance.SOL, null));
        inv.setItem(15, allegianceItem(Allegiance.LUNA, null));
        player.openInventory(inv);
    }

    private void openSwitch(Player player, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, SWITCH_TITLE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane());
        }
        Allegiance current = profile.getAllegiance();
        Allegiance other = current == Allegiance.SOL ? Allegiance.LUNA : Allegiance.SOL;
        inv.setItem(11, currentItem(current));
        inv.setItem(13, costInfoItem(profile));
        inv.setItem(15, allegianceItem(other, profile));
        player.openInventory(inv);
    }

    private void openConfirm(Player player, Allegiance target, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane());
        }
        inv.setItem(11, allegianceItem(target, profile));
        inv.setItem(13, confirmItem(target, profile));
        inv.setItem(15, cancelItem());
        player.openInventory(inv);
    }

    // ---- items -----------------------------------------------------

    private ItemStack allegianceItem(Allegiance a, PlayerProfile profile) {
        ItemStack item = new ItemStack(a == Allegiance.SOL ? Material.SUNFLOWER : Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (profile == null) {
            meta.setDisplayName(color(a == Allegiance.SOL ? "&6&l☀ Child of the Sun" : "&8&l☾ Child of the Moon"));
            meta.setLore(List.of(
                    a == Allegiance.SOL
                            ? color("&7Daylight empowers you. Night weakens you.")
                            : color("&7The dark is your home. Sunlight burns you."),
                    "",
                    color("&7First choice is free. Click to join.")
            ));
        } else {
            boolean free = profile.getSwitches() < plugin.getSettings().getFreeSwitches();
            int cost = plugin.getSettings().getSwitchCost();
            meta.setDisplayName(color(a == Allegiance.SOL ? "&6&lSwitch to ☀ Sol" : "&8&lSwitch to ☾ Luna"));
            meta.setLore(List.of(
                    color("&7Click to switch allegiance."),
                    "",
                    free
                            ? color("&7This switch is free.")
                            : color("&7Cost: &d" + cost + " Eclipse Shards"),
                    color("&7Bank: &d" + profile.getBank())
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack currentItem(Allegiance current) {
        ItemStack item = new ItemStack(current == Allegiance.SOL ? Material.SUNFLOWER : Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&7Current: &f" + current.getDisplayName() + " " + current.getSymbol()));
        meta.setLore(List.of(color("&7You follow the " + (current == Allegiance.SOL ? "day" : "night") + ".")));
        item.setItemMeta(meta);
        return item;
    }

    /** Explains the switch pricing: first switch free, then the configured shard cost. */
    private ItemStack costInfoItem(PlayerProfile profile) {
        int free = plugin.getSettings().getFreeSwitches();
        int cost = plugin.getSettings().getSwitchCost();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&e&lSwitch Rules"));

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Switching sides costs shards:"));
        if (profile == null || profile.getSwitches() < free) {
            lore.add(color("&7- Your first switch is &afree&7."));
        } else {
            lore.add(color("&7- Your free switch is &cused up&7."));
        }
        lore.add(color("&7- After that: &d" + cost + " Eclipse Shards &7per switch."));
        lore.add(color("&7- Gear reforges to the new side &fautomatically&7."));
        if (profile != null) {
            lore.add("");
            lore.add(color("&7Your switches used: &f" + profile.getSwitches()));
            lore.add(color("&7Shard bank: &d" + profile.getBank()));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack confirmItem(Allegiance target, PlayerProfile profile) {
        boolean free = profile.getSwitches() < plugin.getSettings().getFreeSwitches();
        int cost = plugin.getSettings().getSwitchCost();
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&a&l✓ Confirm Switch"));
        meta.setLore(List.of(
                color("&7Switch to &f" + target.getDisplayName() + " " + target.getSymbol()),
                free
                        ? color("&7Cost: &afree")
                        : color("&7Cost: &d" + cost + " shards &7· Bank: &d" + profile.getBank())
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack cancelItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&c&l✗ Cancel"));
        meta.setLore(List.of(color("&7Back to the allegiance list.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    // ---- clicks -----------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(title.equals(CHOOSE_TITLE) || title.equals(SWITCH_TITLE) || title.equals(CONFIRM_TITLE))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        if (title.equals(CONFIRM_TITLE)) {
            handleConfirmClick(player, clicked);
            return;
        }

        Allegiance chosen = allegianceOf(clicked.getType());
        if (chosen == null) return;

        if (title.equals(SWITCH_TITLE)) {
            PlayerProfile profile = plugin.getProfileManager().get(player);
            if (profile.getAllegiance() == chosen) {
                plugin.getMessages().send(player, "choose.already-follower",
                        "&eYou are already a follower of {0}.", chosen.getDisplayName());
                return;
            }
            pending.put(player.getUniqueId(), chosen);
            openConfirm(player, chosen, profile);
            return;
        }

        applySwitch(player, chosen);
    }

    private void handleConfirmClick(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.EMERALD) {
            Allegiance target = pending.get(player.getUniqueId());
            if (target != null) {
                applySwitch(player, target);
            }
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            pending.remove(player.getUniqueId());
            open(player);
        }
    }

    private void applySwitch(Player player, Allegiance chosen) {
        String error = plugin.getProfileManager().switchTo(player, chosen);
        pending.remove(player.getUniqueId());
        if (error != null) {
            plugin.getMessages().send(player, error);
            player.closeInventory();
            return;
        }
        player.closeInventory();
        boolean sol = chosen == Allegiance.SOL;
        plugin.getMessages().send(player, sol ? "choose.success.sol" : "choose.success.luna",
                sol ? "&6You are now a &6Child of the Sun&r!" : "&8You are now a &8Child of the Moon&r!");
        plugin.getMessages().title(player,
                plugin.getMessages().msg(sol ? "choose.title.sol" : "choose.title.luna",
                        sol ? "&6☀ Sol" : "&8☾ Luna"),
                plugin.getMessages().msg("choose.subtitle", "&7Your powers now follow the {0}",
                        sol ? "day" : "night"),
                10, 60, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    private static Allegiance allegianceOf(Material type) {
        return switch (type) {
            case SUNFLOWER -> Allegiance.SOL;
            case CLOCK -> Allegiance.LUNA;
            default -> null;
        };
    }

    private static String color(String s) {
        return Settings.color(s);
    }
}
