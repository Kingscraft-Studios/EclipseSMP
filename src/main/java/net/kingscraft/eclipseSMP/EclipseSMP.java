package net.kingscraft.eclipseSMP;

import net.kingscraft.eclipseSMP.abilities.CooldownManager;
import net.kingscraft.eclipseSMP.abilities.LunaDash;
import net.kingscraft.eclipseSMP.abilities.SolFlare;
import net.kingscraft.eclipseSMP.allegiance.InventorySideListener;
import net.kingscraft.eclipseSMP.allegiance.ProfileManager;
import net.kingscraft.eclipseSMP.command.EclipseCommand;
import net.kingscraft.eclipseSMP.eclipse.EclipseManager;
import net.kingscraft.eclipseSMP.eclipse.EclipseTotem;
import net.kingscraft.eclipseSMP.gui.AllegianceGUI;
import net.kingscraft.eclipseSMP.gui.RecipeBook;
import net.kingscraft.eclipseSMP.gui.ShardMenu;
import net.kingscraft.eclipseSMP.powers.PowerManager;
import net.kingscraft.eclipseSMP.shards.AnvilUpgrades;
import net.kingscraft.eclipseSMP.shards.ShardManager;
import net.kingscraft.eclipseSMP.shards.ShardRecipes;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EclipseSMP extends JavaPlugin {

    private static EclipseSMP instance;

    private Settings settings;
    private Messages messages;
    private ProfileManager profileManager;
    private CooldownManager cooldownManager;
    private DiscordWebhook webhook;
    private EclipseManager eclipseManager;
    private ShardManager shardManager;
    private PowerManager powerManager;
    private AllegianceGUI allegianceGUI;
    private ShardMenu shardMenu;
    private RecipeBook recipeBook;
    private InventorySideListener inventorySideListener;
    private LunaDash lunaDash;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        settings = new Settings(this);
        messages = new Messages(settings);
        cooldownManager = new CooldownManager();
        profileManager = new ProfileManager(this);
        profileManager.startAutosave();
        webhook = new DiscordWebhook(settings);

        shardManager = new ShardManager(this);
        eclipseManager = new EclipseManager(this);
        powerManager = new PowerManager(this);
        allegianceGUI = new AllegianceGUI(this);
        shardMenu = new ShardMenu(this);
        recipeBook = new RecipeBook(this);
        inventorySideListener = new InventorySideListener(this);
        lunaDash = new LunaDash(this);

        registerListeners();
        registerRecipes();
        registerCommand();
        lunaDash.enable();

        if (settings.isEclipseEnabled()) {
            eclipseManager.start();
        }

        getLogger().info("Eclipse SMP enabled. Next eclipse is coming...");
    }

    @Override
    public void onDisable() {
        if (eclipseManager != null) {
            eclipseManager.shutdown();
        }
        if (profileManager != null) {
            profileManager.saveAll(Bukkit.getOnlinePlayers().stream()
                    .map(p -> profileManager.get(p.getUniqueId()))
                    .toList());
            profileManager.shutdown();
        }
        if (powerManager != null) {
            powerManager.shutdown();
        }
        if (lunaDash != null) {
            lunaDash.shutdown();
        }
        ShardRecipes.unregister(this);
        getLogger().info("Eclipse SMP disabled.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(powerManager, this);
        getServer().getPluginManager().registerEvents(eclipseManager, this);
        getServer().getPluginManager().registerEvents(shardManager, this);
        getServer().getPluginManager().registerEvents(new EclipseTotem(this), this);
        getServer().getPluginManager().registerEvents(new AnvilUpgrades(this), this);
        getServer().getPluginManager().registerEvents(lunaDash, this);
        getServer().getPluginManager().registerEvents(new SolFlare(this), this);
        getServer().getPluginManager().registerEvents(profileManager, this);
        getServer().getPluginManager().registerEvents(inventorySideListener, this);
        getServer().getPluginManager().registerEvents(allegianceGUI, this);
        getServer().getPluginManager().registerEvents(shardMenu, this);
        getServer().getPluginManager().registerEvents(recipeBook, this);
    }

    private void registerRecipes() {
        ShardRecipes.register(this);
    }

    private void registerCommand() {
        EclipseCommand executor = new EclipseCommand(this);
        for (String name : new String[]{"eclipse", "top", "deposit", "withdraw", "recipe"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(executor);
                cmd.setTabCompleter(executor);
            }
        }
    }

    public static EclipseSMP getInstance() {
        return instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return messages;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public DiscordWebhook getWebhook() {
        return webhook;
    }

    public EclipseManager getEclipseManager() {
        return eclipseManager;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public AllegianceGUI getAllegianceGUI() {
        return allegianceGUI;
    }

    public ShardMenu getShardMenu() {
        return shardMenu;
    }

    public RecipeBook getRecipeBook() {
        return recipeBook;
    }

    public InventorySideListener getInventorySideListener() {
        return inventorySideListener;
    }
}
