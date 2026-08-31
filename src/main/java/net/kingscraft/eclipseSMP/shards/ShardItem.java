package net.kingscraft.eclipseSMP.shards;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShardItem {

    public static final String KIND_BLADE = "blade";
    public static final String KIND_AXE = "axe";
    public static final String KIND_BOW = "bow";

    public static final String SIDE_SOL = "sol";
    public static final String SIDE_LUNA = "luna";

    private ShardItem() {
    }

    private static EclipseSMP plugin() {
        return EclipseSMP.getInstance();
    }

    public static NamespacedKey keyShard() {
        return plugin().getSettings().key("shard");
    }

    public static NamespacedKey keyTotem() {
        return plugin().getSettings().key("totem");
    }

    public static NamespacedKey keyWeapon() {
        return plugin().getSettings().key("weapon");
    }

    public static NamespacedKey keyArmor() {
        return plugin().getSettings().key("armor");
    }

    public static NamespacedKey keyTier() {
        return plugin().getSettings().key("tier");
    }

    public static NamespacedKey keySide() {
        return plugin().getSettings().key("side");
    }

    public static Material shardMaterial() {
        Material mat = Material.matchMaterial(plugin().getSettings().getShardMaterial());
        return mat != null ? mat : Material.AMETHYST_SHARD;
    }

    // ---- shard item -------------------------------------------------
    public static ItemStack createShard(int amount) {
        ItemStack item = new ItemStack(shardMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&d&lEclipse Shard"));
        meta.setCustomModelData(plugin().getSettings().getShardModelData());
        meta.setLore(List.of(
                color("&7A shard of the Blood Eclipse,"),
                color("&7forged when the sun meets the moon."),
                color("&dUsed to craft Eclipse gear.")
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(keyShard(), PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isShard(ItemStack item) {
        if (item == null || item.getType() != shardMaterial() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyShard(), PersistentDataType.STRING);
    }

    // ---- totem ------------------------------------------------------
    public static ItemStack createTotem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&4&lEclipse Totem"));
        meta.setLore(List.of(
                color("&7Right-click to summon the"),
                color("&4Blood Eclipse &7and claim its shards.")
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(keyTotem(), PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTotem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyTotem(), PersistentDataType.STRING);
    }

    // ---- gear -------------------------------------------------------
    public static ItemStack createBlade(Material base, String side, int tier) {
        return reforge(KIND_BLADE, base, side, tier);
    }

    public static ItemStack createBlade(String side, int tier) {
        return createBlade(Material.DIAMOND_SWORD, side, tier);
    }

    public static ItemStack createAxe(Material base, String side, int tier) {
        return reforge(KIND_AXE, base, side, tier);
    }

    public static ItemStack createAxe(String side, int tier) {
        return createAxe(Material.DIAMOND_AXE, side, tier);
    }

    public static ItemStack createBow(String side, int tier) {
        return reforge(KIND_BOW, Material.BOW, side, tier);
    }

    public static ItemStack createArmor(Material material, String side, int tier) {
        return reforge(null, material, side, tier);
    }

    /** Rebuilds gear from scratch for a given side and tier (tier-scaled stats included). */
    private static ItemStack reforge(String kind, Material material, String side, int tier) {
        boolean armor = kind == null;
        Settings settings = plugin().getSettings();
        int maxTier = settings.getMaxTier();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String sideColor = SIDE_SOL.equals(side) ? "&6" : "&8";
        String icon = SIDE_SOL.equals(side) ? "☀" : "☾";
        String sideName = SIDE_SOL.equals(side) ? "Sol" : "Luna";
        String label = armor ? pieceLabel(material) : kindLabel(kind);
        meta.setDisplayName(color(sideColor + icon + " &l" + sideName + " Eclipse " + label));

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Forged in the Blood Eclipse."));
        lore.addAll(abilityLines(kind, armor, side));
        if (KIND_BLADE.equals(kind)) {
            lore.add(color("&a+" + format(bladeDamagePerTier() * tier) + " Attack Damage"));
        } else if (KIND_AXE.equals(kind)) {
            lore.add(color("&a+" + format(axeDamagePerTier() * tier) + " Attack Damage"));
        } else if (KIND_BOW.equals(kind)) {
            lore.add(color("&a+" + format(bowBonusPerTier() * tier) + " Bow Damage"));
        } else {
            lore.add(color("&a+" + tier + " Armor &7&o(+" + format(0.5 * tier) + " Toughness)"));
        }
        lore.add(color("&4Tier: &c" + tier));
        meta.setLore(lore);

        applyEnchants(meta, kind, armor, material, tier, maxTier);
        applyAttributes(meta, kind, armor, material, tier);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (armor) {
            pdc.set(keyArmor(), PersistentDataType.STRING, "true");
        } else {
            pdc.set(keyWeapon(), PersistentDataType.STRING, kind);
        }
        pdc.set(keyTier(), PersistentDataType.INTEGER, tier);
        pdc.set(keySide(), PersistentDataType.STRING, side);
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> abilityLines(String kind, boolean armor, String side) {
        boolean sol = SIDE_SOL.equals(side);
        List<String> lines = new ArrayList<>();
        if (armor) {
            if (sol) {
                lines.add(color("&cSet bonus: &fStrength, Regen & Speed in sunlight"));
                lines.add(color("&cFull set: &fno night weakness or slowness"));
            } else {
                lines.add(color("&8Set bonus: &fStrength & Speed in darkness"));
                lines.add(color("&8Full set: &fno daylight damage penalty"));
            }
        } else if (KIND_BLADE.equals(kind)) {
            lines.add(color(sol ? "&cIgnites enemies & bonus damage in sunlight"
                    : "&8Lifesteals on hit in darkness"));
        } else if (KIND_AXE.equals(kind)) {
            lines.add(color(sol ? "&cHeavy damage — slows enemies"
                    : "&8Backstab crits — slows enemies"));
        } else {
            lines.add(color(sol ? "&cArrows ignite in sunlight & the eclipse"
                    : "&8Arrows crit in darkness, slow in the eclipse"));
        }
        return lines;
    }

    private static void applyEnchants(ItemMeta meta, String kind, boolean armor,
                                      Material material, int tier, int maxTier) {
        if (armor) {
            meta.addEnchant(Enchantment.PROTECTION, Math.min(4, 1 + tier), true);
            meta.addEnchant(Enchantment.UNBREAKING, Math.min(3, 1 + tier), true);
            if (tier >= maxTier) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
                switch (material) {
                    case DIAMOND_HELMET, NETHERITE_HELMET -> {
                        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                        meta.addEnchant(Enchantment.RESPIRATION, 3, true);
                    }
                    case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE ->
                            meta.addEnchant(Enchantment.THORNS, 3, true);
                    case DIAMOND_BOOTS, NETHERITE_BOOTS -> {
                        meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                        meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                    }
                    default -> {
                    }
                }
            }
        } else if (KIND_BLADE.equals(kind)) {
            meta.addEnchant(Enchantment.SHARPNESS, Math.min(5, 2 + tier), true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, Math.min(2, tier), true);
            if (tier >= maxTier) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.addEnchant(Enchantment.LOOTING, 3, true);
            }
        } else if (KIND_AXE.equals(kind)) {
            meta.addEnchant(Enchantment.SHARPNESS, Math.min(5, 2 + tier), true);
            meta.addEnchant(Enchantment.KNOCKBACK, Math.min(2, tier), true);
            if (tier >= maxTier) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
        } else if (KIND_BOW.equals(kind)) {
            meta.addEnchant(Enchantment.POWER, Math.min(5, 2 + tier), true);
            if (tier >= maxTier) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
        }
    }

    private static void applyAttributes(ItemMeta meta, String kind, boolean armor,
                                        Material material, int tier) {
        // NOTE: writing any custom attribute modifier REPLACES the item's vanilla
        // stat block, so every reforge must re-add the vanilla base values itself.
        if (KIND_BLADE.equals(kind)) {
            addAttr(meta, Attribute.ATTACK_DAMAGE,
                    baseWeaponDamage(material) + bladeDamagePerTier() * tier,
                    EquipmentSlotGroup.MAINHAND, "damage_blade");
        } else if (KIND_AXE.equals(kind)) {
            addAttr(meta, Attribute.ATTACK_DAMAGE,
                    baseWeaponDamage(material) + axeDamagePerTier() * tier,
                    EquipmentSlotGroup.MAINHAND, "damage_axe");
        } else if (armor) {
            EquipmentSlotGroup slot = switch (material) {
                case DIAMOND_HELMET, NETHERITE_HELMET -> EquipmentSlotGroup.HEAD;
                case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> EquipmentSlotGroup.CHEST;
                case DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> EquipmentSlotGroup.LEGS;
                default -> EquipmentSlotGroup.FEET;
            };
            addAttr(meta, Attribute.ARMOR, baseArmor(material) + tier, slot, "armor");
            addAttr(meta, Attribute.ARMOR_TOUGHNESS,
                    baseToughness(material) + 0.5 * tier, slot, "toughness");
        }
    }

    /** Vanilla armor points of the base piece (diamond == netherite). */
    private static double baseArmor(Material material) {
        return switch (material) {
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 8;
            case DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> 6;
            default -> 3;
        };
    }

    /** Vanilla toughness of the base piece: diamond 2, netherite 3. */
    private static double baseToughness(Material material) {
        return isNetherite(material) ? 3 : 2;
    }

    /** Vanilla attack-damage modifier of the base weapon (diamond sword +6, netherite +7, etc.). */
    private static double baseWeaponDamage(Material material) {
        boolean netherite = isNetherite(material);
        if (material.name().endsWith("_AXE")) return netherite ? 9 : 8;
        return netherite ? 7 : 6;
    }

    private static boolean isNetherite(Material material) {
        return switch (material) {
            case NETHERITE_SWORD, NETHERITE_AXE, NETHERITE_HELMET,
                 NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> true;
            default -> false;
        };
    }

    private static void addAttr(ItemMeta meta, Attribute attribute, double amount,
                                EquipmentSlotGroup slot, String keyName) {
        NamespacedKey key = plugin().getSettings().key(keyName);
        meta.addAttributeModifier(attribute, new AttributeModifier(
                key, amount, AttributeModifier.Operation.ADD_NUMBER, slot));
    }

    private static double bladeDamagePerTier() {
        return plugin().getSettings().getBladeBonusPerTier();
    }

    private static double axeDamagePerTier() {
        return plugin().getSettings().getAxeBonusPerTier();
    }

    private static double bowBonusPerTier() {
        return plugin().getSettings().getBowBonusPerTier();
    }

    private static String pieceLabel(Material material) {
        String name = material.name();
        String piece = name.substring(name.lastIndexOf('_') + 1).toLowerCase();
        return piece.substring(0, 1).toUpperCase() + piece.substring(1);
    }

    private static String kindLabel(String kind) {
        if (KIND_BLADE.equals(kind)) return "Blade";
        if (KIND_AXE.equals(kind)) return "Axe";
        return "Bow";
    }

    // ---- gear inspection -------------------------------------------
    public static String gearKind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyWeapon(), PersistentDataType.STRING);
    }

    public static boolean isArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyArmor(), PersistentDataType.STRING);
    }

    public static boolean isGear(ItemStack item) {
        return isArmor(item) || gearKind(item) != null;
    }

    public static String sideOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keySide(), PersistentDataType.STRING);
    }

    public static int tierOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer tier = pdc.get(keyTier(), PersistentDataType.INTEGER);
        return tier == null ? 0 : tier;
    }

    /** Rebuilds an item with a new tier, keeping the side and any player-added enchants. */
    public static ItemStack withTier(ItemStack item, int newTier) {
        String kind = gearKind(item);
        boolean armor = isArmor(item);
        String side = sideOf(item);
        if (kind == null && !armor) return item;
        if (side == null) side = SIDE_SOL;
        Map<Enchantment, Integer> extra = extraEnchants(item, kind, armor);
        ItemStack built = reforge(kind, item.getType(), side, newTier);
        if (!extra.isEmpty()) {
            ItemMeta meta = built.getItemMeta();
            for (Map.Entry<Enchantment, Integer> e : extra.entrySet()) {
                if (meta.getEnchantLevel(e.getKey()) < e.getValue()) {
                    meta.addEnchant(e.getKey(), e.getValue(), true);
                }
            }
            built.setItemMeta(meta);
        }
        return built;
    }

    /** Rebuilds an item on the other allegiance side, keeping the tier. */
    public static ItemStack withSide(ItemStack item, String side) {
        String kind = gearKind(item);
        boolean armor = isArmor(item);
        if (kind == null && !armor) return item;
        Map<Enchantment, Integer> extra = extraEnchants(item, kind, armor);
        ItemStack built = reforge(kind, item.getType(), side, tierOf(item));
        if (!extra.isEmpty()) {
            ItemMeta meta = built.getItemMeta();
            for (Map.Entry<Enchantment, Integer> e : extra.entrySet()) {
                if (meta.getEnchantLevel(e.getKey()) < e.getValue()) {
                    meta.addEnchant(e.getKey(), e.getValue(), true);
                }
            }
            built.setItemMeta(meta);
        }
        return built;
    }

    /** Rebuilds gear on another base material, keeping the kind, side, tier and any player-added enchants. */
    public static ItemStack withMaterial(ItemStack item, Material material) {
        String kind = gearKind(item);
        boolean armor = isArmor(item);
        if (kind == null && !armor) return item;
        if (material == null || item.getType() == material) return item;
        String side = sideOf(item);
        if (side == null) side = SIDE_SOL;
        Map<Enchantment, Integer> extra = extraEnchants(item, kind, armor);
        ItemStack built = reforge(kind, material, side, tierOf(item));
        if (!extra.isEmpty()) {
            ItemMeta meta = built.getItemMeta();
            for (Map.Entry<Enchantment, Integer> e : extra.entrySet()) {
                if (meta.getEnchantLevel(e.getKey()) < e.getValue()) {
                    meta.addEnchant(e.getKey(), e.getValue(), true);
                }
            }
            built.setItemMeta(meta);
        }
        return built;
    }

    private static Map<Enchantment, Integer> extraEnchants(ItemStack item, String kind, boolean armor) {
        Map<Enchantment, Integer> extra = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return extra;
        Set<Enchantment> builtIn = builtInEnchants(kind, armor);
        for (Map.Entry<Enchantment, Integer> e : item.getItemMeta().getEnchants().entrySet()) {
            if (!builtIn.contains(e.getKey())) {
                extra.put(e.getKey(), e.getValue());
            }
        }
        return extra;
    }

    private static Set<Enchantment> builtInEnchants(String kind, boolean armor) {
        if (armor) {
            return Set.of(Enchantment.PROTECTION, Enchantment.UNBREAKING, Enchantment.MENDING,
                    Enchantment.AQUA_AFFINITY, Enchantment.RESPIRATION, Enchantment.THORNS,
                    Enchantment.FEATHER_FALLING, Enchantment.DEPTH_STRIDER);
        }
        if (KIND_BLADE.equals(kind)) {
            return Set.of(Enchantment.SHARPNESS, Enchantment.FIRE_ASPECT, Enchantment.MENDING,
                    Enchantment.LOOTING);
        }
        if (KIND_AXE.equals(kind)) {
            return Set.of(Enchantment.SHARPNESS, Enchantment.KNOCKBACK, Enchantment.MENDING);
        }
        return Set.of(Enchantment.POWER, Enchantment.FLAME, Enchantment.INFINITY, Enchantment.MENDING);
    }

    private static String format(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
    }

    private static String color(String s) {
        return Settings.color(s);
    }
}
