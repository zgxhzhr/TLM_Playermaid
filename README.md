# MultiLoader Template

## 模组说明（人是狐 playermaid）

Forge 1.20.1 专用模组，把「人是狐」做成可游玩的身份系统：

- **主界面**：人是狐状态下按背包键（默认 E）打开主人界面，顶部 36 格直接映射目标玩家真实背包（上半 27 格主背包、下半 9 格快捷栏），底部为访客背包。
- **名牌/渲染名**：人是狐玩家头顶名牌仅在准星对准时显示；渲染名由调试器（superdbg）写入，头顶名牌与 Jade 标题同步。
- **聊天气泡**：随机颜文字与图片表情（复用女仆表情贴图），悬浮头顶正上方，与任何模型模组渲染方式无关。
- **魂符收容**：空魂符（touhou_little_maid:smart_slab_empty）右键人是狐玩家可收容：
  - 被收容玩家传送进收容维度，收到提示「你被收容到魂符中」；魂符变为装有人的形态。
  - 装有人的魂符右键（收容者本人）可放出，被收容玩家被传送回点击者旁边。
  - 装有人的魂符不能丢出背包，也不能转移进箱子、漏斗、潜影盒等非背包容器。
- **收容维度**（playermaid:containment，数据包注册）：
  - 18×18×18 基岩外壳、内衬去皮橡木、内部 16×16×16 空腔，光照恒为满亮。
  - 无自然生成、禁放方块、禁止使用物品（吃食物除外）。
  - 破坏方块瞬间破坏并立刻原样补回；连续破坏 10 次提示「魂符似乎发生了未知的变化」；
    连续破坏 20 次挣脱魂符：被传送回收容者旁边、魂符消失，双方收到提示。

构建：`gradlew :forge:build -x test --offline`；部署到 `mods` 目录，更换 jar 后必须完全重启游戏。

---

This project provides a Gradle project template that can compile mods for both Forge and Fabric using a common sourceset. This project does not require any third party libraries or dependencies. If you have any questions or want to discuss the project join our [Discord](https://discord.myceliummod.network).

## Getting Started

## IntelliJ IDEA
This guide will show how to import the MultiLoader Template into IntelliJ IDEA. The setup process is roughly equivalent to setting up Forge and Fabric independently and should be very familiar to anyone who has worked with their MDKs.

1. Clone or download this repository to your computer.
2. Configure the project by editing the `group`, `mod_name`, `mod_author`, and `mod_id` properties in the `gradle.properties` file. You will also need to change the `rootProject.name`  property in `settings.gradle`, this should match the folder name of your project, or else IDEA may complain.
3. Open the template's root folder as a new project in IDEA. This is the folder that contains this README file and the gradlew executable.
4. If your default JVM/JDK is not Java 17 you will encounter an error when opening the project. This error is fixed by going to `File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM`and changing the value to a valid Java 17 JVM. You will also need to set the Project SDK to Java 17. This can be done by going to `File > Project Structure > Project SDK`. Once both have been set open the Gradle tab in IDEA and click the refresh button to reload the project.
5. Open the Gradle tab in IDEA if it has not already been opened. Navigate to `Your Project > Common > Tasks > vanilla gradle > decompile`. Run this task to decompile Minecraft.
6. Open your Run/Debug Configurations. Under the Application category there should now be options to run Forge and Fabric projects. Select one of the client options and try to run it.
7. Assuming you were able to run the game in step 7 your workspace should now be set up.

### Eclipse
While it is possible to use this template in Eclipse it is not recommended. During the development of this template multiple critical bugs and quirks related to Eclipse were found at nearly every level of the required build tools. While we continue to work with these tools to report and resolve issues support for projects like these are not there yet. For now Eclipse is considered unsupported by this project. The development cycle for build tools is notoriously slow so there are no ETAs available.

## Development Guide
When using this template the majority of your mod is developed in the Common project. The Common project is compiled against the vanilla game and is used to hold code that is shared between the different loader-specific versions of your mod. The Common project has no knowledge or access to ModLoader specific code, apis, or concepts. Code that requires something from a specific loader must be done through the project that is specific to that loader, such as the Forge or Fabric project.

Loader specific projects such as the Forge and Fabric project are used to load the Common project into the game. These projects also define code that is specific to that loader. Loader specific projects can access all of the code in the Common project. It is important to remember that the Common project can not access code from loader specific projects.
