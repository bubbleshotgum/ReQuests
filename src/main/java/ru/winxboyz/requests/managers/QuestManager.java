package ru.winxboyz.requests.managers;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import ru.winxboyz.requests.ReQuests;
import ru.winxboyz.requests.utils.JournalHolder;
import ru.winxboyz.requests.utils.ObjectiveType;
import ru.winxboyz.requests.utils.QuestData;
import ru.winxboyz.requests.utils.QuestObjective;
import ru.winxboyz.requests.utils.QuestReward;
import ru.winxboyz.requests.utils.RewardType;
import ru.winxboyz.requests.utils.Tracker;

public class QuestManager {

    private final ReQuests plugin;
    private final Map<UUID,List<Tracker>> progress;

    /**
     * Квесты берём у плагина на лету, а не копируем в конструкторе.
     * Менеджер создаётся в PluginManager.initialize() до загрузки конфига,
     * поэтому копия в конструкторе навсегда оставалась null.
     * Так же список корректно подхватывается после reload.
     */
    private List<QuestData> quests() {
        List<QuestData> loaded = plugin.getQuests();
        return loaded == null ? List.of() : loaded;
    }

    /**
     * Прогресс игрока может отсутствовать: например, если плагин поставили
     * через /reload и игрок был онлайн до регистрации слушателя.
     */
    private List<Tracker> trackers(Player player) {
        List<Tracker> list = progress.get(player.getUniqueId());
        return list == null ? List.of() : list;
    }

    public boolean isNpcQuestGiver(String npcId, String npcName) {
        return quests().stream().anyMatch(q -> q.getNpc().equals(npcId) || q.getNpc().equals(npcName));
    }

    public QuestData getQuest(String npcId, String npcName) {
        return quests().stream().filter(q -> q.getNpc().equals(npcId) || q.getNpc().equals(npcName)).findFirst().orElse(null);
    }

    public void takeQuest(Player player, QuestData quest) {
        if(!progress.containsKey(player.getUniqueId()))
            addPlayer(player);
        if(!hasQuest(player, quest)) {
            progress.get(player.getUniqueId()).add(new Tracker(quest, System.currentTimeMillis()));
            player.sendActionBar(Component.text("Квест \"" + quest.getDisplayName() + "\" взят").color(NamedTextColor.GREEN));        
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        } else {
            player.sendActionBar(Component.text("Квест \"" + quest.getDisplayName() + "\" уже активен!").color(NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        }
    }

    /**
     * Квест по NPC: ищем и по внутреннему id, и по имени.
     *
     * У NPC в FancyNpcs два идентификатора: внутренний (uuid-строка) и имя.
     * В конфиге квеста обычно указано имя, поэтому ищем по обоим — иначе NPC
     * с нужным именем не находился и квест не выдавался.
     */
    // public QuestData getQuestByNpc(String id, String name) {
    //     return quests.stream()
    //         .filter(q -> q.getNpc().equals(id) || (name != null && q.getNpc().equals(name)))
    //         .findFirst().orElse(null);
    // }

    public void openQuest(Player player, String npcId, String npcName) {
        if(isNpcQuestGiver(npcId, npcName)) {
            QuestData quest = getQuest(npcId, npcName);
            player.openBook(quest.getBook());
        }
    }

    public boolean hasQuests(Player player) {
        return progress.containsKey(player.getUniqueId()) &&
        progress.get(player.getUniqueId()).size() > 0;
    }

    public boolean isOnCooldown(Player player, QuestData quest) {
        return trackers(player).stream().anyMatch(t -> t.getQuest() == quest && t.isOnCooldown());
    }

    public Integer getCooldown(Player player, QuestData quest) {
        Tracker tracker = trackers(player).stream().filter(t -> t.getQuest() == quest).findFirst().orElse(null);
        if(tracker == null)
            return null;
        return (int) (tracker.getResetAt() - System.currentTimeMillis()) / 1000;
    }


    public boolean hasQuest(Player player, String name) {
        return trackers(player).stream().anyMatch(t -> t.getQuest().getName().equals(name));
    }

    public boolean hasQuest(Player player, QuestData quest) {
        return trackers(player).stream().anyMatch(t -> t.getQuest().getName().equals(quest.getName()));
    }


    public boolean hasQuestFrom(Player player, String npcId, String npcName) {
        return trackers(player).stream().anyMatch(t -> {String s = t.getQuest().getNpc(); return s.equals(npcId) || s.equals(npcName);});
    }

    public boolean hasObjective(QuestData quest, ObjectiveType type) {
        return quest.getObjectives().stream().anyMatch(obj -> obj.getType() == type);
    }

    public boolean hasObjective(String npcId, String npcName, ObjectiveType type) {
        QuestData quest = getQuest(npcId, npcName);
        return quest != null && quest.getObjectives().stream().anyMatch(obj -> obj.getType() == type);
    }

    public int objectiveIndex(Tracker tracker, QuestObjective objective) {
        return tracker.getQuest().getObjectives().indexOf(objective);
    }

    public List<ItemStack> getItems(String npcId, String npcName, ObjectiveType type) {
        if(
            type == ObjectiveType.KILL ||
            !isNpcQuestGiver(npcId, npcName)) return null;
        QuestData quest = getQuest(npcId, npcName);
        if(quest == null) return null;
        List<QuestObjective> objectives = quest.queryObjectives(type);
        if(objectives == null || objectives.size() == 0) return null;
        List<ItemStack> items = new ArrayList<>();
        for(QuestObjective objective : objectives) {
            Material material = Material.matchMaterial(objective.getTarget());
            if(material == null) continue;
            items.add(new ItemStack(material, objective.getAmount()));
        }
        return items;
    }

    public void trackCraft(Player player, ItemStack items) {
        if(!hasQuests(player)) return;
        for(Tracker tracker : progress.get(player.getUniqueId())) {
            if(tracker.isOnCooldown()) continue;
            List<QuestObjective> objectives = tracker.getQuest().queryObjectives(ObjectiveType.CRAFT, items.getType().getKey().toString());
            if(objectives.size() == 0) continue;

            int[] tracks = tracker.getProgress();
            for(QuestObjective objective : objectives) {
                int index = objectiveIndex(tracker, objective);
                if(objective.getAmount() <= tracks[index]) continue;
                tracks[index] += items.getAmount();
                if(objective.getAmount() <= tracks[index])
                    checkCompletion(player, tracker);
            }
        }
    }

    public void trackSmelt(Player player, ItemStack items) {
        if(!hasQuests(player)) return;
        for(Tracker tracker : progress.get(player.getUniqueId())) {
            if(tracker.isOnCooldown()) continue;
            List<QuestObjective> objectives = tracker.getQuest().queryObjectives(ObjectiveType.SMELT, items.getType().getKey().toString());
            if(objectives.size() == 0) continue;

            int[] tracks = tracker.getProgress();
            for(QuestObjective objective : objectives) {
                int index = objectiveIndex(tracker, objective);
                if(objective.getAmount() <= tracks[index]) continue;
                tracks[index] += items.getAmount();
                if(objective.getAmount() <= tracks[index])
                    checkCompletion(player, tracker);
            }
        }
    }

    public void trackGive(Player player, String npcName, String npcId) {
        if(!hasQuests(player)) return;
        if(!hasQuestFrom(player, npcId, npcName)) return;
        Tracker tracker = progress.get(player.getUniqueId()).stream().filter(tr -> tr.getQuest() == getQuest(npcId, npcName)).findFirst().get();
        if(tracker.isOnCooldown()) return;
        List<QuestObjective> objectives = tracker.getQuest().queryObjectives(ObjectiveType.GIVE);
        if(objectives.size() == 0) return;
        int[] tracks = tracker.getProgress();
        // List<ItemStack> items = getItems(npcId, npcName, ObjectiveType.GIVE);
        // if(items == null || items.size() == 0) return;
        for(QuestObjective objective : objectives) {
            // if(!items.getType().getKey().asString().equals(objective.getTarget()))
            //     continue;
            int index = objectiveIndex(tracker, objective);
            if(objective.getAmount() <= tracks[index]) continue;

            ItemStack[] inventory = player.getInventory().getContents();
            for(ItemStack item : inventory) {
                if(item != null && item.getType().getKey().asString().equals(objective.getTarget())) {
                    int amount = item.getAmount();
                    if(amount + tracks[index] >= objective.getAmount()) {
                        item.setAmount(amount + tracks[index] - objective.getAmount());
                        tracks[index] = objective.getAmount();
                        checkCompletion(player, tracker);
                        break;
                    }
                    item.setAmount(0);
                    tracks[index] += amount;
                }
            }
        }
    }

    public void handleJournal(Player player) {
        if(player == null)
            return;
        List<Tracker> trackers = progress.get(player.getUniqueId()).stream()
        .filter(t -> !t.isOnCooldown()).toList();
        if(trackers.size() == 0) {
            player.sendActionBar(Component.text("Нет активных квестов").color(NamedTextColor.RED));
            return;
        }
        // List<Inventory> questList = new ArrayList<>();
        int
            totalMissionsPerPage = 1,
            totalPages = trackers.size() / totalMissionsPerPage + 1;
        JournalHolder journal = new JournalHolder();
        for(int i = 0; i < totalPages; i++) {
            Inventory page = Bukkit.createInventory(journal, 27, Component.text("Квесты (" + (i+1) + "/" + totalPages + ")"));
            for(int j = 0; j < totalMissionsPerPage; j++) {
                int index = j + i*totalMissionsPerPage;
                if(index >= trackers.size())
                    break;
                Tracker t = trackers.get(index);
                QuestData quest = t.getQuest();
                int[] questProgress = t.getProgress();
                
                ItemStack q = new ItemStack(Material.PAPER);
                ItemMeta meta = q.getItemMeta();
                meta.displayName(Component.text(quest.getDisplayName()).color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.ITALIC));
                
                List<Component> lore = new ArrayList<>();
                for(int k = 0; k < questProgress.length; k++) {
                    QuestObjective objective = quest.getObjectives().get(k);
                    lore.add(QuestData.objectiveComponent(objective));
                    lore.set(lore.size()-1, lore.get(lore.size()-1).append(
                        Component.text(": " + questProgress[k] + "/" + objective.getAmount()).color(NamedTextColor.GOLD)
                    ));
                }

                meta.lore(lore);
                q.setItemMeta(meta);
                page.setItem(j, q);
            }
            if(i > 0)
                page.setItem(18, new ItemStack(Material.ARROW));
            if(i < totalPages - 1)
                page.setItem(26, new ItemStack(Material.ARROW));
            journal.addInventory(page);
        }
        journal.setPage(1);
        player.openInventory(journal.getInventory());
    }

    public void checkCompletion(Player player, Tracker tracker) {
        for(int i = 0; i < tracker.getProgress().length; i++)
            if(tracker.getProgress()[i] < tracker.getQuest().getObjectives().get(i).getAmount())
                return;
        handleReward(player, tracker.getQuest());
        tracker.sendOnCooldown();
    }

    public void handleReward(Player player, QuestData quest) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        
        Component message = Component.text("Вы выполнили квест: " + quest.getDisplayName() + "!")
            .color(NamedTextColor.GREEN)
            .append(Component.text("Награды:\n").color(NamedTextColor.YELLOW));

        for(QuestReward reward : quest.getRewards()) {
            message = message.append(Component.text((
                reward.getType() == RewardType.MONEY ?
                    "$" + reward.getAmount() :
                reward.getType() == RewardType.EXP   ?
                    reward.getAmount() + " очков опыта" :
                reward.getAmount() + " шт " + Material.matchMaterial(reward.getIdentifier()).toString()) + "\n"
            ).color(NamedTextColor.YELLOW));
            switch(reward.getType()) {
                case MONEY -> ReQuests.getEconomy().depositPlayer(player, reward.getAmount());
                case EXP -> player.giveExp(reward.getAmount());
                case ITEM -> {
                    HashMap<Integer,ItemStack> leftOvers = player.getInventory()
                    .addItem(new ItemStack(Material.matchMaterial(reward.getIdentifier()),reward.getAmount()));
                    for(ItemStack item : leftOvers.values()) {
                        player.getWorld().dropItem(player.getLocation(), item);
                    }
                }
            }
        }
        player.sendMessage(message);
    }

    public void addPlayer(Player player) {
        progress.put(player.getUniqueId(), new ArrayList<>());
    }

    public void removePlayer(Player player) {
        progress.remove(player.getUniqueId());
    }

    // public void handleAccept(Player player, QuestData quest) {
    //     List<Tracker> trackers = progress.get(player.getUniqueId());
    //     if(hasQuest(player, quest)) {
    //         player.sendMessage(Component.text(messages.getOrDefault("quest-already-accepted","КВЕСТ УЖЕ ВЗЯТ")).color(NamedTextColor.RED));
    //         return;
    //     }

    //     Tracker tracker = new Tracker(quest, System.currentTimeMillis());
    //     trackers.add(tracker);
    //     player.sendMessage(Component.text(messages.getOrDefault("quest-accepted","КВЕСТ ВЗЯТ")).color(NamedTextColor.GREEN));
    // }

    public QuestManager(ReQuests plugin) {
        this.plugin = plugin;
        this.progress = plugin.getProgress();
    }

}
