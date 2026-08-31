package net.kingscraft.eclipseSMP.shards;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShardManager implements Listener {

    private record Hit(double damage, long time) {
        Hit plus(double extra) {
            return new Hit(damage + extra, time);
        }
    }

    private final EclipseSMP plugin;
    private final Map<UUID, Map<UUID, Hit>> attribution = new ConcurrentHashMap<>();
    private long droppedThisEclipse;

    public ShardManager(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!plugin.getSettings().isWorldEnabled(victim.getWorld().getName())) return;
        if (attacker.equals(victim)) return;

        Map<UUID, Hit> hits = attribution.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>());
        prune(victim.getUniqueId(), System.currentTimeMillis());
        Hit existing = hits.get(attacker.getUniqueId());
        Hit merged = existing == null
                ? new Hit(event.getFinalDamage(), System.currentTimeMillis())
                : existing.plus(event.getFinalDamage());
        hits.put(attacker.getUniqueId(), merged);
    }

    /** Drops combat tags older than the configured window so stale hits can never steal kill credit. */
    private void prune(UUID victim, long now) {
        Map<UUID, Hit> hits = attribution.get(victim);
        if (hits == null) return;
        long window = plugin.getSettings().getCombatTagMillis();
        hits.values().removeIf(hit -> now - hit.time() > window);
    }

    /** Resolves the attacking player, following projectile shots back to their shooter. */
    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        Player victim = event.getEntity();
        if (!plugin.getSettings().isWorldEnabled(victim.getWorld().getName())) return;

        boolean eclipse = plugin.getEclipseManager().isActive();
        Location loc = victim.getLocation();

        // 1) Carried shards drop on the ground on ANY death.
        int dropped = dropCarriedShards(victim, loc);
        if (eclipse) droppedThisEclipse += dropped;

        // 2) Bank penalty / transfer.
        Player attacker = topAttacker(victim);
        boolean killedByPlayer = attacker != null && attacker.isOnline();
        PlayerProfile victimProfile = plugin.getProfileManager().get(victim);

        if (eclipse && killedByPlayer) {
            int reward = computeReward(attacker, victim);
            victimProfile.lose(reward);
            giveShards(attacker, reward);
            droppedThisEclipse += reward;

            PlayerProfile killerProfile = plugin.getProfileManager().get(attacker);
            killerProfile.addEarned(reward);
            killerProfile.addKill();
            plugin.getProfileManager().save(killerProfile);

            String symbol = killerProfile.hasAllegiance() ? killerProfile.getAllegiance().getSymbol() : "";
            plugin.getMessages().broadcast("shards.pvp-claim",
                    "&c☀☾ &f{0} &7claimed &d{1} Eclipse Shards &7from &f{2} &7{3}",
                    attacker.getName(), reward, victim.getName(), symbol);
        } else if (!eclipse && killedByPlayer) {
            int loss = plugin.getSettings().getNonEclipsePvpLoss();
            victimProfile.lose(loss);
            giveShards(attacker, loss);

            PlayerProfile killerProfile = plugin.getProfileManager().get(attacker);
            killerProfile.addKill();
            plugin.getProfileManager().save(killerProfile);

            plugin.getMessages().send(attacker, "shards.pvp-loss-killer",
                    "&aYou claimed &d{0} Eclipse Shard &afrom &f{1}&a.", loss, victim.getName());
            plugin.getMessages().send(victim, "shards.pvp-loss-victim",
                    "&c{0} &7claimed &d{1} &7Eclipse Shard from your bank.", attacker.getName(), loss);
        } else {
            // Natural death (or the killer is no longer online): bank loss, shard dropped.
            int loss = plugin.getSettings().getNaturalDeathLoss();
            victimProfile.lose(loss);
            victim.getWorld().dropItemNaturally(loc, ShardItem.createShard(loss));
            plugin.getMessages().send(victim, "shards.natural-death",
                    "&cYou lost &d{0} Eclipse Shard &cfrom your bank. It dropped at your location.", loss);
        }

        plugin.getProfileManager().save(victimProfile);
        checkElimination(victim, victimProfile);
        attribution.remove(victim.getUniqueId());
    }

    private int dropCarriedShards(Player victim, Location loc) {
        int percent = plugin.getSettings().getCarriedDropPercent();
        int lost = 0;

        for (ItemStack item : victim.getInventory().getContents()) {
            if (item == null || !ShardItem.isShard(item)) continue;
            int count = item.getAmount();
            int dropped = (int) Math.floor(count * percent / 100.0);
            if (dropped <= 0) continue;
            lost += dropped;
            ItemStack drop = item.clone();
            drop.setAmount(dropped);
            victim.getWorld().dropItemNaturally(loc, drop);
            item.setAmount(count - dropped);
        }
        return lost;
    }

    private void checkElimination(Player victim, PlayerProfile profile) {
        if (!plugin.getSettings().isEliminationEnabled()) return;
        if (profile.getBank() > plugin.getSettings().getEliminateAt()) return;
        if (profile.getBannedUntil() > 0) return;

        long until = System.currentTimeMillis() + plugin.getSettings().getEliminationBanMillis();
        profile.setBannedUntil(until);
        plugin.getProfileManager().save(profile);

        long minutes = plugin.getSettings().getEliminationBanMillis() / 60_000;
        plugin.getMessages().broadcast("elimination.broadcast",
                "&4☀☾ &c{0} &4was eliminated! &7Their shard bank hit the limit. Banned for &f{1} &7minutes.",
                victim.getName(), minutes);
        victim.kickPlayer(plugin.getMessages().msg("elimination.kick",
                "&4You were eliminated!\n&7Your Eclipse Shard bank dropped too low.\n&cYou may rejoin in {0} minutes.",
                minutes));
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getSettings().isEliminationEnabled()) return;
        PlayerProfile profile = plugin.getProfileManager().getFresh(event.getPlayer().getUniqueId());
        long until = profile.getBannedUntil();
        if (until <= 0) return;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) return;

        long minutes = Math.max(1, (remaining / 60_000) + 1);
        event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                plugin.getMessages().msg("elimination.login-kick",
                        "&4You were eliminated!\n&cYou may rejoin in {0} minutes.", minutes));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getProfileManager().get(player);
        long until = profile.getBannedUntil();
        if (until <= 0) return;

        long remaining = until - System.currentTimeMillis();
        if (remaining > 0) {
            long minutes = Math.max(1, (remaining / 60_000) + 1);
            player.kickPlayer(plugin.getMessages().msg("elimination.still-banned",
                    "&cYou may rejoin in {0} minutes.", minutes));
            return;
        }

        profile.setBannedUntil(0);
        profile.setBank(plugin.getSettings().getEliminationResetTo());
        plugin.getProfileManager().save(profile);
        plugin.getMessages().send(player, "elimination.ban-ended",
                "&aYour elimination ban has ended. Your shard bank was reset to &d{0}&a.",
                plugin.getSettings().getEliminationResetTo());
    }

    private Player topAttacker(Player victim) {
        prune(victim.getUniqueId(), System.currentTimeMillis());
        Map<UUID, Hit> map = attribution.get(victim.getUniqueId());
        if (map == null || map.isEmpty()) return null;
        UUID top = map.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingDouble(Hit::damage)))
                .map(Map.Entry::getKey)
                .orElse(null);
        return top == null ? null : plugin.getServer().getPlayer(top);
    }

    private int computeReward(Player attacker, Player victim) {
        Settings settings = plugin.getSettings();
        int reward = settings.getShardBaseDrop();
        reward += settings.getShardBonusPerGear() * gearLevel(victim);

        PlayerProfile ap = plugin.getProfileManager().get(attacker);
        PlayerProfile vp = plugin.getProfileManager().get(victim);
        if (ap.hasAllegiance() && vp.hasAllegiance() && ap.getAllegiance() != vp.getAllegiance()) {
            reward += settings.getShardAlignmentBonus();
        }
        return Math.max(1, Math.min(reward, settings.getShardMaxDrop()));
    }

    public void giveShards(Player player, int amount) {
        ItemStack stack = ShardItem.createShard(amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
        }
    }

    /** Moves every carried Eclipse Shard into the safe bank. Returns the amount banked. */
    public int depositShards(Player player) {
        return depositShards(player, Integer.MAX_VALUE);
    }

    /** Moves up to {@code maxAmount} carried shards into the bank. */
    public int depositShards(Player player, int maxAmount) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        int deposited = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !ShardItem.isShard(item)) continue;
            int take = Math.min(item.getAmount(), maxAmount - deposited);
            if (take <= 0) break;
            item.setAmount(item.getAmount() - take);
            deposited += take;
        }
        if (deposited > 0) {
            profile.addBank(deposited);
            plugin.getProfileManager().save(profile);
        }
        return deposited;
    }

    /** Withdraws shards from the bank into the inventory. Returns false if insufficient.
     *  The bank is allowed to drop toward the elimination limit (never below it). */
    public boolean withdrawShards(Player player, int amount) {
        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (!profile.removeBank(amount, plugin.getSettings().getEliminateAt())) return false;
        giveShards(player, amount);
        plugin.getProfileManager().save(profile);
        return true;
    }

    /** Total equipped eclipse armor pieces + eclipse weapon tier. */
    public int gearLevel(Player player) {
        int level = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (ShardItem.isArmor(item)) level++;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (ShardItem.gearKind(hand) != null) level += ShardItem.tierOf(hand);
        return level;
    }

    /** Damage reduction for wearing eclipse armor, active only in darkness/eclipse.
     *  Scales with the SUM of equipped armor tiers, so higher-tier gear protects more. */
    public double armorReduction(Player player) {
        int totalTier = armorTotalTier(player);
        if (totalTier == 0) return 0;
        return Math.min(0.5, totalTier * plugin.getSettings().getArmorReductionPerTier());
    }

    /** Sum of the tiers of all equipped eclipse armor pieces. */
    public int armorTotalTier(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (ShardItem.isArmor(item)) total += ShardItem.tierOf(item);
        }
        return total;
    }

    public int armorSetPieces(Player player) {
        int pieces = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (ShardItem.isArmor(item)) pieces++;
        }
        return pieces;
    }

    /** Flat damage bonus of an eclipse weapon based on its tier. */
    public double weaponDamageBonus(ItemStack item) {
        Settings settings = plugin.getSettings();
        String kind = ShardItem.gearKind(item);
        int tier = ShardItem.tierOf(item);
        if (kind == null || tier <= 0) return 0;
        return switch (kind) {
            case ShardItem.KIND_BLADE -> settings.getBladeBonusPerTier() * tier;
            case ShardItem.KIND_AXE -> settings.getAxeBonusPerTier() * tier;
            case ShardItem.KIND_BOW -> settings.getBowBonusPerTier() * tier;
            default -> 0;
        };
    }

    public long getDroppedThisEclipse() {
        return droppedThisEclipse;
    }

    public void onEclipseStart() {
        attribution.clear();
        droppedThisEclipse = 0;
    }

    public void onEclipseEnd() {
        attribution.clear();
    }
}
