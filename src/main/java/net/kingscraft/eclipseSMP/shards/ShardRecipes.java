package net.kingscraft.eclipseSMP.shards;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

public final class ShardRecipes {

    private static final List<String> KEYS = List.of(
            "totem", "blade", "axe", "bow",
            "helmet", "chestplate", "leggings", "boots");

    private ShardRecipes() {
    }

    public static void register(EclipseSMP plugin) {
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
    }

    /** Accepts the diamond piece alone, or both diamond and netherite when enabled. */
    private static RecipeChoice.MaterialChoice gearChoice(Settings settings,
                                                          Material diamond, Material netherite) {
        return settings.isNetheriteEnabled()
                ? new RecipeChoice.MaterialChoice(diamond, netherite)
                : new RecipeChoice.MaterialChoice(diamond);
    }

    public static void unregister(EclipseSMP plugin) {
        for (String name : KEYS) {
            Bukkit.removeRecipe(new NamespacedKey(plugin, "eclipse_" + name));
        }
    }

    private static void registerGear(EclipseSMP plugin, String name, String[] shape,
                                     RecipeChoice.ExactChoice shard, char center,
                                     RecipeChoice.MaterialChoice centerChoice,
                                     ItemStack result) {
        NamespacedKey key = new NamespacedKey(plugin, "eclipse_" + name);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        recipe.setIngredient('S', shard);
        recipe.setIngredient(center, centerChoice);
        plugin.getServer().addRecipe(recipe);
    }
}
