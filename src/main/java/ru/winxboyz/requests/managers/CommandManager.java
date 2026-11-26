package ru.winxboyz.requests.managers;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.winxboyz.requests.ReQuests;
import ru.winxboyz.requests.utils.QuestData;

public class CommandManager {
    private final ReQuests plugin;

    private List<QuestData> quests() {
        return plugin.getQuests();
    }

    private Map<String,String> messages() {
        return plugin.getMessages();
    }

    public void handleList(CommandSender sender) {
        Map<String,String> msgs = messages();
        List<QuestData> questList = quests();
        if(questList == null)
            sender.sendActionBar(Component.text(msgs.getOrDefault("not-initialized", "Квесты не инициализированы")).color(NamedTextColor.RED)); 
        Component message = Component.text("Квесты:");
        for(QuestData q : questList)
            message = message.append(Component.text("\n" + q.getDisplayName() + " (" + q.getName() + ")"));
        sender.sendMessage(message);
    }

    public CommandManager(ReQuests plugin) {
        this.plugin = plugin;
    }

}
