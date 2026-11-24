package ru.winxboyz.requests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import ru.winxboyz.requests.ReQuests;
import ru.winxboyz.requests.utils.ObjectiveType;
import ru.winxboyz.requests.utils.QuestData;
import ru.winxboyz.requests.utils.QuestObjective;
import ru.winxboyz.requests.utils.QuestReward;
import ru.winxboyz.requests.utils.RewardType;

public class ConfigManager {

    private final ReQuests plugin;

    // private String dbName;

    private ConfigurationSection
        localeSection,
        // sqlSection,
        questsSection;

    private final List<QuestData> quests = new ArrayList<>();
    private final Map<String,String> messages = new HashMap<>();

    public List<QuestData> getQuests() {
        return List.copyOf(quests);
    }
    public Map<String,String> getMessages() {
        return Map.copyOf(messages);
    }

    public void loadConfig() {
        // Чистим перед загрузкой: без этого при reload квесты и сообщения
        // накапливались бы в списках, а в режиме "квеста не заданы" ещё и двоились.
        quests.clear();
        messages.clear();

        plugin.setLocale(plugin.getConfig().getString("locale","en"));
        
        // sqlSection     = plugin.getConfig().getConfigurationSection("sql");
        localeSection  = plugin.getConfig().getConfigurationSection("locales." + plugin.getLocale());
        questsSection  = plugin.getConfig().getConfigurationSection("quests");

        // if(sqlSection == null) {
        //     plugin.getLogger().severe("No SQL connection credentials specified");
        //     return;
        // }
        BiConsumer<Player,QuestData> takeQuest = plugin.getManagers().getQuestManager()::takeQuest;
        if(questsSection == null) {
            plugin.getLogger().warning("No quests specified in config, creating default quests");
            quests.add(
                new QuestData(
                    "sakura", "Цветение вишни", "Принесите мне 10 вишневых саженцев", "npc_sakura",
                    List.of(new QuestObjective(ObjectiveType.GIVE, "minecraft:cherry_sapling", 10)),
                    List.of(new QuestReward(RewardType.MONEY, 1000)),
                    4 * 60 * 60 * 1000L,
                    24 * 60 * 60 * 1000L,
                    takeQuest
                )
            );
            for(QuestData quest : quests) {
                plugin.getConfig().set("quests." + quest.getName() + ".display-name", quest.getDisplayName());
                plugin.getConfig().set("quests." + quest.getName() + ".description", quest.getDescription());
                plugin.getConfig().set("quests." + quest.getName() + ".npc", quest.getNpc());
                plugin.getConfig().set("quests." + quest.getName() + ".cooldown", quest.getCooldown());
                plugin.getConfig().set("quests." + quest.getName() + ".duration", quest.getDuration());
                for(int i = 0; i < quest.getObjectives().size(); i++) {
                    QuestObjective obj = quest.getObjectives().get(i);
                    plugin.getConfig().set("quests." + quest.getName() + ".objectives." + i + ".type", obj.getType().name());
                    plugin.getConfig().set("quests." + quest.getName() + ".objectives." + i + ".target", obj.getTarget());
                    plugin.getConfig().set("quests." + quest.getName() + ".objectives." + i + ".amount", obj.getAmount());
                }
                for(int i = 0; i < quest.getRewards().size(); i++) {
                    QuestReward reward = quest.getRewards().get(i);
                    plugin.getConfig().set("quests." + quest.getName() + ".rewards." + i + ".type", reward.getType().name());
                    if(reward.getType() == RewardType.ITEM)
                        plugin.getConfig().set("quests." + quest.getName() + ".rewards." + i + ".identifier", reward.getIdentifier());
                    plugin.getConfig().set("quests." + quest.getName() + ".rewards." + i + ".amount", reward.getAmount());
                }
            }
            plugin.saveConfig();
        } else {
            Set<String> keys = questsSection.getKeys(false);
            for(String key : keys) {
                // Ошибка в одном квесте не должна ронять загрузку всего конфига:
                // иначе исключение вылетает из onEnable и quests остаётся null.
                try {
                    String
                        displayName = questsSection.getString(key + ".display-name"),
                        description = questsSection.getString(key + ".description"),
                        npc = questsSection.getString(key + ".npc");
                    long
                        cooldown = questsSection.getLong(key + ".cooldown"),
                        duration = questsSection.getLong(key + ".duration");

                    ConfigurationSection objectivesSection = questsSection.getConfigurationSection(key + ".objectives");
                    ConfigurationSection rewardsSection    = questsSection.getConfigurationSection(key + ".rewards");

                    List<QuestObjective> objectives = objectivesSection == null
                        ? List.of()
                        : objectivesSection.getKeys(false).stream().map(i -> {
                            String type = objectivesSection.getString(i + ".type");
                            String target = objectivesSection.getString(i + ".target");
                            int amount = objectivesSection.getInt(i + ".amount");
                            return new QuestObjective(ObjectiveType.valueOf(type.trim().toUpperCase()), target, amount);
                        }).toList();

                    List<QuestReward> rewards = rewardsSection == null
                        ? List.of()
                        : rewardsSection.getKeys(false).stream().map(i -> {
                            String type = rewardsSection.getString(i + ".type");
                            String identifier = rewardsSection.getString(i + ".identifier");
                            int amount = rewardsSection.getInt(i + ".amount");
                            return new QuestReward(RewardType.valueOf(type.trim().toUpperCase()), identifier, amount);
                        }).toList();

                    quests.add(new QuestData(key, displayName, description, npc, objectives, rewards, duration, cooldown, takeQuest));
                } catch(Exception e) {
                    plugin.getLogger().warning("Failed to load quest '" + key + "': " + e.getMessage());
                }
            }
        }

        if(localeSection != null)
            for(String key : localeSection.getKeys(false))
                messages.put(key, localeSection.getString(key));

        // plugin.setTableName(sqlSection.getString("locationsTable"));
        // if(dbName == null || !dbName.equals(sqlSection.getString("database"))) {
        //     dbName = sqlSection.getString("database","reore");
        //     PluginManager.getInstance().getDatabaseManager().close();
        //     PluginManager.getInstance().getDatabaseManager().connect(dbName);
        // }
        // PluginManager.getInstance().getDatabaseManager().createTableLocations().thenAccept(res -> {
        //     PluginManager.getInstance().getDatabaseManager().fetchLocations().thenAccept(locs -> {
        //         for(Location loc : locs) {
        //             ResourceData data = plugin.getResources().stream().filter(
        //                 rd -> loc.getBlock().getType().equals(rd.getResource())
        //             ).findFirst().orElse(null);
        //             if(data == null) {
        //                 plugin.getLogger().warning("Could not find resource data for block at "
        //                 + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
        //                 + " in world " + loc.getWorld().getName());
        //                 continue;
        //             }
        //             plugin.getProgress().put(loc, data);
        //         }
        //     });
        // });
    }

    public ConfigManager(ReQuests plugin) {
        this.plugin = plugin;
    }

}
