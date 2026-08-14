package net.kingscraft.eclipseSMP.gui;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.shards.ShardItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeBook implements Listener {

    private static final String MAIN_TITLE = "&8Eclipse Recipes";
    private static final String GENERIC_ANVIL_TITLE = "&8Eclipse Anvil Upgrades";

    private record RecipeEntry(String key, String label, ItemStack result,
                               String[] shape, Map<Character, ItemStack> ingredients, boolean gear,
                               Material alt, String altName) {
    }

    private final EclipseSMP plugin;
    private final List<RecipeEntry> entries;
    private final Map<String, RecipeEntry> entriesByKey = new HashMap<>();

    public RecipeBook(EclipseSMP plugin) {
        this.plugin = plugin;
        this.entries = buildEntries();
        for (RecipeEntry entry : entries) {
            entriesByKey.put(entry.key(), entry);
        }
    }

    private RecipeEntry byKey(String key) {
        return entriesByKey.get(key);
    }

    public void open(Player player) {
        openMain(player);
    }

    // ---- gui builders ------------------------------------------------

    private void openMain(Player player) {
        Inventory inv = base(c(MAIN_TITLE));
        inv.setItem(10, mainItem(byKey("totem")));
        inv.setItem(11, mainItem(byKey("blade")));
        inv.setItem(12, mainItem(byKey("axe")));
        inv.setItem(13, mainItem(byKey("bow")));
        inv.setItem(14, mainItem(byKey("helmet")));
        inv.setItem(15, mainItem(byKey("chestplate")));
        inv.setItem(16, mainItem(byKey("leggings")));
        inv.setItem(19, mainItem(byKey("boots")));
        inv.setItem(20, anvilEntryItem());
        player.openInventory(inv);
    }

    private void openDetail(Player player, String key, String material) {
        RecipeEntry entry = entriesByKey.get(key);
        if (entry == null) return;

        boolean altView = material != null && entry.alt() != null && entry.alt().name().equals(material);
        Material current = altView ? entry.alt() : entry.result().getType();

        Inventory inv = base(c("&8Recipe: &f" + entry.label()));
        String[] shape = entry.shape();
        for (int row = 0; row < 3; row++) {
            String line = shape[row];
            for (int col = 0; col < 3; col++) {
                char ch = line.charAt(col);
                if (ch == ' ') continue;
                if (ch == 'X' && altView) {
                    inv.setItem(10 + row * 9 + col,
                            named(entry.alt(), "&f" + entry.altName(), "&7The base item to transform."));
                } else {
                    inv.setItem(10 + row * 9 + col, entry.ingredients().get(ch));
                }
            }
        }
        ItemStack result = altView ? ShardItem.withMaterial(entry.result(), entry.alt()) : entry.result();
        inv.setItem(25, result);

        inv.setItem(37, button(Material.BARRIER, "&c&lBack",
                List.of("&7Return to the recipe list."), "back", key, "main"));

        if (entry.gear()) {
            inv.setItem(40, info(Material.PAPER, "&7How to craft & upgrade",
                    List.of("&7Craft this item, then upgrade it",
                            "&7with &dEclipse Shards &7in an anvil.",
                            "",
                            "&7Use the &eAnvil &7button below.")));
            inv.setItem(43, button(Material.ANVIL, "&e&lAnvil Upgrades",
                    List.of("&7How to upgrade this item's tier."), "open_anvil", key, "detail",
                    current.name()));
        } else {
            inv.setItem(40, info(Material.PAPER, "&7About the Totem",
                    List.of("&7Craft the &4Eclipse Totem&7 and",
                            "&7right-click it to summon the",
                            "&4Blood Eclipse&7.")));
        }
        if (entry.alt() != null && plugin.getSettings().isNetheriteEnabled()) {
            inv.setItem(44, toggleButton(entry, current, "detail"));
        }
        player.openInventory(inv);
    }

    private void openAnvil(Player player, String key, String back, String material) {
        RecipeEntry entry = entriesByKey.get(key);
        if (entry == null || !entry.gear()) return;

        String title = "main".equals(back)
                ? c(GENERIC_ANVIL_TITLE) : c("&8Anvil: &f" + entry.label());
        Inventory inv = base(title);

        Material current = materialFor(entry, material);
        ItemStack t1 = current == entry.alt() ? ShardItem.withMaterial(entry.result(), entry.alt()) : entry.result();
        inv.setItem(20, t1);
        inv.setItem(21, arrow());
        inv.setItem(22, ShardItem.createShard(1));
        inv.setItem(23, arrow());
        inv.setItem(24, ShardItem.withTier(t1, 2));

        int maxTier = plugin.getSettings().getMaxTier();
        for (int tier = 1; tier <= Math.min(maxTier, 5); tier++) {
            inv.setItem(27 + tier, ShardItem.withTier(t1, tier));
        }

        inv.setItem(40, info(Material.PAPER, "&7How tier upgrades work",
                List.of("&7Place your Eclipse gear in an anvil",
                        "&7with &dEclipse Shards &7in the right slot.",
                        "",
                        "&d1 Eclipse Shard &7= &f1 Tier",
                        "&7Excess shards are returned.",
                        "&7Max tier: &c" + maxTier)));

        String backTo = "main".equals(back) ? "main" : "detail";
        inv.setItem(37, button(Material.BARRIER, "&c&lBack",
                List.of("&7Back."), "back", key, backTo, current.name()));
        if (entry.alt() != null && plugin.getSettings().isNetheriteEnabled()) {
            inv.setItem(44, toggleButton(entry, current, "anvil"));
        }
        player.openInventory(inv);
    }

    private Material materialFor(RecipeEntry entry, String material) {
        if (material != null && entry.alt() != null && entry.alt().name().equals(material)) {
            return entry.alt();
        }
        return entry.result().getType();
    }

    private ItemStack toggleButton(RecipeEntry entry, Material current, String page) {
        boolean toNetherite = entry.alt() != null && !entry.alt().equals(current);
        Material icon = toNetherite ? Material.NETHERITE_INGOT : Material.DIAMOND;
        String name = toNetherite ? "&8&lNetherite version" : "&b&lDiamond version";
        List<String> lore = List.of("&7Show the recipe with the "
                + (toNetherite ? "netherite" : "diamond") + " base piece.");
        return button(icon, name, lore, "toggle_material", entry.key(), page, current.name());
    }

    // ---- items ------------------------------------------------------

    private ItemStack mainItem(RecipeEntry entry) {
        ItemStack item = entry.result().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
        lore.add("");
        lore.add(c("&7Click to view the crafting recipe."));
        if (entry.gear()) {
            lore.add(c("&7Crafts to your allegiance."));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return tag(item, "open_recipe", entry.key(), null);
    }

    private ItemStack anvilEntryItem() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c("&e&lEclipse Anvil Upgrades"));
        meta.setLore(List.of(
                c("&7Upgrade your Eclipse gear"),
                c("&7with Eclipse Shards in an anvil."),
                c(""),
                c("&d1 Shard &7= &f1 Tier")
        ));
        item.setItemMeta(meta);
        return tag(item, "open_anvil_guide", null, null);
    }

    private ItemStack arrow() {
        return button(Material.GRAY_STAINED_GLASS_PANE, "&7➜", List.of(), null, null, null);
    }

    private ItemStack info(Material material, String name, List<String> lore) {
        return button(material, name, lore, null, null, null);
    }

    private ItemStack button(Material material, String name, List<String> lore,
                             String action, String recipe, String back) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(RecipeBook::c).toList());
        }
        item.setItemMeta(meta);
        return tag(item, action, recipe, back);
    }

    private ItemStack button(Material material, String name, List<String> lore,
                             String action, String recipe, String back, String mat) {
        return tag(button(material, name, lore, action, recipe, back), null, null, null, mat);
    }

    private ItemStack tag(ItemStack item, String action, String recipe, String back) {
        if (action == null && recipe == null && back == null) return item;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (action != null) pdc.set(actionKey(), PersistentDataType.STRING, action);
        if (recipe != null) pdc.set(recipeKey(), PersistentDataType.STRING, recipe);
        if (back != null) pdc.set(backKey(), PersistentDataType.STRING, back);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tag(ItemStack item, String action, String recipe, String back, String material) {
        ItemStack tagged = tag(item, action, recipe, back);
        if (material != null) {
            ItemMeta meta = tagged.getItemMeta();
            meta.getPersistentDataContainer().set(materialKey(), PersistentDataType.STRING, material);
            tagged.setItemMeta(meta);
        }
        return tagged;
    }

    private Inventory base(String title) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, pane);
        }
        return inv;
    }

    // ---- clicks -----------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!isOurs(title)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String action = pdc.get(actionKey(), PersistentDataType.STRING);
        if (action == null) return;

        String recipe = pdc.get(recipeKey(), PersistentDataType.STRING);
        String back = pdc.get(backKey(), PersistentDataType.STRING);
        switch (action) {
            case "open_recipe" -> openDetail(player, recipe, null);
            case "open_anvil" -> openAnvil(player, recipe, "detail",
                    pdc.get(materialKey(), PersistentDataType.STRING));
            case "open_anvil_guide" -> openAnvil(player, "blade", "main", null);
            case "back" -> {
                String mat = pdc.get(materialKey(), PersistentDataType.STRING);
                if ("detail".equals(back)) {
                    openDetail(player, recipe, mat);
                } else {
                    openMain(player);
                }
            }
            case "toggle_material" -> {
                RecipeEntry entry = entriesByKey.get(recipe);
                if (entry != null && entry.alt() != null) {
                    Material current = materialFor(entry, pdc.get(materialKey(), PersistentDataType.STRING));
                    Material next = current == entry.alt() ? entry.result().getType() : entry.alt();
                    String nextName = next.name();
                    if ("anvil".equals(back)) {
                        openAnvil(player, recipe, "detail", nextName);
                    } else {
                        openDetail(player, recipe, nextName);
                    }
                }
            }
            default -> {
            }
        }
    }

    private boolean isOurs(String title) {
        return title.equals(c(MAIN_TITLE))
                || title.equals(c(GENERIC_ANVIL_TITLE))
                || title.startsWith(c("&8Recipe: "))
                || title.startsWith(c("&8Anvil: "));
    }

    // ---- recipe data ------------------------------------------------

    private static List<RecipeEntry> buildEntries() {
        ItemStack shard = ShardItem.createShard(1);
        ItemStack star = named(Material.NETHER_STAR, "&fNether Star",
                "&7The heart of the Eclipse Totem.");

        List<RecipeEntry> list = new ArrayList<>();
        list.add(new RecipeEntry("totem", "Eclipse Totem", ShardItem.createTotem(),
                new String[]{"S S", " N ", "S S"}, Map.of('S', shard, 'N', star), false,
                null, null));

        String[] cross = {" S ", "SXS", " S "};
        addGear(list, "blade", Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                "Diamond Sword", "Netherite Sword", cross, shard);
        addGear(list, "axe", Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                "Diamond Axe", "Netherite Axe", cross, shard);
        addGear(list, "bow", Material.BOW, null, "Bow", null, cross, shard);
        addGear(list, "helmet", Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                "Diamond Helmet", "Netherite Helmet", cross, shard);
        addGear(list, "chestplate", Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
                "Diamond Chestplate", "Netherite Chestplate", cross, shard);
        addGear(list, "leggings", Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
                "Diamond Leggings", "Netherite Leggings", cross, shard);
        addGear(list, "boots", Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS,
                "Diamond Boots", "Netherite Boots", cross, shard);
        return list;
    }

    private static void addGear(List<RecipeEntry> list, String key, Material base, Material alt,
                                String baseName, String altName, String[] shape, ItemStack shard) {
        ItemStack result;
        if (base == Material.DIAMOND_SWORD) {
            result = ShardItem.createBlade(ShardItem.SIDE_SOL, 1);
        } else if (base == Material.DIAMOND_AXE) {
            result = ShardItem.createAxe(ShardItem.SIDE_SOL, 1);
        } else if (base == Material.BOW) {
            result = ShardItem.createBow(ShardItem.SIDE_SOL, 1);
        } else {
            result = ShardItem.createArmor(base, ShardItem.SIDE_SOL, 1);
        }
        ItemStack piece = named(base, "&f" + baseName, "&7The base item to transform.");
        list.add(new RecipeEntry(key, "Eclipse " + label(key), result, shape,
                Map.of('S', shard, 'X', piece), true, alt, altName));
    }

    private static String label(String key) {
        return switch (key) {
            case "blade" -> "Blade";
            case "axe" -> "Axe";
            case "bow" -> "Bow";
            case "helmet" -> "Helmet";
            case "chestplate" -> "Chestplate";
            case "leggings" -> "Leggings";
            case "boots" -> "Boots";
            default -> key;
        };
    }

    private static ItemStack named(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c(name));
        meta.setLore(Arrays.stream(new String[]{loreLine}).map(RecipeBook::c).toList());
        item.setItemMeta(meta);
        return item;
    }

    // ---- keys -------------------------------------------------------

    private NamespacedKey actionKey() {
        return plugin.getSettings().key("rb_action");
    }

    private NamespacedKey recipeKey() {
        return plugin.getSettings().key("rb_recipe");
    }

    private NamespacedKey backKey() {
        return plugin.getSettings().key("rb_back");
    }

    private NamespacedKey materialKey() {
        return plugin.getSettings().key("rb_material");
    }

    private static String c(String s) {
        return Settings.color(s);
    }
}
