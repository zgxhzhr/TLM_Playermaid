# 人是狐（playermaid）· NeoForge 1.21.1

把玩家变成「女仆」的身份系统模组，NeoForge 1.21.1 专用（硬依赖车万女仆 Touhou Little Maid）。

## 功能一览

- **主界面**：人是狐状态下按背包键（默认 E）打开主人界面，顶部 36 格直接映射目标玩家真实背包（上半 27 格主背包、下半 9 格快捷栏），底部为访客背包。
- **设置界面（不依赖调试器）**：`/playermaidgui` 打开自己的设置界面，**任何玩家可用、无需 OP**；`/playermaidgui <玩家>` 帮别人配置，需要 **OP 等级 ≥ 2**。界面内可开关人是狐、改渲染名、**选择主人**、调好感度（0–384）、切换日程、开关无敌，确认后由服务端校验保存。
- **认主与权限**：在设置界面点「选择主人…」展开在线玩家列表选定主人后，该玩家成为真实主人实体——**此后只有主人能右键打开该玩家的女仆背包**（未认主时维持原来的开放规则）。
- **主人联动饰品**：认主后，依赖主人实体的女仆饰品按万法皆通原版规则完整生效——双心之链由主人分担一半伤害（超 32 格不分担；配梦云水晶时主人不承担）、泣血之心让主人一并回血、发簪把伤害转移给主人（按主人实际掉血回填）、法术强化核心复制主人的铁魔法属性、梦云水晶把「全法术精通」进度授予主人。未认主或主人不在场时退回各自的降级行为。
- **名牌/渲染名**：人是狐玩家头顶名牌仅在准星对准时显示；渲染名由设置界面（或调试器）写入，头顶名牌与 Jade 标题同步。
- **聊天气泡**：随机颜文字与图片表情（复用女仆表情贴图），悬浮头顶正上方，与任何模型模组渲染方式无关。
- **魂符收容**：空魂符（`touhou_little_maid:smart_slab_empty`）右键人是狐玩家可收容：
  - 被收容玩家传送进收容维度，收到提示「你被收容到魂符中」；魂符变为装有人的形态。
  - 装有人的魂符右键（收容者本人）可放出，被收容玩家被传送回点击者旁边。
  - 装有人的魂符不能丢出背包，也不能转移进箱子、漏斗、潜影盒等非背包容器。
- **收容维度**（`playermaid:containment`，数据包注册）：
  - 18×18×18 基岩外壳、内衬去皮橡木、内部 16×16×16 空腔，光照恒为满亮。
  - 无自然生成、禁放方块、禁止使用物品（吃食物除外）。
  - 破坏方块瞬间破坏并立刻原样补回；连续破坏 10 次提示「魂符似乎发生了未知的变化」；
    连续破坏 20 次挣脱魂符：被传送回收容者旁边、魂符消失，双方收到提示。

## 构建

需要 **JDK 21**（工程已把 `org.gradle.java.home` 钉在 JDK 21；系统默认的 Java 25 会让 buildSrc 的 Groovy 编译失败）。

```
./gradlew :neoforge:build -x test --offline
```

产物位于 `neoforge/build/libs/playermaid-neoforge-1.21.1-<version>.jar`。

> 注：`libs/` 下的第三方模组 jar（车万女仆、Jade、Curios）不入库，克隆后需自行放置以满足编译。
> 这些仅用于编译期解析，运行期由整合包提供。

## 可选依赖

- Jade：准心女仆信息面板（主人 / 日程 / 好感度等级 / 还需点数 / 无敌）与面板底部模组名。缺席时相关 Mixin/插件自动跳过。
- Curios：女仆侧 Curios 兼容按钮。缺席时按钮不出现。

## 移植备注

本工程由 1.20.1 Forge 版（`TLM_playermaid-1.20.1`）移植而来，模板取自 MultiLoader-Template-1.21.1，
已精简为 `common` + `neoforge` 两个模块（fabric / forge 模块移除）。移植中的主要 API 变化：

- 网络层：`SimpleChannel` → `CustomPacketPayload` + `RegisterPayloadHandlersEvent` + `PacketDistributor`
- 事件：`LivingAttackEvent` / `LivingHurtEvent` 合并为 `LivingIncomingDamageEvent`；`TickEvent.PlayerTickEvent` → `PlayerTickEvent.Post`
- 注册体系：`RegistryObject` → `DeferredHolder` / `DeferredItem`；`IForgeMenuType` → `IMenuTypeExtension`
- 物品数据：`ItemStack.getTag()` → DataComponents（梦云水晶无敌倒计时改为自定义数据组件）
- 属性：`AttributeModifier` 记录化（UUID → `ResourceLocation` id）；`Attribute` → `Holder<Attribute>`
- 客户端：`MenuScreens.register` 私有化 → `RegisterMenuScreensEvent`；`DistExecutor` 移除 → `FMLEnvironment.dist`；
  `ImageButton` / `StateSwitchingButton` 的贴图坐标构造被移除，改用 TLM 自带的 `TouhouImageButton` / `TouhouStateSwitchButton`
- 顶点缓冲：`VertexConsumer.vertex/uv/uv2/color/endVertex` → `addVertex/setUv/setLight/setColor`

## 许可证

[MIT License](LICENSE) — Copyright (c) 2026 逐光星火_执火人
