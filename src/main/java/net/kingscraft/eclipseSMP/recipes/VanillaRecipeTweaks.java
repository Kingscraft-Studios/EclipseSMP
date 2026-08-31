package net.kingscraft.eclipseSMP.recipes;

import net.kingscraft.eclipseSMP.EclipseSMP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional rebalances of vanilla crafting, each toggleable in config.yml:
 * - Golden apple: 4 gold ingots instead of 8.
 * - Cobweb: craftable from 5 string (X shape).
 * - Anvil: 7 iron ingots total instead of 31 iron worth of blocks+ingots.
 * - Totem of Undying: diamond core, emerald block sides, gold body.
 */
public final class VanillaRecipeTweaks {

    private final EclipseSMP plugin;
    private final List<NamespacedKey> added = new ArrayList<>();

    public VanillaRecipeTweaks(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (plugin.getSettings().isGoldenAppleTweakEnabled()) {
            removeVanilla("golden_apple");
            ShapedRecipe recipe = new ShapedRecipe(key("tweaks_golden_apple"),
                    new ItemStack(Material.GOLDEN_APPLE));
            recipe.shape(" G ", "GAG", " G ");
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('A', Material.APPLE);
            add(recipe);
        }
        if (plugin.getSettings().isCobwebTweakEnabled()) {
            // No vanilla recipe exists; nothing to remove.
            ShapedRecipe recipe = new ShapedRecipe(key("tweaks_cobweb"),
                    new ItemStack(Material.COBWEB));
            recipe.shape("S S", " S ", "S S");
            recipe.setIngredient('S', Material.STRING);
            add(recipe);
        }
        if (plugin.getSettings().isAnvilTweakEnabled()) {
            removeVanilla("anvil");
            // Same layout as vanilla but every block becomes an ingot: 7 iron total.
            ShapedRecipe recipe = new ShapedRecipe(key("tweaks_anvil"),
                    new ItemStack(Material.ANVIL));
            recipe.shape("III", " I ", "III");
            recipe.setIngredient('I', Material.IRON_INGOT);
            add(recipe);
        }
        if (plugin.getSettings().isTotemTweakEnabled()) {
            // Diamond core, emerald blocks flanking it, gold body everywhere else.
            ShapedRecipe recipe = new ShapedRecipe(key("tweaks_totem"),
                    new ItemStack(Material.TOTEM_OF_UNDYING));
            recipe.shape("GEG", "GDG", "GGG");
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('E', Material.EMERALD_BLOCK);
            recipe.setIngredient('D', Material.DIAMOND);
            add(recipe);
        }
    }

    public void unregister() {
        for (NamespacedKey key : added) {
            Bukkit.removeRecipe(key);
        }
        added.clear();
    }

    private void add(ShapedRecipe recipe) {
        plugin.getServer().addRecipe(recipe);
        added.add(recipe.getKey());
    }

    private void removeVanilla(String name) {
        Bukkit.removeRecipe(NamespacedKey.minecraft(name));
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
