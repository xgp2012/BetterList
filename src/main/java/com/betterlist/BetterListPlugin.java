package com.betterlist;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BetterList 插件主类
 * 替代原生 /list 指令，提供更丰富的在线玩家信息显示
 *
 * @author 灯塔照耀海洋
 */
public class BetterListPlugin extends JavaPlugin implements Listener {

    /**
     * 存储玩家加入服务器的时间戳，用于计算在线时长
     */
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();

    @Override
    public void onEnable() {
        // 保存默认配置文件
        saveDefaultConfig();

        // 读取配置项
        int playersPerPage = getConfig().getInt("players-per-page", 10);
        String sortBy = getConfig().getString("sort-by", "playtime");

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        // 注册 /list 命令处理器
        getCommand("list").setExecutor(new ListCommand(this, playersPerPage, sortBy));

        getLogger().info("BetterList 已启用！原生 /list 指令已被替代。");
    }

    @Override
    public void onDisable() {
        playerJoinTimes.clear();
        getLogger().info("BetterList 已禁用。");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 记录玩家加入时间
        playerJoinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 移除离线玩家的记录，避免内存泄漏
        playerJoinTimes.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 获取玩家的在线时长
     *
     * @param player 目标玩家
     * @return 在线时长 Duration 对象
     */
    public Duration getPlayerPlayTime(Player player) {
        Long joinTime = playerJoinTimes.get(player.getUniqueId());
        if (joinTime == null) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(System.currentTimeMillis() - joinTime);
    }

    /**
     * 判断玩家是否为管理员（OP 或拥有 betterlist.admin 权限）
     *
     * @param player 目标玩家
     * @return 是否为管理员
     */
    public boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("betterlist.admin");
    }

    /**
     * 获取玩家的网络延迟（Ping）
     *
     * @param player 目标玩家
     * @return Ping 值（毫秒），获取失败时返回 0
     */
    public int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return 0;
        }
    }
}
