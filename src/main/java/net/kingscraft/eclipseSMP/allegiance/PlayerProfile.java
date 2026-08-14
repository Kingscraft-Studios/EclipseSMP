package net.kingscraft.eclipseSMP.allegiance;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

public final class PlayerProfile {

    private final UUID uuid;
    private Allegiance allegiance;
    private int bank;
    private int totalEarned;
    private int kills;
    private int switches;
    private long bannedUntil;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean hasAllegiance() {
        return allegiance != null;
    }

    public Allegiance getAllegiance() {
        return allegiance;
    }

    public void setAllegiance(Allegiance allegiance) {
        this.allegiance = allegiance;
    }

    public int getBank() {
        return bank;
    }

    public void setBank(int bank) {
        this.bank = bank;
    }

    public void addBank(int amount) {
        this.bank += amount;
    }

    /** Removes shards on death. May take the bank negative. */
    public void lose(int amount) {
        this.bank -= amount;
    }

    public boolean removeBank(int amount) {
        if (bank < amount) return false;
        bank -= amount;
        return true;
    }

    public int getTotalEarned() {
        return totalEarned;
    }

    public void addEarned(int amount) {
        this.totalEarned += amount;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getSwitches() {
        return switches;
    }

    public void addSwitch() {
        this.switches++;
    }

    public long getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(long bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public void load(YamlConfiguration yaml) {
        String a = yaml.getString("allegiance");
        this.allegiance = a == null ? null : Allegiance.valueOf(a);
        this.bank = yaml.getInt("bank", 0);
        this.totalEarned = yaml.getInt("total-earned", 0);
        this.kills = yaml.getInt("kills", 0);
        this.switches = yaml.getInt("switches", 0);
        this.bannedUntil = yaml.getLong("banned-until", 0);
    }

    public void save(YamlConfiguration yaml) {
        if (allegiance != null) {
            yaml.set("allegiance", allegiance.name());
        } else {
            yaml.set("allegiance", null);
        }
        yaml.set("bank", bank);
        yaml.set("total-earned", totalEarned);
        yaml.set("kills", kills);
        yaml.set("switches", switches);
        yaml.set("banned-until", bannedUntil);
    }
}
