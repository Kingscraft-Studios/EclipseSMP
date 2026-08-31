package net.kingscraft.eclipseSMP;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Persists internal timers across restarts in save.yml. */
public final class SaveStore {

    private final EclipseSMP plugin;
    private final File file;
    private final YamlConfiguration yaml = new YamlConfiguration();

    public SaveStore(EclipseSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "save.yml");
        if (file.exists()) {
            try {
                yaml.load(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not read save.yml: " + e.getMessage());
            }
        }
    }

    public long getLong(String path, long def) {
        return yaml.getLong(path, def);
    }

    /** Stores a timestamp/flag and immediately flushes it to disk. */
    public void set(String path, long value) {
        yaml.set(path, value);
        flush();
    }

    private void flush() {
        try {
            File tmp = new File(plugin.getDataFolder(), "save.yml.tmp");
            yaml.save(tmp);
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write save.yml: " + e.getMessage());
        }
    }
}
