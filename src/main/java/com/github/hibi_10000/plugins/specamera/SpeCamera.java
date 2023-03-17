package com.github.hibi_10000.plugins.specamera;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class SpeCamera extends JavaPlugin implements Listener {
    Map<UUID, BukkitTask> tasks = new HashMap<>();
    String reqPerm = "minecraft.command.gamemode";

    @Override
    public void onEnable() {
        super.onEnable();
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand gamemode = getServer().getPluginCommand("gamemode");
        PluginCommand specamera = getCommand("specamera");
        if (specamera == null) return;
        if (gamemode != null) {
            reqPerm = gamemode.getPermission();
            specamera.setPermission(reqPerm);
        } else {
            reqPerm = specamera.getPermission();
        }
        getLogger().log(Level.INFO, "Permissions required for specamera command execution have been set to " + reqPerm);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player qp = e.getPlayer();
        tasks.get(qp.getUniqueId()).cancel();
        tasks.remove(qp.getUniqueId());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("specamera")) {
            return super.onCommand(sender, command, label, args);
        }
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender
                || !sender.hasPermission(reqPerm)) return false;
        Player sp = (Player) sender;
        if (args.length == 0) {
            if (!tasks.containsKey(sp.getUniqueId())) {
                sender.sendMessage("§cCommandUsage: /" + label + " [delay<tick>]");
                return true;
            }
            tasks.get(sp.getUniqueId()).cancel();
            tasks.remove(sp.getUniqueId());
            sender.sendMessage("§b[SpeCamera]§c SpeCameraを無効にしました");
            return true;
        }
        if (args.length > 2 || !args[0].chars().allMatch(Character::isDigit)) {
            sender.sendMessage("§cCommandUsage: /" + label + " [delay<tick>]");
            return false;
        }

        if (tasks.containsKey(sp.getUniqueId())) {
            tasks.get(sp.getUniqueId()).cancel();
            tasks.remove(sp.getUniqueId());
            sender.sendMessage("§b[SpeCamera]§a Targetの変更速度を"
                    + Long.parseLong(args[0]) + "tickに変更しました");

        } else {
            sender.sendMessage("§b[SpeCamera]§a SpeCameraを有効にしました (変更速度: "
                    + Long.parseLong(args[0]) + "tick)");
        }
        tasks.put(sp.getUniqueId(), new BukkitRunnable() {
            final Player p = sp;
            @Override
            public void run() {
                if (!p.hasPermission(reqPerm)) {
                    tasks.get(p.getUniqueId()).cancel();
                    tasks.remove(p.getUniqueId());
                    p.sendMessage("§b[SpeCamera]§c 権限が無くなったためSpeCameraを無効にしました");
                    return;
                }
                if (!p.getGameMode().equals(GameMode.SPECTATOR)) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§cSpeCameraを使用するにはスペクテイターにしてください！"));
                    return;
                }
                List<Entity> destP = Bukkit.selectEntities(sender, "@r[gamemode=!spectator]");
                if (destP.size() == 0) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§cスペクテイターではないプレイヤーがいません！"));
                    return;
                }
                p.setSpectatorTarget(destP.get(0));
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§aNew Target: §b" + destP.get(0).getName()));
            }
        }.runTaskTimer(this, Long.parseLong(args[0]), Long.parseLong(args[0])));
        return true;
    }
}
