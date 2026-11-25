package ru.winxboyz.requests.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import ru.winxboyz.requests.managers.InventoryManager;
import ru.winxboyz.requests.managers.QuestManager;

public class ItemSmeltListener implements Listener {

    private final QuestManager      questManager;
    private final InventoryManager  inventoryManager;


    @EventHandler(ignoreCancelled = true)
    public void ItemFurnacePutListener(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();

        if(clickedInventory == null)
            return;

        Player player = (Player) event.getWhoClicked();

        if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            inventoryManager.handleShiftClick(
                player,
                event.getCurrentItem()
            );
            return;
        }
        if(event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
            return;

        Inventory inventory = ((Player) event.getWhoClicked()).getOpenInventory().getTopInventory();
        if(inventory != clickedInventory)
            return;

        Block block = inventory.getLocation().getBlock();
        
        inventoryManager.handleClick(block, player, event.getSlot(), event.getCursor(), event.getCurrentItem());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSmelt(FurnaceSmeltEvent event) {
        ItemStack result = event.getResult();
        Block block = event.getBlock();
        if(!(block.getState() instanceof Furnace))
            return;
        // if(!inventoryManager.hasIssuer(block)) return;

        UUID playerUUID = inventoryManager.getIssuer(block);
        if(playerUUID == null)
            return;
        Player player = Bukkit.getPlayer(playerUUID);

        if(player == null)
            return;

        questManager.trackSmelt(player, result);

        if(event.getSource().getAmount() == 1)
            inventoryManager.removeIssuer(block, playerUUID);
    }

    public ItemSmeltListener(QuestManager questManager, InventoryManager inventoryManager) {
        this.questManager = questManager;
        this.inventoryManager = inventoryManager;
    }

}
