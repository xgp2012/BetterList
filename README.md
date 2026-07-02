# CS-BetterList

替代 Minecraft Paper 服务端原生 `/list` 指令的插件，提供更丰富的在线玩家信息展示。

## 功能

- 查看在线玩家列表（名称、游戏模式、Ping 延迟、在线时长）
- 管理员玩家名称以红色高亮显示
- 按在线时长降序排序，或按玩家名称字母序排序
- 分页显示，支持可点击的翻页按钮
- 悬浮提示（鼠标悬停查看详细信息：游戏模式、在线时长、所在世界）
- Ping 延迟色彩标识（绿色 < 50ms / 黄色 < 100ms / 红色 ≥ 100ms）
- `/list` 命令输出仅执行者自己可见

## 兼容性

- Paper 1.21.8+
- Paper 衍生服务端包括 Leaves 和 Leaf

## 安装

1. 下载 `cs-betterlist-1.3.0.jar`
2. 放入服务器的 `plugins/` 目录
3. 重启服务器或执行 `/reload` 加载插件
4. 插件自动生成 `plugins/CS-BetterList/config.yml` 配置文件
5. 好了享受吧

## 命令

| 命令 | 说明 |
|------|------|
| `/list` | 显示第一页在线玩家列表 |
| `/list <页码>` | 显示指定页码的在线玩家列表 |

## 权限

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `bukkit.command.list` | 所有人 | 允许使用 `/list` 命令 |
| `betterlist.admin` | OP | 在 `/list` 中名称以红色标记为管理员 |

## 配置 (config.yml)

```yaml
# 每页显示的玩家数量（默认 10）
players-per-page: 10

# 排序方式: "playtime" (在线时长降序) 或 "name" (玩家名升序)
sort-by: "playtime"
```

## 构建

```bash
# 需要 JDK 21+
mvn clean package
```

构建产物位于 `target/cs-betterlist-1.x.0.jar`。

## 预览

```
=== 在线玩家列表 (3/20) 第 1/1 页 ===
  灯塔照耀海洋 [生存] - 12ms - 1小时23分45秒
  张三 [创造] - 68ms - 45分30秒
  李四 [冒险] - 120ms - 3分15秒
  [<]  1 / 1  [>]
```

## 许可证

MIT License

## 作者

tips：不会经常更新或添加新功能可以二创
**灯塔照耀海洋**
