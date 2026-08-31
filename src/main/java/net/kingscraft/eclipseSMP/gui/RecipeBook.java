package net.kingscraft.eclipseSMP.gui;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
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
import java.util.LinkedHashMap;
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

    private List<RecipeEntry> buildEntries() {
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

        list.add(shardEntry());
        addTweaks(list);
        return list;
    }

    /**
     * The four config-gated vanilla rebalances (VanillaRecipeTweaks). They get
     * their own main-menu section and only appear while enabled in config.yml.
     */
    private void addTweaks(List<RecipeEntry> list) {
        if (plugin.getSettings().isGoldenAppleTweakEnabled()) {
            list.add(new RecipeEntry("tw_golden_apple", "Golden Apple",
                    new ItemStack(Material.GOLDEN_APPLE),
                    new String[]{" G ", "GAG", " G "},
                    Map.of('G', named(Material.GOLD_INGOT, "&fGold Ingot", "&7Only 4 - half of vanilla."),
                            'A', named(Material.APPLE, "&fApple", "&7The core.")),
                    false, null, null));
        }
        if (plugin.getSettings().isCobwebTweakEnabled()) {
            list.add(new RecipeEntry("tw_cobweb", "Cobweb",
                    new ItemStack(Material.COBWEB),
                    new String[]{"S S", " S ", "S S"},
                    Map.of('S', named(Material.STRING, "&fString", "&75 strands in an X.")),
                    false, null, null));
        }
        if (plugin.getSettings().isAnvilTweakEnabled()) {
            list.add(new RecipeEntry("tw_anvil", "Anvil",
                    new ItemStack(Material.ANVIL),
                    new String[]{"III", " I ", "III"},
                    Map.of('I', named(Material.IRON_INGOT, "&fIron Ingot", "&77 total instead of 31 iron.")),
                    false, null, null));
        }
        if (plugin.getSettings().isTotemTweakEnabled()) {
            list.add(new RecipeEntry("tw_totem", "Totem of Undying",
                    new ItemStack(Material.TOTEM_OF_UNDYING),
                    new String[]{"GEG", "GDG", "GGG"},
                    Map.of('G', named(Material.GOLD_INGOT, "&fGold Ingots", "&7The body."),
                            'E', named(Material.EMERALD_BLOCK, "&fEmerald Block", "&7Flanks the core."),
                            'D', named(Material.DIAMOND, "&fDiamond", "&7The core.")),
                    false, null, null));
        }
    }

    /**
     * Mirrors ShardRecipes' grid parsing so the book always shows the live
     * 'shards.recipe.ingredients' layout and yield from config.yml.
     */
    private RecipeEntry shardEntry() {
        List<String> config = plugin.getSettings().getShardRecipeIngredients();
        List<Material> cells = parseCells(config);
        if (cells.isEmpty()) {
            cells = parseCells(List.of(
                    "ECHO_SHARD:1", "DIAMOND:1", "GOLD_INGOT:1",
                    "AMETHYST_SHARD:2", "GLOWSTONE_DUST:2", "LAPIS_LAZULI:2"));
        }

        Map<Character, ItemStack> ingredients = new LinkedHashMap<>();
        StringBuilder[] rows = {new StringBuilder("   "), new StringBuilder("   "), new StringBuilder("   ")};
        char nextLetter = 'A';
        for (int i = 0; i < Math.min(9, cells.size()); i++) {
            Material material = cells.get(i);
            Character letter = null;
            for (Map.Entry<Character, ItemStack> existing : ingredients.entrySet()) {
                if (existing.getValue().getType() == material) {
                    letter = existing.getKey();
                    break;
                }
            }
            if (letter == null) {
                letter = nextLetter++;
                ingredients.put(letter, named(material, "&f" + pretty(material),
                        "&7Forge ingredient."));
            }
            rows[i / 3].setCharAt(i % 3, letter);
        }

        int yield = Math.max(1, plugin.getSettings().getShardRecipeYield());
        return new RecipeEntry("shard", "Eclipse Shard", ShardItem.createShard(yield),
                new String[]{rows[0].toString(), rows[1].toString(), rows[2].toString()},
                ingredients, false, null, null);
    }

    private static List<Material> parseCells(List<String> entries) {
        List<Material> cells = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            Material material = Material.matchMaterial(parts[0].trim());
            int count = 1;
            boolean bad = material == null || !material.isItem();
            if (!bad && parts.length > 1) {
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    bad = true;
                }
            }
            if (bad || count <= 0) continue;
            for (int i = 0; i < count && cells.size() < 9; i++) {
                cells.add(material);
            }
            if (cells.size() >= 9) break;
        }
        return cells;
    }

    private static String pretty(Material material) {
        String[] words = material.name().toLowerCase().replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
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
        inv.setItem(4, glint(header()));

        // Row 1 - totem + Eclipse gear
        inv.setItem(10, mainItem(player, byKey("totem")));
        inv.setItem(11, mainItem(player, byKey("blade")));
        inv.setItem(12, mainItem(player, byKey("axe")));
        inv.setItem(13, mainItem(player, byKey("bow")));
        inv.setItem(14, mainItem(player, byKey("helmet")));
        inv.setItem(15, mainItem(player, byKey("chestplate")));
        inv.setItem(16, mainItem(player, byKey("leggings")));

        // Row 2 - boots, shard forging, anvil upgrades
        inv.setItem(20, mainItem(player, byKey("boots")));
        inv.setItem(21, mainItem(player, byKey("shard")));
        inv.setItem(22, glint(anvilEntryItem()));

        // Section: config-gated vanilla rebalances
        int slot = 29;
        boolean anyTweak = false;
        for (RecipeEntry entry : entries) {
            if (!entry.key().startsWith("tw_")) continue;
            anyTweak = true;
            if (slot <= 32) {
                inv.setItem(slot++, mainItem(player, entry));
            }
        }
        if (anyTweak) {
            inv.setItem(24, sectionLabel());
        }
        player.openInventory(inv);
    }

    private ItemStack header() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c("&d&l☀ Eclipse Recipes ☾"));
        meta.setLore(List.of(
                c("&7Top rows: &fEclipse gear&7, the &4Eclipse Totem&7,"),
                c("&dshard forging &7and &eanvil upgrades&7."),
                c("&7Bottom row: &6vanilla rebalances &7when enabled."),
                "",
                c("&eClick an item to view its recipe.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack sectionLabel() {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c("&6&lVanilla Rebalances"));
        meta.setLore(List.of(
                c("&7Custom versions of classic recipes,"),
                c("&7enabled server-wide.")));
        item.setItemMeta(meta);
        return item;
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
        ItemStack result = displayResult(player, entry, altView);
        inv.setItem(25, result);
        ItemStack accent = pane(Material.ORANGE_STAINED_GLASS_PANE);
        inv.setItem(16, accent);
        inv.setItem(24, accent);
        inv.setItem(34, accent);

        inv.setItem(37, button(Material.BARRIER, "&c&lBack",
                List.of("&7Return to the recipe list."), "back", key, "main"));

        if (entry.gear()) {
            inv.setItem(40, info(Material.PAPER, "&7How to craft & upgrade",
                    List.of("&7Craft this item, then upgrade it",
                            "&7with &dEclipse Shards &7in an anvil.",
                            "",
                            "&7Use the &eAnvil &7button below.")));
            inv.setItem(43, glint(button(Material.ANVIL, "&e&lAnvil Upgrades",
                    List.of("&7How to upgrade this item's tier."), "open_anvil", key, "detail",
                    current.name())));
        } else if ("shard".equals(entry.key())) {
            long cooldownMinutes = plugin.getSettings().getShardRecipeCooldownMillis() / 60_000L;
            inv.setItem(40, info(Material.PAPER, "&7Forging Eclipse Shards",
                    List.of("&7Craft shards at a crafting table.",
                            "&d1 forge &7= &f" + Math.max(1, plugin.getSettings().getShardRecipeYield()) + " shard(s)",
                            "&7Cooldown: &f" + cooldownMinutes + "m",
                            "&7Lifetime cap: &f" + plugin.getSettings().getShardRecipeLifetimeCap() + " shards",
                            "&7Max owned: &f" + plugin.getSettings().getShardRecipeMaxOwned() + " &7(bank + pockets)",
                            "",
                            "&cForge one at a time - shift-clicking is blocked.")));
        } else if (entry.key().startsWith("tw_")) {
            inv.setItem(40, info(Material.PAPER, "&7Server Rebalance",
                    List.of("&7A custom take on this classic recipe,",
                            "&7rebalanced for Eclipse SMP.",
                            "",
                            "&7Craft it like any vanilla recipe.")));
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
        ItemStack base = displayResult(player, entry, false);
        ItemStack t1 = current == entry.alt() ? ShardItem.withMaterial(base, entry.alt()) : base;
        inv.setItem(20, t1);
        inv.setItem(21, arrow());
        inv.setItem(22, ShardItem.createShard(1));
        inv.setItem(23, arrow());
        inv.setItem(24, ShardItem.withTier(t1, 2));

        int maxTier = plugin.getSettings().getMaxTier();
        for (int tier = 1; tier <= Math.min(maxTier, 5); tier++) {
            inv.setItem(27 + tier, ShardItem.withTier(t1, tier));
        }
        ItemStack bracket = pane(Material.ORANGE_STAINED_GLASS_PANE);
        inv.setItem(27, bracket);
        inv.setItem(33, bracket);

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

    private ItemStack mainItem(Player player, RecipeEntry entry) {
        ItemStack item = displayResult(player, entry, false);
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

    /** The entry's result rebuilt for the viewer's allegiance (and base material when toggled). */
    private ItemStack displayResult(Player player, RecipeEntry entry, boolean altView) {
        String side = sideFor(player);
        ItemStack sided = ShardItem.withSide(entry.result(), side);
        return altView && entry.alt() != null ? ShardItem.withMaterial(sided, entry.alt()) : sided;
    }

    /** The allegiance whose gear this player should see in the recipe book. */
    private String sideFor(Player player) {
        Allegiance allegiance = plugin.getProfileManager().get(player).getAllegiance();
        return allegiance == Allegiance.LUNA ? ShardItem.SIDE_LUNA : ShardItem.SIDE_SOL;
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
        return button(Material.ORANGE_STAINED_GLASS_PANE, "&6➜", List.of(), null, null, null);
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
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack corner = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack inner = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean edge = row == 0 || row == 5 || col == 0 || col == 8;
            inv.setItem(i, edge ? border : inner);
        }
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(45, corner);
        inv.setItem(53, corner);
        return inv;
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /** Magical shimmer for actionable/highlight items. */
    private static ItemStack glint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
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
