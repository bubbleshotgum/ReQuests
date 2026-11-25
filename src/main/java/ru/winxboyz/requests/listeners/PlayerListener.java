package ru.winxboyz.requests.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
// import org.bukkit.event.player.PlayerKickEvent;
// import org.bukkit.event.player.PlayerQuitEvent;

import ru.winxboyz.requests.managers.QuestManager;

public class PlayerListener implements Listener {

    private QuestManager manager;

    @EventHandler 
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.addPlayer(event.getPlayer());
    }

    // @EventHandler
    // public void onPlayerQuit(PlayerQuitEvent event) {
    //     manager.removePlayer(event.getPlayer());
    // }

    // @EventHandler
    // public void onPlayerKick(PlayerKickEvent event) {
    //     manager.removePlayer(event.getPlayer());
    // }

    public PlayerListener(QuestManager manager) {
        this.manager = manager;
    }
}
