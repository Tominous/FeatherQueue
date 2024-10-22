package me.ceezuns.queue;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.ceezuns.FeatherQueue;
import me.ceezuns.queue.events.PlayerQueueJoinEvent;
import me.ceezuns.queue.events.PlayerQueueLeaveEvent;
import me.ceezuns.queue.events.PlayerQueuePushEvent;
import me.ceezuns.queue.events.QueueStatusChangeEvent;
import me.ceezuns.queue.priority.QueuePriority;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

@CommandAlias("queue")
public class QueueCommand extends BaseCommand {

    public QueueCommand() {
        FeatherQueue.getInstance().getCommandManager().getCommandCompletions().registerAsyncCompletion("queues", callback -> FeatherQueue.getInstance().getQueueManager().getQueues().keySet());
        FeatherQueue.getInstance().getCommandManager().getCommandCompletions().registerAsyncCompletion("queue-statuses", callback -> Arrays.stream(QueueStatus.values()).parallel().map(QueueStatus::name).collect(Collectors.toList()));
        FeatherQueue.getInstance().getCommandManager().getCommandCompletions().registerAsyncCompletion("queue-priorities", callback -> FeatherQueue.getInstance().getQueuePriorityManager().getPriorities().parallelStream().map(QueuePriority::getIdentifier).collect(Collectors.toList()));
        FeatherQueue.getInstance().getCommandManager().getCommandCompletions().registerAsyncCompletion("queue-priorities-by-permission", callback -> FeatherQueue.getInstance().getQueuePriorityManager().getPriorities().parallelStream().map(QueuePriority::getPermission).collect(Collectors.toList()));
        FeatherQueue.getInstance().getCommandManager().registerCommand(this);
    }

    @Default
    @HelpCommand
    public static void onHelpCommand(CommandSender sender) {
        FeatherQueue.getInstance().getConfiguration().getStringList("messages.queueHelpCommand").forEach(line -> sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', line))));
    }

    @Subcommand("join")
    @CommandCompletion("@queues")
    @CommandPermission("queue.join")
    public static void onJoinCommand(ProxiedPlayer sender, String identifier) {
        if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier) == null) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.invalidIdentifier").replaceAll("%identifier%", identifier))));
        } else if (FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).isQueued()) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.alreadyQueued").replaceAll("%identifier%", FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).getQueue().getIdentifier()))));
        } else if (sender.getServer().getInfo().equals(FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getServer())) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.sameServer").replaceAll("%identifier%", sender.getServer().getInfo().getName()))));
        }  else if (sender.hasPermission("featherqueue.bypass")) {
            sender.connect(FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getServer());
        } else if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getStatus() == QueueStatus.CLOSED) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.closedQueue").replaceAll("%identifier%", sender.getServer().getInfo().getName()))));
        } else if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getPlayers().size() + 1 > FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getMaximumQueueSize()) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.maximumQueueSizeExceeded").replaceAll("%identifier%", sender.getServer().getInfo().getName()))));
        } else {
            FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).setQueue(FeatherQueue.getInstance().getQueueManager().getQueue(identifier));
            FeatherQueue.getInstance().getQueueManager().getQueue(identifier).addPlayer(FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender));
            FeatherQueue.getInstance().getProxy().getPluginManager().callEvent(new PlayerQueueJoinEvent(sender, FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).getQueue()));
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueJoinCommand.joinedQueue").replaceAll("%identifier%", identifier))));
        }
    }

    @Subcommand("leave")
    @CommandPermission("queue.leave")
    public static void onLeaveCommand(ProxiedPlayer sender) {
        if (FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).isQueued()) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueLeaveCommand.leftQueue").replaceAll("%identifier%", FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).getQueue().getIdentifier()))));
            FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).getQueue().removePlayer(FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender));
            FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).setQueue(null);
            FeatherQueue.getInstance().getProxy().getPluginManager().callEvent(new PlayerQueueLeaveEvent(sender, FeatherQueue.getInstance().getQueuePlayerManager().getPlayer(sender).getQueue()));
        } else {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueLeaveCommand.notQueued"))));
        }
    }

    @Subcommand("set size")
    @CommandCompletion("@queues")
    @CommandPermission("queue.set.size")
    public static void onSetSizeCommand(CommandSender sender, String identifier, int maximumQueueSize) {
        if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier) == null) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetSizeCommand.invalidIdentifier").replaceAll("%identifier%", identifier))));
        } else if (maximumQueueSize < 0) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetSizeCommand.invalidSize").replaceAll("%size%", String.valueOf(maximumQueueSize)))));
        } else if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getMaximumQueueSize() == maximumQueueSize) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetSizeCommand.sameSize").replaceAll("%size%", String.valueOf(maximumQueueSize)).replaceAll("%identifier%", identifier))));
        } else {
            FeatherQueue.getInstance().getQueueManager().getQueue(identifier).setMaximumQueueSize(maximumQueueSize);
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetSizeCommand.sizeChange").replaceAll("%identifier%", identifier).replaceAll("%size%", String.valueOf(maximumQueueSize)))));
        }
    }

    @Subcommand("set status")
    @CommandCompletion("@queues @queue-statuses")
    @CommandPermission("queue.set.status")
    public static void onSetStatusCommand(CommandSender sender, String identifier, String status) {
        if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier) == null) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetStatusCommand.invalidIdentifier").replaceAll("%identifier%", identifier))));
        } else if (!QueueStatus.isValidStatus(status)) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetStatusCommand.invalidStatus").replaceAll("%status%", status))));
        } else if (FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getStatus().name().equalsIgnoreCase(status)) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetStatusCommand.sameStatus").replaceAll("%status%", status).replaceAll("%identifier%", identifier))));
        } else {
            FeatherQueue.getInstance().getProxy().getPluginManager().callEvent(new QueueStatusChangeEvent(FeatherQueue.getInstance().getQueueManager().getQueue(identifier), FeatherQueue.getInstance().getQueueManager().getQueue(identifier).getStatus(), QueueStatus.valueOf(status.toUpperCase())));
            FeatherQueue.getInstance().getQueueManager().getQueue(identifier).setStatus(QueueStatus.valueOf(status.toUpperCase()));
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetStatusCommand.statusChange").replaceAll("%identifier%", identifier).replaceAll("%status%", status))));
        }
    }

    @Subcommand("set priority permission")
    @CommandCompletion("@queue-priorities @queue-priorities-by-permission")
    @CommandPermission("queue.set.priority.permission")
    public static void onSetPriorityPermissionCommand(CommandSender sender, String identifier, String permission) {
        if (FeatherQueue.getInstance().getQueuePriorityManager().getPriority(identifier) == null) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetPriorityPermissionCommand.invalidIdentifier").replaceAll("%identifier%", identifier))));
        } else if (FeatherQueue.getInstance().getQueuePriorityManager().getPriority(identifier).getPermission().equals(permission)) {
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetPriorityPermissionCommand.samePermission").replaceAll("%identifier%", identifier).replaceAll("%permission%", permission))));
        } else {
            FeatherQueue.getInstance().getQueuePriorityManager().getPriority(identifier).setPermission(permission);
            sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', FeatherQueue.getInstance().getConfiguration().getString("messages.queueSetPriorityPermissionCommand.permissionChange").replaceAll("%identifier%", identifier).replaceAll("%permission%", permission))));
        }
    }

}
