package com.betterlist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * /list 命令处理器
 * 支持分页显示、按在线时长或名称排序、管理员标记等功能
 *
 * @author 灯塔照耀海洋
 */
public class ListCommand implements CommandExecutor, TabCompleter {

    private final BetterListPlugin plugin;
    private final int playersPerPage;
    private final String sortBy;

    public ListCommand(BetterListPlugin plugin, int playersPerPage, String sortBy) {
        this.plugin = plugin;
        this.playersPerPage = Math.max(1, playersPerPage);
        this.sortBy = sortBy;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // 获取所有在线玩家
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage(Component.text("当前没有在线玩家。", NamedTextColor.GRAY));
            return true;
        }

        // 按配置排序：按名称字母序 或 按在线时长降序
        if ("name".equalsIgnoreCase(sortBy)) {
            onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            onlinePlayers.sort((a, b) -> {
                Duration timeA = plugin.getPlayerPlayTime(a);
                Duration timeB = plugin.getPlayerPlayTime(b);
                return timeB.compareTo(timeA);
            });
        }

        // 计算总页数
        int totalPlayers = onlinePlayers.size();
        int totalPages = Math.max(1, (totalPlayers + playersPerPage - 1) / playersPerPage);

        // 解析请求的页码
        int requestedPage = 1;
        if (args.length > 0) {
            try {
                requestedPage = Integer.parseInt(args[0]);
                if (requestedPage < 1) requestedPage = 1;
                if (requestedPage > totalPages) requestedPage = totalPages;
            } catch (NumberFormatException ignored) {
                // 非法页码输入，默认显示第一页
            }
        }

        // 计算分页范围
        int startIndex = (requestedPage - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, totalPlayers);

        // 发送列表标题
        sender.sendMessage(Component.text("=== 在线玩家列表 (", NamedTextColor.GOLD)
                .append(Component.text(totalPlayers, NamedTextColor.YELLOW))
                .append(Component.text("/", NamedTextColor.GOLD))
                .append(Component.text(plugin.getServer().getMaxPlayers(), NamedTextColor.YELLOW))
                .append(Component.text(") 第 ", NamedTextColor.GOLD))
                .append(Component.text(requestedPage, NamedTextColor.YELLOW))
                .append(Component.text("/", NamedTextColor.GOLD))
                .append(Component.text(totalPages, NamedTextColor.YELLOW))
                .append(Component.text(" 页 ===", NamedTextColor.GOLD)));

        // 逐行显示当前页的玩家信息
        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(formatPlayerEntry(onlinePlayers.get(i)));
        }

        // 发送翻页导航（仅多页时显示）
        sendPageNavigation(sender, requestedPage, totalPages, label);

        return true;
    }

    /**
     * 格式化单个玩家的显示条目，包含名称、游戏模式、Ping 和在线时长
     */
    private Component formatPlayerEntry(Player player) {
        Duration playTime = plugin.getPlayerPlayTime(player);
        String playTimeStr = formatDuration(playTime);
        String displayName = player.getPlayerListName() != null
                ? player.getPlayerListName() : player.getName();

        // 管理员玩家显示为红色
        TextColor nameColor = plugin.isAdmin(player) ? NamedTextColor.RED : NamedTextColor.WHITE;

        // 游戏模式标签与颜色映射
        String modeStr;
        NamedTextColor modeColor;
        switch (player.getGameMode()) {
            case CREATIVE:
                modeStr = "[创造]";
                modeColor = NamedTextColor.AQUA;
                break;
            case ADVENTURE:
                modeStr = "[冒险]";
                modeColor = NamedTextColor.DARK_PURPLE;
                break;
            case SPECTATOR:
                modeStr = "[旁观]";
                modeColor = NamedTextColor.GRAY;
                break;
            default:
                modeStr = "[生存]";
                modeColor = NamedTextColor.GREEN;
                break;
        }

        // 构建悬浮提示信息
        TextComponent.Builder hoverBuilder = Component.text();
        hoverBuilder.append(Component.text("游戏模式: ", NamedTextColor.GRAY));
        hoverBuilder.append(Component.text(modeStr, modeColor));
        hoverBuilder.append(Component.newline());
        hoverBuilder.append(Component.text("在线时长: ", NamedTextColor.GRAY));
        hoverBuilder.append(Component.text(playTimeStr, NamedTextColor.GREEN));
        hoverBuilder.append(Component.newline());
        hoverBuilder.append(Component.text("世界: ", NamedTextColor.GRAY));
        hoverBuilder.append(Component.text(player.getWorld().getName(), NamedTextColor.YELLOW));

        // 名称 + 模式 + Ping + 在线时长
        return Component.text("  ", NamedTextColor.GRAY)
                .append(Component.text(displayName, nameColor))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text(modeStr, modeColor))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(formatPing(player))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(playTimeStr, NamedTextColor.GREEN))
                .hoverEvent(HoverEvent.showText(hoverBuilder.build()));
    }

    /**
     * 将 Duration 格式化为易读的中文时间字符串
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 格式化 Ping 显示，根据延迟高低使用不同颜色
     * 绿色：< 50ms，黄色：50-99ms，红色：≥ 100ms
     */
    private Component formatPing(Player player) {
        int ping = plugin.getPlayerPing(player);
        String pingStr = ping + "ms";

        NamedTextColor pingColor;
        if (ping < 50) {
            pingColor = NamedTextColor.GREEN;
        } else if (ping < 100) {
            pingColor = NamedTextColor.YELLOW;
        } else {
            pingColor = NamedTextColor.RED;
        }

        return Component.text(pingStr, pingColor);
    }

    /**
     * 发送翻页导航栏，包含可点击的上一页/下一页按钮
     */
    private void sendPageNavigation(CommandSender sender, int currentPage,
                                     int totalPages, String commandLabel) {
        if (totalPages <= 1) return;

        TextComponent.Builder navBuilder = Component.text();

        // 上一页按钮（首页时显示为灰色不可用状态）
        if (currentPage > 1) {
            navBuilder.append(Component.text("[<] ", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand(
                            "/" + commandLabel + " " + (currentPage - 1)))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("上一页", NamedTextColor.GRAY))));
        } else {
            navBuilder.append(Component.text("[<] ", NamedTextColor.DARK_GRAY));
        }

        // 页码指示器
        navBuilder.append(Component.text(
                "  " + currentPage + " / " + totalPages + "  ", NamedTextColor.GRAY));

        // 下一页按钮（末页时显示为灰色不可用状态）
        if (currentPage < totalPages) {
            navBuilder.append(Component.text("[>]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand(
                            "/" + commandLabel + " " + (currentPage + 1)))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("下一页", NamedTextColor.GRAY))));
        } else {
            navBuilder.append(Component.text("[>]", NamedTextColor.DARK_GRAY));
        }

        sender.sendMessage(navBuilder.build());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        // 提供页码的 Tab 补全建议
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("1");
            suggestions.add("2");
            suggestions.add("3");
            return suggestions;
        }
        return null;
    }
}