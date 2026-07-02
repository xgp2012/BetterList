# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

BetterList 是一个 Minecraft Paper 服务端插件，替代原生 `/list` 指令，提供更丰富的在线玩家信息展示（名称、游戏模式、Ping、在线时长），支持分页和排序。

## 架构

```
src/main/java/com/csbetterlist/
├── BetterListPlugin.java   # 插件主类 (JavaPlugin + Listener)
│   ├── 管理玩家加入/离开事件，维护 playerJoinTimes (Map<UUID, Long>)
│   ├── 提供 getPlayerPlayTime()、isAdmin()、getPlayerPing() 辅助方法
│   └── onEnable() 中读取配置，注册事件监听器和命令执行器
└── ListCommand.java        # 命令执行器 + Tab 补全 (CommandExecutor + TabCompleter)
    ├── onCommand() 负责排序、分页、格式化输出
    ├── formatPlayerEntry() 单行玩家信息（名称+模式+Ping+时长，含 hover 提示）
    ├── formatDuration() / formatPing() 格式化工具
    └── sendPageNavigation() 可点击的翻页导航栏
```

资源文件：
- `plugin.yml` — 插件描述、`/list` 命令注册、权限节点
- `config.yml` — 每页数量、排序方式、游戏模式中文标签

## 常用命令

```bash
# 构建（需要 JDK 21+）
mvn clean package

# 构建产物
target/cs-betterlist-1.x.0.jar
```

## 开发注意事项

- **兼容性**: Paper 1.14 - 1.21.x（`api-version: '1.14'`）
- **文字**: 使用 `net.kyori.adventure` 库构建 Component 树（颜色、点击事件、悬浮提示）
- **配置读取**: `playersPerPage` 和 `sortBy` 在 `onEnable()` 时从 config.yml 读取，传入 ListCommand 构造函数，之后不可变
- **配置未使用字段**: `config.yml` 中有 `admin-permission` 字段但未实际读取使用（admin 判断硬编码为 `betterlist.admin`）
- **排序**: 默认按在线时长降序，`sort-by: "name"` 时按名称字母序
- **分页**: 页码从 1 开始，非法输入回退到第一页
