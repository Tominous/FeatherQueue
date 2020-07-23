package me.ceezuns.queue.tasks;

import me.ceezuns.FeatherQueue;
import me.ceezuns.queue.Queue;
import me.ceezuns.queue.QueueStatus;
import me.ceezuns.queue.player.QueuePlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class QueuePushTask implements Runnable {

    private Queue queue;
    private ScheduledTask task;

    public QueuePushTask(Queue queue) {
        this.queue = queue;
        this.task = FeatherQueue.getInstance().getProxy().getScheduler().schedule(FeatherQueue.getInstance(), this, 1, this.queue.getPushTaskDelay(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if (this.queue.getStatus() == QueueStatus.PAUSED || this.queue.getStatus() == QueueStatus.CLOSED || this.queue.getPlayers().isEmpty()) {
            return;
        } else {
            QueuePlayer player = this.queue.getPlayers().poll();
            player.setQueue(null);
            player.getPlayer().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.pushTask").replaceAll("%identifier%", this.queue.getIdentifier()))));
            player.getPlayer().connect(this.queue.getServer());
        }
    }

    public void cancel() {
        this.task.cancel();
    }
}
