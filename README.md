# GlideClient Forge

[English](README_en.md)

GlideClient Forge 是基于 [GlideClient](https://github.com/GlideClient/client)
移植的独立 Minecraft Forge 1.8.9 客户端模组。

本项目只支持 Forge。最终构建产物是一个放入 `mods` 目录的客户端模组
JAR，不提供原版 LaunchWrapper 版本或自定义启动器 JSON。尚无 Forge 1.8.9
等价接口的底层功能仍使用少量 Mixin。

本项目是独立社区项目，并非 GlideClient、Minecraft、Forge 或 OptiFine
官方版本，也未获得上述项目的认可或背书。

## 项目状态

当前候选版本为 `7.2-forge.2`，目标环境如下：

- Minecraft 1.8.9
- Forge 11.15.1.2318
- OptiFine 1.8.9 HD U M6 pre2
- Java 8

干净构建会运行单元测试、Linux Unix Domain Socket 集成测试和生产 JAR
结构检查。

Arch Linux 运行基线使用 Java 8u502、KDE Wayland（Minecraft 通过
XWayland 运行）和 NVIDIA GPU。组合测试同时加载了 OptiFine、Myau、
Essential/OneConfig、Keystrokes、NotEnoughUpdates、PolyPatcher 和
SimpleToggleSprint，并验证以下内容可同时显示：

- 游戏世界正常渲染；
- Myau Radar 和模块列表；
- Keystrokes；
- Glide 准星；
- Glide 现代热键栏；
- 客户端正常关闭。

这只是当前测试组合的回归基线，不代表任意 Forge 模组或任意 OptiFine
版本都一定兼容。Essential/OneConfig 在组合环境中仍会输出已知的第三方
Mixin 警告，但这些警告没有阻止进入世界、绘制 HUD 或正常关闭。

## 当前实现

- 通过普通 Forge `mods` 目录加载。
- 提供 Forge `@Mod` 生命周期入口和按 Forge 顺序执行的 Mixin Tweaker。
- 在 Forge 安装运行时反混淆转换器后初始化内置 Mixin，并使用 `searge`
  混淆上下文。
- 通过 Forge 事件桥接客户端 Tick、渲染 Tick、键鼠输入、世界生命周期、
  玩家与实体更新、攻击、跳跃、FOV、声音、菜单、世界/玩家/实体渲染、
  方块高亮、第一人称覆盖层和 HUD。
- Glide 替换原版 HUD 元素时仍发送 Forge Overlay `Post` 事件，使第三方
  HUD 监听器可以继续绘制。
- 隔离单个 Glide 事件处理器、NanoVG、模糊和现代热键栏的异常，避免一个
  功能失败后阻断整个 HUD。
- 处理 OptiFine Forge API stub 和 Vecmath 类加载冲突。
- 使用公共与 Windows 专用的独立 Mixin 配置，Linux 和 macOS 不加载
  Windows LWJGL Display 注入。
- Discord IPC 在 Windows 使用命名管道，在 Linux/macOS 使用 Unix
  Domain Socket，并限制数据包大小、完整处理短读写。

## 平台支持

- **Linux/X11、Linux/Wayland + XWayland：** 已在 Arch Linux 实机测试。
  Wayland 环境下，无边框全屏模块不会执行 X11 窗口定位操作，而是交给
  Minecraft/LWJGL 处理。
- **Windows 10/11：** 保留支持。CI 会在 Windows 编译并运行平台无关
  测试，但正式稳定版发布前仍需要进行真实 Minecraft 运行回归。
- **macOS：** 已实现桌面集成和 Discord Unix Socket，但尚未纳入完整
  Minecraft 运行测试矩阵。

Minecraft 1.8.9 必须使用 Java 8。LWJGL 2 不提供原生 Wayland 后端，
Wayland 桌面需要 XWayland。

任何项目都无法保证兼容所有会修改相同 Minecraft 方法、或者内置不同
Mixin 运行时的模组。提交兼容性问题时，请提供 Minecraft、Forge、
OptiFine、Java、操作系统版本及日志中的第一个相关异常。

## 安装

1. 安装 Forge `1.8.9-11.15.1.2318`。
2. 将 `GlideClient-Forge-7.2-forge.2.jar` 放入该实例的 `mods` 目录。
3. 启动普通 Forge 1.8.9 配置。

该 JAR 同时包含剩余 Mixin 所需的早期启动逻辑和常规 `glideclient` Forge
模组容器。它只在客户端使用，不需要安装到服务端。

## 构建

### 环境要求

- JDK 8
- Git
- 可访问旧版 ForgeGradle 和 Maven 仓库的网络

Linux 或 macOS：

```bash
./gradlew clean build --console=plain
```

Windows：

```bat
gradlew.bat clean build --console=plain
```

可分发产物位于：

```text
build/libs/GlideClient-Forge-7.2-forge.2.jar
```

`./gradlew smokeJar` 会额外生成仅用于自动化测试的控制器 JAR。该文件不得
作为模组发行，也不应安装到普通用户实例。

创建 IntelliJ 开发环境：

```bash
./gradlew setupDecompWorkspace
./gradlew genIntellijRuns
```

ForgeGradle 2.1 和 Gradle 4.10.3 均为旧工具链，请始终使用 Java 8。

## 运行回归测试

Linux smoke runner 会把当前生产 JAR 和测试控制器部署到隔离的游戏目录，
直接进入指定单人世界，执行 Mixin 运行时审计，捕获 XWayland 窗口，扫描
日志并等待客户端正常退出：

```bash
./gradlew clean build smokeJar --console=plain
tools/linux/run-world-smoke.sh \
  --version-dir "/path/to/.minecraft/versions/GlideClient-Forge" \
  --game-dir "/path/to/isolated-game-dir" \
  --world "New World"
```

测试 OptiFine 时，将选定的 Forge 1.8.9 OptiFine JAR 放入隔离目录的
`mods` 文件夹并增加 `--require-optifine`。测试器会同时检查 OptiFine
版本标记和 Forge 的 OptiFine 检测日志。

隔离游戏目录必须预先包含指定存档和待测模组组合。截图验证依赖
`xdotool` 与 ImageMagick。脚本发生失败或超时时会终止它启动的客户端。
Windows 测试辅助脚本位于 `tools/windows/`。

## 启动架构

JAR Manifest 在 Forge 主 Tweaker 之后注册 `GlideMixinTweaker`。启动器会：

1. 确认 Forge 运行时反混淆转换器已就绪；
2. 准备受限的 Vecmath/LWJGL 类加载兼容边界；
3. 委托内置 Mixin Tweaker；
4. 注册 `mixins.soar.json`；
5. 仅在 Windows 注册 `mixins.soar.windows.json`；
6. 由 Forge 扫描 `GlideForgeMod`，并在 Forge/FML Event Bus 注册
   `ForgeEventBridge`。

当前仍属于混合实现：安装、生命周期、输入、世界事件和共享 HUD 路径已使用
Forge；Forge 1.8.9 没有等价事件的底层渲染及游戏行为仍使用 Mixin。

## 路线图

1. 扩展更多独立模组组合以及生存/创造 HUD 的自动化回归测试。
2. 在 Forge 1.8.9 提供等价事件时继续替换剩余 Hook。
3. 逐步减少类路径转换器和 Mixin，而不牺牲现有功能。
4. 增加真实 Windows Minecraft 运行测试；当前 GitHub CI 只覆盖 Windows
   编译和单元测试。

## 上游与归属

本仓库基于 [`GlideClient/client`](https://github.com/GlideClient/client)，
起始上游提交为 `695c208`。GlideClient 的维护者将其描述为 Soar Client
的更新版本。

原项目使用 GNU General Public License version 3，本衍生项目继续使用
相同许可证。原有第三方许可证和声明保留在对应源码及资源目录。修改和归属
说明见 [NOTICE.md](NOTICE.md)，内置依赖说明见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。依赖许可证也包含在生产
JAR 的 `META-INF/licenses/` 中。

## 贡献

请保持改动范围明确。修改共享 Mixin、渲染或启动逻辑时，必须构建生产 JAR
并运行对应回归测试。

## 许可证

GlideClient Forge 使用 [GNU General Public License version 3](LICENSE)
发布。分发源码或修改后的二进制文件时，必须继续遵守 GPLv3，包括提供对应
源码。
