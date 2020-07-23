package me.ceezuns.queue.player;

import me.ceezuns.FeatherQueue;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Level;

public class QueuePlayerListener implements Listener {

    public QueuePlayerListener() {
        FeatherQueue.getInstance().getProxy().getPluginManager().registerListener(FeatherQueue.getInstance(), this);
        FeatherQueue.getInstance().getLogger().log(Level.INFO, "Registered QueuePlayerListener as a listener.");
    }

    @EventHandler
    public void onPostLoginEvent(PostLoginEvent event) {
        if (FeatherQueue.getInstance().getQueuePlayerManager().getPlayers().add(new QueuePlayer(event.getPlayer(), FeatherQueue.getInstance().getQueuePriorityManager().getPriorityForPlayer(event.getPlayer())))) {
            FeatherQueue.getInstance().getLogger().log(Level.INFO, "Created QueuePlayer for " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ").");
        } else {
            FeatherQueue.getInstance().getLogger().log(Level.SEVERE, "Failed to create QueuePlayer for " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ").");
        }
    }

    @EventHandler
    public void onPostDisconnectEvent(PlayerDisconnectEvent event) {
        if (FeatherQueue.getInstance().getQueuePlayerManager().getPlayers().remove(FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(event.getPlayer()))) {
            FeatherQueue.getInstance().getLogger().log(Level.INFO, "Removed QueuePlayer for " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ").");
        } else {
            FeatherQueue.getInstance().getLogger().log(Level.SEVERE, "Failed to remove QueuePlayer for " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ").");
        }
    }

}
