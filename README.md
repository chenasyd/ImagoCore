# ImagoCore

通过资源包在 Minecraft 中显示自定义图像。支持 **GUI 背景图** 和 **通用字符图像** 两种模式。

---

## 目录

- [安装](#安装)
- [GUI 背景图像](#gui-背景图像)
  - [目录结构](#目录结构)
  - [添加背景图](#添加背景图)
  - [参数说明](#参数说明)
- [字符图像](#字符图像)
  - [目录结构](#目录结构-1)
  - [添加字符图](#添加字符图)
- [应用资源包](#应用资源包)
- [命令参考](#命令参考)
- [配置参考](#配置参考)
- [常见问题](#常见问题)

---

## 安装

1. 将 `imagocore-*.jar` 放入 `plugins/` 目录
2. 重启服务器，插件会自动创建所需目录和模板文件
3. 加载生成的资源包（见 [应用资源包](#应用资源包)）

> **兼容性**：Paper / Spigot / Folia 1.21+

---

## GUI 背景图像

在背包 GUI 标题栏显示自定义背景图。支持 9、18、27、36、45、54 格多种规格。

### 目录结构

```
plugins/ImagoCore/gui/
├── gui.yml              ← 主注册表（自动生成，只读）
├── 54/                  ← 54 格 GUI
│   ├── gui.yml          ← 条目定义
│   └── default.png      ← 图片文件
├── 27/
│   ├── gui.yml
│   └── default.png
├── 18/
├── 36/
├── 45/
└── 9/
```

### 添加背景图

1. 进入对应规格的文件夹，例如 `gui/54/`
2. 放入你的 PNG 图片，例如 `premium.png`
3. 编辑该文件夹下的 `gui.yml`，在 `entries` 下添加条目：

```yaml
# gui/54/gui.yml
slots: 54
defaults:
  ascent: 13
  height: 222
  shift_x: -8
entries:
  default:
    texture: "default.png"
  premium:                  # ← 新增条目
    texture: "premium.png"
```

4. 插件会自动分配 Unicode 字符并更新资源包
5. 使用 `/ic t g 54-premium` 测试效果

### 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `slots` | 背包槽位数 | 文件夹名 |
| `ascent` | 字体上行高度（像素） | 13 |
| `height` | 图片总高度（像素） | 222 |
| `shift_x` | 水平偏移（通常为负值，左移对齐） | -8 |

> 每个条目可单独覆盖上述默认值：
> ```yaml
> entries:
>   custom:
>     texture: "custom.png"
>     ascent: 20
>     height: 256
>     shift_x: -12
> ```

---

## 字符图像

比 GUI 背景更通用 —— 每个 PNG 获得一个 Unicode 字符，可在聊天、标题、计分板等**任意位置**使用。

### 目录结构

```
plugins/ImagoCore/char/
├── char.yml          ← 注册表（自动生成）
├── my_icon.png       ← 直接放 PNG 即可
├── logo.png
└── arrow.png
```

### 添加字符图

1. 把 PNG 图片丢进 `plugins/ImagoCore/char/` 目录
2. 重启服务器或执行 `/ic r b` 重建资源包
3. 查看 `char/char.yml` 获取分配的字符：

```yaml
# char/char.yml
defaults:
  ascent: 8
  height: 8
registrations:
  my_icon: "\uE900"
  logo: "\uE901"
  arrow: "\uE902"
```

4. 在游戏中使用分配的字符：

```
# 发送包含图标的消息
/tellraw @p {"text":"点击这里 \uE900 查看详情"}

# 在标题中显示图标
/title @p title {"text":"\uE901 欢迎回来 \uE901"}

# 通过插件代码使用
player.sendMessage("§f你的图标: \uE900");
```

### 调整显示大小

在 `char/char.yml` 中添加 `entries.<name>.ascent` 和 `entries.<name>.height`：

```yaml
# char/char.yml
defaults:
  ascent: 8
  height: 8
registrations:
  my_icon: "\uE900"
  logo: "\uE901"
entries:
  logo:
    ascent: 16
    height: 16
```

---

## 应用资源包

### 方式一：`server.properties`（推荐）

编辑服务器根目录下的 `server.properties`：

```properties
resource-pack=https://你的域名/build.zip
resource-pack-sha1=<SHA1哈希值>
require-resource-pack=false
```

### 方式二：手动加载

1. 将 `plugins/ImagoCore/build.zip` 分发给玩家
2. 玩家放入 `.minecraft/resourcepacks/` 并启用

### 方式三：本地开发测试

World 自带的 `server.properties` 支持本地协议：

```properties
resource-pack=file/plugins/ImagoCore/build.zip
```

> **注意**：每次修改图片或添加条目后，执行 `/ic r b` 重建 `build.zip`。插件会在启动时自动检测变化并重建。

---

## 命令参考

| 命令 | 说明 |
|------|------|
| `/imagocore resource build [输出路径]` | 构建资源包 zip |
| `/ic r b [输出路径]` | 同上（简写） |
| `/imagocore test gui [条目ID] [fill]` | 打开测试 GUI |
| `/ic t g` | 列出所有可用的 GUI 条目 |
| `/ic t g 54-default` | 打开 54 格默认 GUI |
| `/ic t g 54-default false` | 不填充物品（空背包） |

---

## 配置参考

```yaml
# plugins/ImagoCore/config.yml
verbose-logging: false
max-animations: 50
render-tick-interval: 1

gui:
  title:
    background_char: "\uE800"
    shift_char_coarse: "\uE801"
    shift_char_fine: "\uE802"
    shift_x: -8
    ascent: 13
    height: 222

resource-pack:
  output: "plugins/ImagoCore/build.zip"

char:
  defaults:
    ascent: 8
    height: 8
```

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `resource-pack.output` | zip 输出路径 | `plugins/ImagoCore/build.zip` |
| `char.defaults.ascent` | 字符图像默认上行高度 | 8 |
| `char.defaults.height` | 字符图像默认高度 | 8 |

---

## 常见问题

### 资源包加载后看不到图？

1. 确认已执行 `/ic r b` 重建资源包
2. 检查 `server.properties` 中的 `resource-pack` 地址是否可访问
3. 确认客户端已接受并启用了资源包
4. 图片 PNG 分辨率需要与字体参数匹配（如 54 格：宽约 -8px 偏移后的 GUI 宽度，高 222px）

### 修改图片后没有更新？

执行 `/ic r b` 手动重建，或重启服务器（插件启动时会自动检测变化）。

### 字符区段说明

- GUI 背景字符：`U+E800` ~ `U+E8FF`
- 字符图像：`U+E900` ~ `U+E9FF`
- 偏移字符：`U+E801`（-16px 粗调）、`U+E802`（-8px 精调）

这些都在 Unicode 私有使用区（PUA），不会与正常文字冲突。
