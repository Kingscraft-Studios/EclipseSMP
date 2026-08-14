package net.kingscraft.eclipseSMP.abilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public void start(UUID uuid, String key, long millis) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis() + millis);
    }

    public boolean has(UUID uuid, String key) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return false;
        Long end = map.get(key);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            map.remove(key);
            return false;
        }
        return true;
    }

    public long remainingMillis(UUID uuid, String key) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return 0;
        Long end = map.get(key);
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void remove(UUID uuid, String key) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map != null) map.remove(key);
    }

    public void removeAll(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
