package ru.winxboyz.requests.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class QuestData {
    private final String name, displayName, description, npc;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final long cooldown, duration;
    private final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

    public long getCooldown() {
        return cooldown;
    }

    public long getDuration() {
        return duration;
    }

    public String getName() {
        return name;
    }

    public ItemStack getBook() {
        return book.clone();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getNpc() {
        return npc;
    }

    // public static Material getMaterial(String identifier) {
    //     return Material.matchMaterial(identifier);
    // }

    // public static Component fromMaterial(Material material) {
    //     if(material == null)
    //         return Component.text("");
    //     return Component.translatable(material);
    // }

    public static Component fromKey(String key, ObjectiveType type) {
        String replacedKey = key.replace(":", ".");
        if(ObjectiveType.KILL == type) 
            return Component.translatable("entity." + replacedKey);
        return Component.translatable("item." + replacedKey);
    }

    public List<QuestObjective> getObjectives() {
        return List.copyOf(objectives);
    }

    public List<QuestObjective> queryObjectives(ObjectiveType type, String target, int amount) {
        return objectives.stream()
        .filter(obj ->
            obj.getType() == type &&
            obj.getTarget().equals(target) &&
            obj.getAmount() == amount
        ).toList();
    }
    public List<QuestObjective> queryObjectives(ObjectiveType type, String target) {
        return objectives.stream()
        .filter(obj -> obj.getType() == type && obj.getTarget().equals(target)).toList();
    }
    public List<QuestObjective> queryObjectives(ObjectiveType type, int amount) {
        return objectives.stream()
        .filter(obj -> obj.getType() == type && obj.getAmount() == amount).toList();
    }
    public List<QuestObjective> queryObjectives(String target, int amount) {
        return objectives.stream()
        .filter(obj -> obj.getTarget().equals(target) && obj.getAmount() == amount).toList();
    }
    public List<QuestObjective> queryObjectives(ObjectiveType type) {
        return objectives.stream()
        .filter(obj -> obj.getType() == type).toList();
    }
    public List<QuestObjective> queryObjectives(int amount) {
        return objectives.stream()
        .filter(obj -> obj.getAmount() == amount).toList();
    }
    public List<QuestObjective> queryObjectives(String target) {
        return objectives.stream()
        .filter(obj -> obj.getTarget().equals(target)).toList();
    }

    public List<QuestReward> getRewards() {
        return List.copyOf(rewards);
    }

    public static Component objectiveComponent(QuestObjective objective) {
        Component type = fromKey(objective.getTarget(), objective.getType());
        StringBuilder objectiveText = new StringBuilder().append("- ");
        switch(objective.getType()) {
            case ObjectiveType.GIVE  -> objectiveText.append("Принести ").append(objective.getAmount()).append(" ");
            case ObjectiveType.KILL  -> objectiveText.append("Убить ").append(objective.getAmount()).append(" ");
            case ObjectiveType.CRAFT -> objectiveText.append("Создать ").append(objective.getAmount()).append(" ");
            case ObjectiveType.SMELT -> objectiveText.append("Переплавить ").append(objective.getAmount()).append(" ");
            case ObjectiveType.BREW  -> objectiveText.append("Приготовить ").append(objective.getAmount()).append(" ");
        }
        String objectiveString = objectiveText.toString();
        return Component.text(objectiveString).append(type).append(Component.text("\n"));
    }

    public QuestData(
        String name, String displayName, String description, String npc,
        List<QuestObjective> objectives, List<QuestReward> rewards,
        long duration, long cooldown,
        BiConsumer<Player,QuestData> callback
    ) {
        final Duration MAX_LIFETIME = Duration.ofDays(14);
        if(cooldown < duration) throw new IllegalArgumentException("Cooldown cannot be less than duration");
        
        this.cooldown = cooldown;
        this.duration = duration;
        this.displayName = displayName;
        this.name = name;
        this.description = description;
        this.npc = npc;
        this.objectives = objectives;
        this.rewards = rewards;

        List<Component> pages = new ArrayList<>(1);

        int i = 0, symbolsPerPage = 150;
        Component component = Component.text("").append(Component.text(this.displayName + "\n\n").color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
        do {
            int endIndex = Math.min((i+1)*symbolsPerPage, description.length());
            String pageText = description.substring(i*symbolsPerPage, endIndex);
            Component appendum = Component.text(pageText);
            pages.add(component.append(appendum));
            component = Component.text("");
        } while(i++ < description.length() / symbolsPerPage);

        if(((TextComponent)pages.getLast()).content().length() * 3 >= symbolsPerPage)
            pages.add(Component.text(""));
        pages.set(pages.size()-1,pages.getLast().append(Component.text("\n\nЦель:\n").decorate(TextDecoration.BOLD)));

        for(QuestObjective objective : objectives) {
            pages.set(pages.size()-1,pages.getLast().append(objectiveComponent(objective)));
        }
        
        if(((TextComponent)pages.getLast()).content().length() * 3 >= symbolsPerPage)
            pages.add(Component.text(""));
        pages.set(pages.size()-1,pages.getLast().append(Component.text("\nНаграда:\n").decorate(TextDecoration.BOLD)));
        for(QuestReward reward : rewards) {
            StringBuilder rewardText = new StringBuilder().append("- ");
            switch(reward.getType()) {
                case RewardType.EXP:
                    rewardText.append(reward.getAmount()).append(" опыта\n");
                    break;
                case RewardType.MONEY:
                    rewardText.append("$").append(reward.getAmount()).append("\n");
                    break;
                case RewardType.ITEM:
                    String itemName = Material.matchMaterial(reward.getIdentifier()).name();
                    rewardText.append(reward.getAmount()).append(" ").append(itemName).append("\n");
                    break;
            }
            pages.set(pages.size()-1,pages.getLast().append(Component.text(rewardText.toString())));
        }
        pages.set(pages.size()-1,pages.getLast()
        .append(Component.text("Принять\n").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
        .clickEvent(
            ClickEvent.callback(audience -> {
                if(audience instanceof Player player) {
                    callback.accept(player,this);
                    player.closeInventory();
                }
            }, ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(MAX_LIFETIME)
                .build())
        ))
        .append(Component.text("Отклонить\n").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
        .clickEvent(
            ClickEvent.callback(audience -> {
                if(audience instanceof Player player) {
                    player.closeInventory();
                }
            }, ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(MAX_LIFETIME)
                .build())
        )));

        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.title(Component.text(displayName));
        meta.author(Component.text(npc));
        meta.pages(pages);

        book.setItemMeta(meta);
    }
}
