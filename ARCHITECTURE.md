# ImagoCore 架构文档

> 图像显示驱动核心 — 为其他插件提供 GUI 图像覆盖、实体图像显示及动画能力。

---

## 项目结构

```
ImagoCore/
├── ARCHITECTURE.md                          # 本文件 — 架构 & 约定
├── i18n-missing-keys.txt                    # 国际化缺失键记录
├── pom.xml                                  # Maven 构建
├── .codebuddy/rules/development-rules.md    # CodeBuddy 开发规则
└── src/main/
    ├── java/org/a/imagoCore/
    │   ├── ImagoCore.java                   # 插件主入口
    │   │
    │   ├── config/
    │   │   ├── ConfigManager.java           # 配置管理器（热重载）
    │   │   └── MainConfig.java              # 类型安全配置读取
    │   │
    │   ├── scheduler/
    │   │   └── CompatibleScheduler.java     # Spigot/Folia 统一调度
    │   │
    │   ├── image/
    │   │   ├── ImageRenderer.java           # 渲染接口
    │   │   ├── display/
    │   │   │   ├── ImageDisplay.java        # 显示抽象接口
    │   │   │   ├── entity/
    │   │   │   │   └── EntityImageDisplay.java # 物品框/地图实体显示
    │   │   │   └── gui/
    │   │   │       ├── GuiImageDisplay.java    # GUI 物品显示
    │   │   │       └── GuiTitleRenderer.java   # GUI 标题图像渲染
    │   │   └── animation/
    │   │       └── AnimationDriver.java     # 动画驱动（帧序列）
    │   │
    ├── event/
    │   └── ImageEvent.java              # 事件基类
    │
    ├── command/
    │   ├── SubCommand.java              # 子命令接口
    │   ├── ImagoCoreCommand.java        # /imagocore 主命令调度
    │   ├── test/
    │   │   ├── TestCommand.java         # test 子命令
    │   │   ├── TestAction.java          # test 动作接口
    │   │   └── GuiTestHandler.java      # test gui 处理器
    │   └── resource/
    │       └── ResourceCommand.java     # resource build 命令
    │
    └── resource/pack/
        ├── FontProvider.java            # 字体提供者 JSON 模型
        ├── GuiFontDefinition.java       # font/default.json 构建器
        └── ResourcePackGenerator.java   # 资源包 → build.zip 生成器
│
└── resources/
    ├── plugin.yml                       # 插件描述
    ├── config.yml                       # 默认配置（热重载）
    └── resource-pack-template/          # 资源包模板
        ├── pack.mcmeta
        ├── pack.png
        └── assets/minecraft/
            ├── font/default.json        # (运行期动态生成)
            └── textures/textures/gui/
                └── custom_gui_54.png
```

---

## 关键约定

### 配置读取

| 方式 | 访问 | 热重载 | 适用场景 |
|---|---|---|---|
| `ConfigManager.getMainConfig()` | `plugin.getConfigManager().getMainConfig()` | ✅ `/guildadmin reload` | 运行时配置 |
| `Bukkit getConfig()` | `plugin.getConfig()` | ❌ | 仅 `onEnable` 引导 |

### 调度器

所有异步/定时逻辑**必须**使用 `CompatibleScheduler`，禁止直接调用 BukkitScheduler：

```java
CompatibleScheduler sched = plugin.getScheduler();
sched.runTask(plugin, runnable);                    // 主线程/全局区域
sched.runTask(plugin, entity, runnable);             // 实体区域（Folia 安全）
sched.runTaskTimer(plugin, entity, runnable, 0L, 1L);// 实体区域重复
sched.runTaskLater(plugin, runnable, 20L);           // 延迟 1 秒
sched.runTaskAsync(plugin, runnable);                // 异步
sched.cancelAll(plugin);                             // onDisable 清理
```

### Folia 检测

- 运行时通过 `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` 自动检测。
- `scheduler.isFolia()` 可查询当前环境。
- `plugin.yml` 已设置 `folia-supported: true`。

### 实体操作安全

在 Folia 环境中，跨区域操作实体（如 `Entity#teleport`、`ItemFrame#setItem`）**必须**使用实体级调度器：

```java
// ❌ 错误 — Folia 可能抛异常
entity.teleport(location);

// ✅ 正确
sched.runTask(plugin, entity, () -> entity.teleport(location));
```

### 事件

- 自定义事件基类：`ImageEvent extends Event`
- 事件包：`org.a.imagoCore.event`
- 命名规范：`<动作><领域>Event`，如 `ImageLoadEvent`、`ImageDisplayUpdateEvent`

### 国际化

- 代码中日志/消息**硬编码英文**
- 中文翻译写入 `i18n-missing-keys.txt`，格式：
  ```
  image.load.failed=图像加载失败
  animation.stopped=动画已停止
  ```

---

## 资源包生成

### 命令

```
/imagocore resource build [output]     # 完整
/ic r b                                # 别名
```

输出 `build.zip`（默认 `build/build.zip`，路径相对于服务端根目录）。

### 模板

资源包模板嵌入在 `src/main/resources/resource-pack-template/`，包含：
- `pack.mcmeta` — pack_format 48 (MC 1.21)
- `pack.png` — 资源包图标
- `assets/minecraft/textures/textures/gui/custom_gui_54.png` — GUI 背景纹理

### 动态字体生成

`assets/minecraft/font/default.json` 由 `GuiFontDefinition` 运行时动态生成，包含：

| Unicode | 类型  | 用途       |
|---------|-------|------------|
| \uE801  | space | 粗调偏移   |
| \uE802  | space | 微调偏移   |
| \uE800  | bitmap| GUI 背景图 |

### 扩展

添加新字体条目（如自定义小图标）：

```java
ResourcePackGenerator gen = new ResourcePackGenerator(plugin, file, guiTitle);
gen.getFontDefinition().addProvider(
    FontProvider.bitmap("minecraft:textures/custom/icon.png", 8, 8, "\uE003")
);
gen.build();
```

---

## 设计决策

### 为什么用接口 + 实现而非继承链？

- 接口 `ImageDisplay` 定义了"一个显示表面"的行为契约。
- `EntityImageDisplay` / `GuiImageDisplay` 各自实现，不共享父类。
- 新增显示类型（例如 `MapImageDisplay`）只需实现接口，不破坏现有结构。

### 为什么 CompatibleScheduler 不继承 BukkitScheduler？

- `RegionizedScheduler`（Folia）与 `BukkitScheduler`（Spigot）没有共同接口。
- 使用组合 + 委托模式，提供统一的窄接口（Facade），隐藏两种实现差异。

### 为什么 ConfigManager 用 `saveDefaultConfig()` + `reloadConfig()`？

- 标准 Bukkit API 自带热加载链路：
  - `saveDefaultConfig()` — 首次运行时写入 `config.yml`
  - `reloadConfig()` — 重新读取磁盘，刷新 `FileConfiguration`
- `MainConfig` 封装了 `ConfigurationSection`，每次 get 从实时对象读取，天然支持热重载。

---

## 接入指南（面向其他插件）

其他插件通过软依赖（`softdepend: [ImagoCore]`）接入：

```java
// 获取 ImagoCore 实例
ImagoCore core = (ImagoCore) Bukkit.getPluginManager().getPlugin("ImagoCore");
if (core == null) return; // ImagoCore 未加载

// 创建显示
ImageRenderer renderer = new MyRenderer();
ImageDisplay display = new EntityImageDisplay(worldId, entityId, renderer, "my-image");

// 播放动画
AnimationDriver anim = new AnimationDriver(display, frames, 1, AnimationDriver.Mode.LOOP);
anim.start();
```

---

## 开发者

- **Java 21**
- **Paper API 1.21.11**（编译依赖）
- **Maven**（`mvn clean package` 构建）
- **兼容性**: Spigot 1.21+ / Folia
