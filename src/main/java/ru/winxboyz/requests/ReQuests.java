package ru.winxboyz.requests;

// import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.milkbowl.vault.economy.Economy;
import ru.winxboyz.requests.commands.JournalCommand;
import ru.winxboyz.requests.listeners.FurnaceBreakListener;
import ru.winxboyz.requests.listeners.ItemCraftListener;
import ru.winxboyz.requests.listeners.ItemSmeltListener;
import ru.winxboyz.requests.listeners.JournalClickListener;
import ru.winxboyz.requests.listeners.NpcInteractListener;
import ru.winxboyz.requests.listeners.PlayerListener;
import ru.winxboyz.requests.managers.ConfigManager;
import ru.winxboyz.requests.managers.DatabaseManager;
import ru.winxboyz.requests.managers.PluginManager;
import ru.winxboyz.requests.tasks.RemoveExpired;
import ru.winxboyz.requests.utils.QuestData;
import ru.winxboyz.requests.utils.Tracker;

public class ReQuests extends JavaPlugin {

    private PluginManager pluginManager;
    private ConfigManager configManager;
    private String locale;

    private Map<String,String> messages = new HashMap<>();
    private List<QuestData> quests = new ArrayList<>();
    private final Map<UUID,List<Tracker>> progress = new HashMap<>();

    private static Economy econ = null;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public List<QuestData> getQuests() {
        return quests;
    }

    public Map<UUID,List<Tracker>> getProgress() {
        return progress;
    }
    public Map<String,String> getMessages() {
        return messages;
    }

    public PluginManager getManagers() {
        return pluginManager;
    }

    public void reload() {
        reloadConfig();
        progress.clear();
        pluginManager.getConfigManager().loadConfig();
        quests = pluginManager.getConfigManager().getQuests();
        messages = pluginManager.getConfigManager().getMessages();
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        
        pluginManager = PluginManager.getInstance();

        pluginManager.initialize(this);
        configManager = pluginManager.getConfigManager();
        configManager.loadConfig();
        quests = configManager.getQuests();
        messages = configManager.getMessages();

        DatabaseManager dbManager = pluginManager.getDatabaseManager();
        dbManager.connect();
        dbManager.createTables().thenCompose(v -> dbManager.loadProgress())
        .thenAccept(map -> { Bukkit.getScheduler().runTask(this, () -> { progress.putAll(map); }); })
        .exceptionally(e -> {
            getLogger().severe("SQL exception occured during db contracts:\n" + e.getMessage());
            return null;
        });

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            // commands.registrar().register(new ReQuestsCommand(this).initialize());
            commands.registrar().register(new JournalCommand(pluginManager.getQuestManager()).initialize());
        });
        getServer().getPluginManager().registerEvents(new NpcInteractListener(pluginManager.getQuestManager()), this);
        getServer().getPluginManager().registerEvents(new ItemCraftListener(pluginManager.getQuestManager()), this);
        getServer().getPluginManager().registerEvents(new ItemSmeltListener(pluginManager.getQuestManager(),pluginManager.getInventoryManager()), this);
        getServer().getPluginManager().registerEvents(new FurnaceBreakListener(pluginManager.getInventoryManager()), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(pluginManager.getQuestManager()), this);
        getServer().getPluginManager().registerEvents(new JournalClickListener(), this);
        
        
        RemoveExpired removeExpired = new RemoveExpired(progress);

        Bukkit.getScheduler().runTaskTimer(this, removeExpired::tick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, dbManager::saveProgress, 6000L, 6000L);
        getLogger().info(getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if(pluginManager != null) {
            pluginManager.getDatabaseManager().saveProgressSync();
            pluginManager.getDatabaseManager().close();
        }
        progress.clear();
        getLogger().info(getName() + " has been disabled!");
    }
    
}
