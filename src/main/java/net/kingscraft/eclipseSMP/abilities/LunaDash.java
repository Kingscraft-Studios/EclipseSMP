package net.kingscraft.eclipseSMP.abilities;

import net.kingscraft.eclipseSMP.EclipseSMP;
import net.kingscraft.eclipseSMP.Settings;
import net.kingscraft.eclipseSMP.allegiance.Allegiance;
import net.kingscraft.eclipseSMP.allegiance.PlayerProfile;
import net.kingscraft.eclipseSMP.environment.LightState;
import net.kingscraft.eclipseSMP.environment.SunlightDetector;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LunaDash implements Listener {

    private final EclipseSMP plugin;
    private final Map<UUID, Deque<Long>> presses = new ConcurrentHashMap<>();
    private final Set<Integer> hiddenIds = ConcurrentHashMap.newKeySet();
    private PacketListenerAbstract equipmentListener;

    public LunaDash(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (equipmentListener == null) {
            equipmentListener = new EquipmentHideListener();
            PacketEvents.getAPI().getEventManager().registerListener(equipmentListener);
        }
    }

    public void shutdown() {
        if (equipmentListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(equipmentListener);
            equipmentListener = null;
        }
        hiddenIds.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        Settings settings = plugin.getSettings();

        if (!settings.isLunaDashEnabled()) return;
        if (!settings.isWorldEnabled(player.getWorld().getName())) return;
        PlayerProfile profile = plugin.getProfileManager().get(player);
        if (!profile.hasAllegiance() || profile.getAllegiance() != Allegiance.LUNA) return;

        boolean eclipse = plugin.getEclipseManager().isActive();
        LightState state = eclipse ? LightState.ECLIPSE : SunlightDetector.resolve(player);
        if (state != LightState.DARKNESS && state != LightState.ECLIPSE) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (plugin.getCooldownManager().has(uuid, "dash")) {
            long remaining = plugin.getCooldownManager().remainingMillis(uuid, "dash") / 1000;
            plugin.getMessages().actionBar(player, "dash.ready", "&8☾ &7Dash ready in &f{0}&7s", remaining);
            return;
        }

        Deque<Long> list = presses.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long window = settings.getLunaDashWindowMs();
        while (!list.isEmpty() && now - list.peekFirst() > window) {
            list.pollFirst();
        }

        boolean dashed = false;
        Long last = list.peekLast();
        if (last != null) {
            long gap = now - last;
            if (gap >= settings.getLunaDashMicroCooldownMs() && gap <= window) {
                dashed = true;
            }
        }
        list.addLast(now);

        if (dashed) {
            list.clear();
            doDash(player, settings);
        }
    }

    private void doDash(Player player, Settings settings) {
        UUID uuid = player.getUniqueId();
        plugin.getCooldownManager().start(uuid, "dash", settings.getLunaDashCooldownSeconds() * 1000L);

        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize().multiply(settings.getLunaDashPower());
        player.setVelocity(dir.setY(settings.getLunaDashHeight()));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                settings.getLunaDashInvisibilitySeconds() * 20,
                0));

        if (settings.isLunaDashHideEquipment()) {
            hideEquipment(player, settings);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> restoreEquipment(player, settings),
                    settings.getLunaDashInvisibilitySeconds() * 20L);
        }

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.3, 0.3, 0.3, 0.02);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 40, 0.5, 0.5, 0.5, 0.2);

        plugin.getMessages().actionBar(player, "dash.fired",
                "&8☾ &7Vanish dash &fready in &7{0}s", settings.getLunaDashCooldownSeconds());
    }

    private void hideEquipment(Player player, Settings settings) {
        hiddenIds.add(player.getEntityId());
        sendEquipmentToViewers(player, emptyEquipment(), settings.isLunaDashHideEquipmentSelf());
    }

    private void restoreEquipment(Player player, Settings settings) {
        hiddenIds.remove(player.getEntityId());
        if (!player.isOnline()) return;
        sendEquipmentToViewers(player, realEquipment(player), settings.isLunaDashHideEquipmentSelf());
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private void sendEquipmentToViewers(Player player, List<Equipment> equipment, boolean includeSelf) {
        WrapperPlayServerEntityEquipment packet =
                new WrapperPlayServerEntityEquipment(player.getEntityId(), equipment);
        for (Player viewer : player.getWorld().getPlayers()) {
            if (viewer.getUniqueId().equals(player.getUniqueId()) && !includeSelf) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer, packet);
        }
    }

    private List<Equipment> emptyEquipment() {
        List<Equipment> equipment = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HELMET, EquipmentSlot.CHEST_PLATE,
                EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS,
                EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND}) {
            equipment.add(new Equipment(slot, ItemStack.EMPTY));
        }
        return equipment;
    }

    private List<Equipment> realEquipment(Player player) {
        List<Equipment> equipment = new ArrayList<>();
        equipment.add(new Equipment(EquipmentSlot.HELMET, toPacketEvents(player.getInventory().getHelmet())));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, toPacketEvents(player.getInventory().getChestplate())));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, toPacketEvents(player.getInventory().getLeggings())));
        equipment.add(new Equipment(EquipmentSlot.BOOTS, toPacketEvents(player.getInventory().getBoots())));
        equipment.add(new Equipment(EquipmentSlot.MAIN_HAND, toPacketEvents(player.getInventory().getItemInMainHand())));
        equipment.add(new Equipment(EquipmentSlot.OFF_HAND, toPacketEvents(player.getInventory().getItemInOffHand())));
        return equipment;
    }

    private static ItemStack toPacketEvents(org.bukkit.inventory.ItemStack bukkit) {
        if (bukkit == null || bukkit.getType().isAir()) return ItemStack.EMPTY;
        return SpigotConversionUtil.fromBukkitItemStack(bukkit);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        presses.remove(event.getPlayer().getUniqueId());
        hiddenIds.remove(event.getPlayer().getEntityId());
    }

    private final class EquipmentHideListener extends PacketListenerAbstract {
        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;
            Player viewer = event.getPlayer();
            WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
            if (viewer == null) return;
            boolean self = viewer.getEntityId() == packet.getEntityId();
            if (self && !plugin.getSettings().isLunaDashHideEquipmentSelf()) return;
            if (hiddenIds.contains(packet.getEntityId())) {
                packet.setEquipment(emptyEquipment());
                event.markForReEncode(true);
            }
        }
    }
}
