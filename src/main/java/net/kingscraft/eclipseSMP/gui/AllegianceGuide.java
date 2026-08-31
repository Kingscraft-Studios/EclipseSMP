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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The allegiance guide: explains every power the viewer's side grants.
 * Sol and Luna each get their own page and title.
 */
public final class AllegianceGuide implements Listener {

    private static final String SOL_TITLE = "&6\u2600 Sol Powers";
    private static final String LUNA_TITLE = "&8\u263E Luna Powers";

    private final EclipseSMP plugin;

    public AllegianceGuide(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (!profile.hasAllegiance()) {
            plugin.getMessages().send(player, "power.no-allegiance",
                    "&cChoose your allegiance: &e/eclipse choose");
            plugin.getAllegianceGUI().open(player);
            return;
        }

        boolean sol = profile.getAllegiance() == Allegiance.SOL;
        Inventory inv = Bukkit.createInventory(null, 27,
                Settings.color(sol ? SOL_TITLE : LUNA_TITLE));
        fillPanes(inv);

        if (sol) {
            buildSol(inv);
        } else {
            buildLuna(inv);
        }
        inv.setItem(4, banner(sol));
        inv.setItem(18, eclipseItem());
        inv.setItem(26, backButton());
        player.openInventory(inv);
    }

    // ---- sol page ----------------------------------------------------

    private void buildSol(Inventory inv) {
        Settings s = plugin.getSettings();
        inv.setItem(10, entry(Material.BLAZE_POWDER, "&6&lSunlight Power", List.of(
                line("While standing in direct sunlight:"),
                stat("Damage dealt", "x" + s.getSolSunlightDamage()),
                buff("Regeneration", roman(s.getSolSunlightRegenLevel())),
                buff("Speed", roman(s.getSolSunlightSpeedLevel())),
                line("Your melee hits set enemies on fire."))));
        inv.setItem(11, entry(Material.CLOCK, "&7&lAfter Dark", List.of(
                line("At night you are weakened:"),
                stat("Damage dealt", "x" + s.getSolDarknessDamage()),
                debuff("Slowness", roman(s.getSolDarknessSlownessLevel())),
                debuff("Weakness", roman(s.getSolDarknessWeaknessLevel())),
                line("Full Sol armor ignores this entirely!"))));
        inv.setItem(13, entry(Material.FIRE_CHARGE, "&c&lSolar Flare", List.of(
                line("Double-tap sneak in sunlight to"),
                line("detonate a solar burst around you."),
                stat("Blast damage", format(s.getSolFlareDamage()) + " (armor-piercing)"),
                stat("Radius", format(s.getSolFlareRadius())),
                stat("Ignites targets", "yes"),
                stat("Cooldown", s.getSolFlareCooldownSeconds() + "s"))));
        inv.setItem(15, entry(Material.DIAMOND_SWORD, "&6&lSol Gear", List.of(
                stat("Eclipse Blade", "+" + pct(s.getSolBladeSunlightMultiplier())
                        + " damage & ignites in sun"),
                stat("Eclipse Axe", "+" + pct(s.getSolAxeSunlightMultiplier()) + " & slows targets"),
                stat("Eclipse Bow", "arrows ignite in sunlight"))));
        inv.setItem(16, entry(Material.GOLDEN_CHESTPLATE, "&6&lSol Armor Set", List.of(
                line("Wearing Eclipse armor in sunlight:"),
                buff("Strength", roman(s.getSolSetStrengthLevel())),
                buff("Regeneration", roman(s.getArmorSetRegenLevel())),
                buff("Speed", roman(s.getArmorSetSpeedLevel())),
                line("No night weakness or slowness."))));
    }

    // ---- luna page ---------------------------------------------------

    private void buildLuna(Inventory inv) {
        Settings s = plugin.getSettings();
        inv.setItem(10, entry(Material.OBSIDIAN, "&8&lDarkness Power", List.of(
                line("While in darkness (night/caves):"),
                stat("Damage dealt", "x" + s.getLunaDarknessDamage()),
                buff("Speed", roman(s.getLunaDarknessSpeedLevel())),
                line("The night is your domain."))));
        inv.setItem(11, entry(Material.GLOWSTONE_DUST, "&e&lDaylight Weakness", List.of(
                line("Caught in direct daylight:"),
                stat("Damage taken", "x" + s.getLunaSunlightIncoming()),
                line("Full Luna armor removes this penalty!"))));
        inv.setItem(13, entry(Material.FEATHER, "&8&lVanish Dash", List.of(
                line("Double-tap sneak in darkness to"),
                line("dash forward and vanish."),
                stat("Invisibility", s.getLunaDashInvisibilitySeconds() + "s"),
                stat("Gear hidden", "yes"),
                stat("Cooldown", s.getLunaDashCooldownSeconds() + "s"))));
        inv.setItem(15, entry(Material.DIAMOND_SWORD, "&8&lLuna Gear", List.of(
                stat("Eclipse Blade", "lifesteals "
                        + Math.round(s.getLunaLifestealPercent() * 100) + "% in darkness"),
                stat("Eclipse Axe", "backstab crits from behind"),
                stat("Eclipse Bow", "crits in dark, slows in eclipse"))));
        inv.setItem(16, entry(Material.CHAINMAIL_CHESTPLATE, "&8&lLuna Armor Set", List.of(
                line("Wearing Eclipse armor in darkness:"),
                buff("Strength", roman(s.getLunaSetStrengthLevel())),
                buff("Speed", roman(s.getArmorSetSpeedLevel())),
                line("No daylight damage penalty."))));
    }

    // ---- shared ------------------------------------------------------

    private ItemStack eclipseItem() {
        Settings s = plugin.getSettings();
        return entry(Material.WITHER_ROSE, "&4&lBlood Eclipse — Both Sides", List.of(
                line("When a Blood Eclipse rages:"),
                stat("Damage dealt", "x" + s.getEclipseDamageMult()),
                buff("Speed", roman(s.getEclipseSpeedLevel())),
                buff("Regeneration", roman(s.getEclipseRegenLevel())),
                line("Everyone gets one random surge."),
                line("PvP kills drop Eclipse Shards!")));
    }

    private ItemStack banner(boolean sol) {
        return entry(sol ? Material.SUNFLOWER : Material.CLOCK,
                sol ? "&6&l\u2600 Child of the Sun" : "&8&l\u263E Child of the Moon",
                List.of(line("Everything your allegiance can do."),
                        line("Switch sides: /eclipse choose")));
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&c&lBack"));
        meta.setLore(List.of(color("&7Return to the main menu.")));
        item.setItemMeta(meta);
        return item;
    }

    // ---- clicks ------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean ours = title.equals(Settings.color(SOL_TITLE)) || title.equals(Settings.color(LUNA_TITLE));
        if (!ours) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            plugin.getShardMenu().open(player);
        }
    }

    // ---- helpers -----------------------------------------------------

    private void fillPanes(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private ItemStack entry(Material material, String name, List<String> lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> lore = new ArrayList<>();
        for (String text : lines) {
            lore.add(color(text));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String line(String text) {
        return "&7" + text;
    }

    private static String stat(String label, String value) {
        return "&7" + label + ": &b" + value;
    }

    private static String buff(String label, String value) {
        return "&7" + label + " &a" + value;
    }

    private static String debuff(String label, String value) {
        return "&7" + label + " &c" + value;
    }

    private static String pct(double multiplier) {
        return "+" + Math.round((multiplier - 1.0) * 100) + "%";
    }

    private static String format(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(level);
        };
    }

    private static String color(String s) {
        return Settings.color(s);
    }
}
