package net.kingscraft.eclipseSMP.shards;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShardRecipes implements Listener {

    private static final List<String> KEYS = List.of(
            "totem", "blade", "axe", "bow",
            "helmet", "chestplate", "leggings", "boots", "shard");

    private static final String SHARD_KEY_NAME = "eclipse_shard";

    private final EclipseSMP plugin;

    public ShardRecipes(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Settings settings = plugin.getSettings();
        RecipeChoice.ExactChoice shard = new RecipeChoice.ExactChoice(ShardItem.createShard(1));

        // Eclipse Totem — 4 shards around a Nether Star.
        registerGear(plugin, "totem", new String[]{"S S", " N ", "S S"},
                shard, 'N', new RecipeChoice.MaterialChoice(Material.NETHER_STAR),
                ShardItem.createTotem());

        // Cross-pattern gear: 4 shards + a diamond or netherite piece.
        // The result is re-materialized to the base item's material and
        // re-tagged to the crafter's allegiance at craft time.
        registerGear(plugin, "blade", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD),
                ShardItem.createBlade(ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "axe", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
                ShardItem.createAxe(ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "bow", new String[]{" S ", "SXS", " S "},
                shard, 'X', new RecipeChoice.MaterialChoice(Material.BOW),
                ShardItem.createBow(ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "helmet", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET),
                ShardItem.createArmor(Material.DIAMOND_HELMET, ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "chestplate", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE),
                ShardItem.createArmor(Material.DIAMOND_CHESTPLATE, ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "leggings", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS),
                ShardItem.createArmor(Material.DIAMOND_LEGGINGS, ShardItem.SIDE_SOL, 1));
        registerGear(plugin, "boots", new String[]{" S ", "SXS", " S "},
                shard, 'X', gearChoice(settings, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS),
                ShardItem.createArmor(Material.DIAMOND_BOOTS, ShardItem.SIDE_SOL, 1));

        if (settings.isShardRecipeEnabled()) {
            registerShard();
        }
    }

    /**
     * Eclipse Shard forging recipe. Shards ARE the economy, so the costs come
     * from {@code shards.recipe.ingredients} (default: a genuine grind — an
     * Ancient City echo shard, diamond, gold, amethyst, glowstone and lapis).
     * Gated by wealth, cooldown and a lifetime cap so stashing shards in
     * chests can't be used to farm them.
     */
    private static final List<String> DEFAULT_INGREDIENTS = List.of(
            "ECHO_SHARD:1", "DIAMOND:1", "GOLD_INGOT:1",
            "AMETHYST_SHARD:2", "GLOWSTONE_DUST:2", "LAPIS_LAZULI:2");

    private void registerShard() {
        List<Material> cells = parseGrid(plugin.getSettings().getShardRecipeIngredients());
        if (cells.isEmpty()) {
            plugin.getLogger().warning("Invalid 'shards.recipe.ingredients' in config.yml — using default shard costs.");
            cells = parseGrid(DEFAULT_INGREDIENTS);
        }

        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        Map<Material, Character> letterByMaterial = new LinkedHashMap<>();
        Map<Character, RecipeChoice> choices = new HashMap<>();
        StringBuilder[] rows = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
        for (int i = 0; i < 9 && i < cells.size(); i++) {
            Material material = cells.get(i);
            Character letter = letterByMaterial.get(material);
            if (letter == null) {
                letter = letters[letterByMaterial.size()];
                letterByMaterial.put(material, letter);
                choices.put(letter, new RecipeChoice.MaterialChoice(material));
            }
            rows[i / 3].append(letter.charValue());
        }

        ShapedRecipe recipe = new ShapedRecipe(shardKey(),
                ShardItem.createShard(Math.max(1, plugin.getSettings().getShardRecipeYield())));
        recipe.shape(trim(rows[0]), trim(rows[1]), trim(rows[2]));
        choices.forEach(recipe::setIngredient);
        plugin.getServer().addRecipe(recipe);
    }

    private static String trim(StringBuilder row) {
        String s = row.toString();
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') end--;
        return s.substring(0, end);
    }

    /**
     * Turns entries like {@code ECHO_SHARD:1} into up to nine grid cells,
     * filled row-major; each cell holds one item of that material.
     */
    private List<Material> parseGrid(List<String> entries) {
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
            if (bad || count <= 0) return List.of();
            for (int i = 0; i < count && cells.size() < 9; i++) {
                cells.add(material);
            }
            if (cells.size() >= 9) break;
        }
        return cells;
    }

    private record Gate(String key, String fallback, Object[] args) {
    }

    /** Blocks the shard preview whenever any craft gate denies the crafter. */
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null || !ShardItem.isShard(result)) return;
        if (!(event.getView().getPlayer() instanceof Player player)
                || gateCheck(player) != null) {
            inv.setResult(null);
        }
    }

    /** Commits cooldown + lifetime counter only on a real, single-item take. */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(shardKey())) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gate gate = gateCheck(player);
        if (gate != null) {
            event.setCancelled(true);
            plugin.getMessages().send(player, gate.key(), gate.fallback(), gate.args());
            return;
        }
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            plugin.getMessages().send(player, "shards.recipe-one-at-a-time",
                    "&cForge shards one at a time.");
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().get(player);
        profile.setLastShardCraft(System.currentTimeMillis());
        profile.setShardCraftedTotal(profile.getShardCraftedTotal()
                + Math.max(1, plugin.getSettings().getShardRecipeYield()));
        plugin.getProfileManager().save(profile);
    }

    /** Null when crafting is allowed, otherwise the denial message to show. */
    private Gate gateCheck(Player player) {
        Settings settings = plugin.getSettings();
        if (!settings.isShardRecipeEnabled()) return null;

        PlayerProfile profile = plugin.getProfileManager().get(player);
        int crafted = profile.getShardCraftedTotal();
        int cap = settings.getShardRecipeLifetimeCap();
        if (crafted >= cap) {
            return new Gate("shards.recipe-cap",
                    "&cYou have reached your lifetime limit of &d{0} &cforged Eclipse Shards.",
                    new Object[]{cap});
        }

        long elapsed = System.currentTimeMillis() - profile.getLastShardCraft();
        long cooldown = settings.getShardRecipeCooldownMillis();
        if (elapsed < cooldown) {
            return new Gate("shards.recipe-cooldown",
                    "&cYou can forge your next Eclipse Shard in &f{0}&c.",
                    new Object[]{formatRemaining(cooldown - elapsed)});
        }

        int maxOwned = settings.getShardRecipeMaxOwned();
        if (ownedShards(player) >= maxOwned) {
            return new Gate("shards.recipe-too-rich",
                    "&cYou own too many Eclipse Shards (&d{0}+&c) to forge more.",
                    new Object[]{maxOwned});
        }
        return null;
    }

    private static String formatRemaining(long millis) {
        long totalSeconds = Math.max(1, (millis + 999) / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    /** Bank + carried shards. */
    private int ownedShards(Player player) {
        int carried = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ShardItem.isShard(item)) carried += item.getAmount();
        }
        return carried + plugin.getProfileManager().get(player).getBank();
    }

    /** Accepts the diamond piece alone, or both diamond and netherite when enabled. */
    private static RecipeChoice.MaterialChoice gearChoice(Settings settings,
                                                          Material diamond, Material netherite) {
        return settings.isNetheriteEnabled()
                ? new RecipeChoice.MaterialChoice(diamond, netherite)
                : new RecipeChoice.MaterialChoice(diamond);
    }

    public void unregister() {
        for (String name : KEYS) {
            Bukkit.removeRecipe(new NamespacedKey(plugin, name));
        }
    }

    private void registerGear(EclipseSMP plugin, String name, String[] shape,
                              RecipeChoice.ExactChoice shard, char center,
                              RecipeChoice.MaterialChoice centerChoice,
                              ItemStack result) {
        NamespacedKey key = new NamespacedKey(plugin, name.startsWith("eclipse_") ? name : "eclipse_" + name);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        recipe.setIngredient('S', shard);
        recipe.setIngredient(center, centerChoice);
        plugin.getServer().addRecipe(recipe);
    }

    private NamespacedKey shardKey() {
        return new NamespacedKey(plugin, SHARD_KEY_NAME);
    }
}
