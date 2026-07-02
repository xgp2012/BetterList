package com.csbetterlist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final int DEFAULT_MAX_PLAYERS_PAGE = 10;
    private static final String SORT_BY_PLAYTIME = "playtime";
    private static final String SORT_BY_NAME = "name";

    // 颜色常量
    private static final NamedTextColor COLOR_TITLE = NamedTextColor.GOLD;
    private static final NamedTextColor COLOR_NUMBER = NamedTextColor.YELLOW;
    private static final NamedTextColor COLOR_GRAY = NamedTextColor.GRAY;
    private static final NamedTextColor COLOR_NAME_ADMIN = NamedTextColor.RED;
    private static final NamedTextColor COLOR_NAME_PLAYER = NamedTextColor.WHITE;
    private static final NamedTextColor COLOR_PLAYTIME = NamedTextColor.GREEN;
    private static final NamedTextColor COLOR_NAV_ACTIVE = NamedTextColor.YELLOW;
    private static final NamedTextColor COLOR_NAV_INACTIVE = NamedTextColor.DARK_GRAY;
    private static final NamedTextColor COLOR_HOVER_LABEL = NamedTextColor.GRAY;

    // 游戏模式颜色映射
    private static final NamedTextColor MODE_SURVIVAL_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor MODE_CREATIVE_COLOR = NamedTextColor.AQUA;
    private static final NamedTextColor MODE_ADVENTURE_COLOR = NamedTextColor.DARK_PURPLE;
    private static final NamedTextColor MODE_SPECTATOR_COLOR = NamedTextColor.GRAY;

    // Ping 阈值
    private static final int PING_GOOD_THRESHOLD = 50;
    private static final int PING_MEDIUM_THRESHOLD = 100;

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
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage(Component.text("当前没有在线玩家。", COLOR_GRAY));
            return true;
        }

        sortPlayers(onlinePlayers);

        int totalPlayers = onlinePlayers.size();
        int totalPages = (totalPlayers + playersPerPage - 1) / playersPerPage;

        int requestedPage = parsePage(args, totalPages);
        int startIndex = (requestedPage - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, totalPlayers);

        sendHeader(sender, totalPlayers, requestedPage, totalPages);

        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(formatPlayerEntry(onlinePlayers.get(i)));
        }

        sendPageNavigation(sender, requestedPage, totalPages, label);
        return true;
    }

    private void sortPlayers(List<Player> players) {
        if (SORT_BY_NAME.equalsIgnoreCase(sortBy)) {
            players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            players.sort((a, b) -> {
                Duration timeA = plugin.getPlayerPlayTime(a);
                Duration timeB = plugin.getPlayerPlayTime(b);
                return timeB.compareTo(timeA);
            });
        }
    }

    private int parsePage(@NotNull String[] args, int totalPages) {
        if (args.length == 0) return 1;
        try {
            int page = Integer.parseInt(args[0]);
            return Math.max(1, Math.min(page, totalPages));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void sendHeader(@NotNull CommandSender sender, int totalPlayers,
                            int page, int totalPages) {
        var builder = Component.text();
        builder.append(Component.text("=== 在线玩家列表 (", COLOR_TITLE));
        builder.append(Component.text(totalPlayers, COLOR_NUMBER));
        builder.append(Component.text("/", COLOR_TITLE));
        builder.append(Component.text(plugin.getServer().getMaxPlayers(), COLOR_NUMBER));
        builder.append(Component.text(") 第 ", COLOR_TITLE));
        builder.append(Component.text(page, COLOR_NUMBER));
        builder.append(Component.text("/", COLOR_TITLE));
        builder.append(Component.text(totalPages, COLOR_NUMBER));
        builder.append(Component.text(" 页 ===", COLOR_TITLE));
        sender.sendMessage(builder.build());
    }

    private Component formatPlayerEntry(Player player) {
        Duration playTime = plugin.getPlayerPlayTime(player);
        String playTimeStr = formatDuration(playTime);
        String displayName = getPlayerDisplayName(player);
        NamedTextColor nameColor = getAdminColor(player);
        GamemodeInfo modeInfo = getGamemodeInfo(player);

        TextComponent.Builder hoverBuilder = Component.text();
        appendHoverSection(hoverBuilder, "游戏模式", modeInfo.label, modeInfo.color);
        appendHoverSection(hoverBuilder, "在线时长", playTimeStr, COLOR_PLAYTIME);
        appendHoverSection(hoverBuilder, "世界", player.getWorld().getName(), COLOR_NUMBER);

        return Component.text("  ")
                .append(Component.text(displayName, nameColor))
                .append(Component.text(" ", COLOR_GRAY))
                .append(Component.text(modeInfo.label, modeInfo.color))
                .append(Component.text(" - ", COLOR_GRAY))
                .append(formatPing(player))
                .append(Component.text(" - ", COLOR_GRAY))
                .append(Component.text(playTimeStr, COLOR_PLAYTIME))
                .hoverEvent(HoverEvent.showText(hoverBuilder.build()));
    }

    private String getPlayerDisplayName(Player player) {
        return player.getPlayerListName() != null ? player.getPlayerListName() : player.getName();
    }

    private NamedTextColor getAdminColor(Player player) {
        return plugin.isAdmin(player) ? COLOR_NAME_ADMIN : COLOR_NAME_PLAYER;
    }

    private GamemodeInfo getGamemodeInfo(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE:
                return new GamemodeInfo("[创造]", MODE_CREATIVE_COLOR);
            case ADVENTURE:
                return new GamemodeInfo("[冒险]", MODE_ADVENTURE_COLOR);
            case SPECTATOR:
                return new GamemodeInfo("[旁观]", MODE_SPECTATOR_COLOR);
            default:
                return new GamemodeInfo("[生存]", MODE_SURVIVAL_COLOR);
        }
    }

    private static class GamemodeInfo {
        final String label;
        final NamedTextColor color;

        GamemodeInfo(String label, NamedTextColor color) {
            this.label = label;
            this.color = color;
        }
    }

    private void appendHoverSection(TextComponent.Builder builder, String label,
                                    String value, NamedTextColor color) {
        builder.append(Component.text(label + ": ", COLOR_HOVER_LABEL));
        builder.append(Component.text(value, color));
        builder.append(Component.newline());
    }

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

    private Component formatPing(Player player) {
        int ping = plugin.getPlayerPing(player);
        NamedTextColor color = getPingColor(ping);
        return Component.text(ping + "ms", color);
    }

    private NamedTextColor getPingColor(int ping) {
        if (ping < PING_GOOD_THRESHOLD) return NamedTextColor.GREEN;
        if (ping < PING_MEDIUM_THRESHOLD) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    private void sendPageNavigation(@NotNull CommandSender sender, int currentPage,
                                    int totalPages, @NotNull String commandLabel) {
        if (totalPages <= 1) return;

        TextComponent.Builder nav = Component.text();

        // 上一页
        if (currentPage > 1) {
            nav.append(Component.text("[<] ", COLOR_NAV_ACTIVE)
                    .clickEvent(ClickEvent.runCommand("/" + commandLabel + " " + (currentPage - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("上一页", COLOR_GRAY))));
        } else {
            nav.append(Component.text("[<] ", COLOR_NAV_INACTIVE));
        }

        // 下一页
        if (currentPage < totalPages) {
            nav.append(Component.text("[>]", COLOR_NAV_ACTIVE)
                    .clickEvent(ClickEvent.runCommand("/" + commandLabel + " " + (currentPage + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("下一页", COLOR_GRAY))));
        } else {
            nav.append(Component.text("[>]", COLOR_NAV_INACTIVE));
        }

        // 页码指示器
        nav.append(Component.text("  " + currentPage + " / " + totalPages + "  ", COLOR_GRAY));

        sender.sendMessage(nav.build());
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender,
                                               @NotNull Command command,
                                               @NotNull String alias,
                                               @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("1", "2", "3");
        }
        return List.of();
    }
}
