package ru.winxboyz.requests.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import ru.winxboyz.requests.managers.QuestManager;

public class ItemCraftListener implements Listener {

    private QuestManager manager;

    @EventHandler(ignoreCancelled = true)
    public void onItemCrafted(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        Player player = (Player) event.getWhoClicked();

        manager.trackCraft(player, result);
    }

    public ItemCraftListener(QuestManager manager) {
        this.manager = manager;
    }

}
