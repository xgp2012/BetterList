package com.csbetterlist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * CS-BetterList 插件主类
 * 替代原生 /list 指令，提供更丰富的在线玩家信息显示
 *
 * @author 灯塔照耀海洋
 */
public class BetterListPlugin extends JavaPlugin implements Listener {

    /** 存储玩家加入服务器的时间戳，用于计算在线时长 */
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        printStartupBanner();

        int playersPerPage = getConfig().getInt("players-per-page", 10);
        String sortBy = getConfig().getString("sort-by", "playtime");

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("list").setExecutor(new ListCommand(this, playersPerPage, sortBy));
    }

    @Override
    public void onDisable() {
        playerJoinTimes.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerJoinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerJoinTimes.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 获取玩家的在线时长
     */
    public Duration getPlayerPlayTime(Player player) {
        Long joinTime = playerJoinTimes.get(player.getUniqueId());
        return joinTime == null ? Duration.ZERO : Duration.ofMillis(System.currentTimeMillis() - joinTime);
    }

    /**
     * 判断玩家是否为管理员（OP 或拥有 betterlist.admin 权限）
     */
    public boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("betterlist.admin");
    }

    /**
     * 获取玩家的网络延迟（Ping）
     */
    public int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ==================== 炫酷启动横幅 ====================

    private void printStartupBanner() {
        var logger = getLogger();

        var banner = Component.text()
                .append(Component.text(" ╔══════════════════════════════════════╗", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ║                                    ║", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ║  ", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ██████╗ ██████╗ ████████╗", NamedTextColor.DARK_AQUA))
                .append(Component.text("    ", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ██╔═══██╗██╔══██╗╚══██╔══╝", NamedTextColor.DARK_AQUA))
                .append(Component.text("    ", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ██║   ██║██████╔╝   ██║   ", NamedTextColor.AQUA))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ║  ", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ██║   ██║██╔══██╗   ██║   ", NamedTextColor.AQUA))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ║  ", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ╚██████╔╝██████╔╝   ██║   ", NamedTextColor.AQUA))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ║  ", NamedTextColor.DARK_AQUA))
                .append(Component.text("  ╚═════╝ ╚═════╝    ╚═╝   ", NamedTextColor.GOLD))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ║                                    ║", NamedTextColor.DARK_AQUA))
                .append(Component.text(" ║     CS-BetterList v1.3.0           ║", NamedTextColor.GOLD))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ║        替代原生 /list 指令          ║", NamedTextColor.GRAY))
                .append(Component.text("  ║", NamedTextColor.DARK_AQUA))
                .append(Component.newline())
                .append(Component.text(" ╚══════════════════════════════════════╝", NamedTextColor.DARK_AQUA))
                .build();

        logger.info("");
        logger.info(banner.toString());
        logger.info(Component.text("已启用：在线玩家列表增强插件", NamedTextColor.GREEN).toString());
        logger.info(Component.text("作者：灯塔照耀海洋", NamedTextColor.GRAY).toString());
        logger.info("");
    }
}
