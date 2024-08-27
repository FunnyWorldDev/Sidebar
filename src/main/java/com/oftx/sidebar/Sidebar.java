package com.oftx.sidebar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Sidebar extends JavaPlugin implements Listener {
    private Map<UUID, Boolean> playerSidebarStatus = new HashMap<>();

    @Override
    public void onEnable() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);

        // 从配置文件加载玩家侧边栏状态
        for (String key : getConfig().getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            playerSidebarStatus.put(uuid, getConfig().getBoolean(key));
        }

        // 定时任务更新侧边栏
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (playerSidebarStatus.getOrDefault(player.getUniqueId(), true)) {
                        updateSidebar(player);
                    } else {
                        player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // 每秒运行一次
    }

    @Override
    public void onDisable() {
        // 保存玩家侧边栏状态到配置文件
        for (Map.Entry<UUID, Boolean> entry : playerSidebarStatus.entrySet()) {
            getConfig().set(entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!playerSidebarStatus.containsKey(player.getUniqueId())) {
            playerSidebarStatus.put(player.getUniqueId(), true); // 默认开启侧边栏
        }
        if (playerSidebarStatus.get(player.getUniqueId())) {
            updateSidebar(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            boolean status = !playerSidebarStatus.getOrDefault(uuid, true);
            playerSidebarStatus.put(uuid, status);
            if (status) {
                player.sendMessage(ChatColor.GREEN + "侧边栏已开启");
                updateSidebar(player);
            } else {
                player.sendMessage(ChatColor.RED + "侧边栏已关闭");
                player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            }
            return true;
        }
        return false;
    }

    private void updateSidebar(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("sidebar", "dummy", "");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int ping = player.getPing();
        ChatColor pingColor = ping < 100 ? ChatColor.GREEN : (ping < 200 ? ChatColor.YELLOW : ChatColor.RED);
        Score pingScore = objective.getScore(pingColor + "延时");
        pingScore.setScore(ping);

        double tps = getServerTPS();

        if (tps <= 20) {
            ChatColor tpsColor = tps > 10 ? ChatColor.YELLOW : ChatColor.RED;
            Score tpsScore = objective.getScore(tpsColor + "TPS");
            tpsScore.setScore((int) tps);
        }

        player.setScoreboard(board);
    }

    private double getServerTPS() {
        try {
            Object minecraftServer = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Field recentTpsField = minecraftServer.getClass().getField("recentTps");
            double[] recentTps = (double[]) recentTpsField.get(minecraftServer);
            return recentTps[0]; // 获取最近一分钟的TPS
        } catch (Exception e) {
            e.printStackTrace();
            return 20.0; // 如果发生错误，返回默认值
        }
    }

}
