# The Shanghai Wetro Mod — 1.20.1 移植版

**[Modrinth 下载](https://modrinth.com/mod/shwetro-1201)** | [GitHub Release](https://github.com/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases)

[MTR Shanghai Wetro Addon (shwetro)](https://modrinth.com/mod/shwetro) 的 **Minecraft 1.20.1** 移植工程（Fabric + Forge 双加载器）。

原作：TKP30 / DF11Z-003A / MMCJelly（MIT License）
1.20.1 移植：基于 1.19.4 发行版 jar 反编译 + 双版本 Mojang 官方映射桥接转换，适配 MTR 3.2.2（1.20.1）。

## 依赖

| 组件 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Minecraft Transit Railway (MTR) | 3.2.2（1.20.1 版，3.x 系） |
| Fabric Loader / Fabric API | 0.14.21+ / 0.92.2+1.20.1 |
| Forge | 47.x（1.20.1） |

> **注意**：不支持 MTR 4.x。

## 构建

```bash
# 预备：把 MTR 3.2.2 的 1.20.1 dev jar 放到 checkouts/1.20.1/
#   mtr-common.jar / mtr-fabric.jar / mtr-forge.jar
#   https://aphrodite281.github.io/Minecraft-Transit-Railway-3.x.x/lib/1.20.1/
./gradlew build
```

产物：
- `fabric/build/libs/fabric-<version>.jar`
- `forge/build/libs/forge-<version>.jar`

## 1.19.4 → 1.20.1 主要适配点

- `Material`/`MaterialColor` 移除 → `Properties.of().mapColor(MapColor).sound(SoundType)`
- `BlockBehaviour.Properties.lightEmission` → `lightLevel`；`instab` → `instabreak`
- `Screen.render(PoseStack)` → `render(GuiGraphics)`（GuiGraphics 于 1.20.1 引入）
- `Tesselator.getBuffer()` → `getBuilder()`；`PoseStack$Pose.last()` → `pose()`
- `Entity.level` 私有化 → `serverLevel()`；`getVisualOrder` → `getVisualOrderText`
- `StateHolder.cycle` 语义变化（返回新状态而非属性值）
- `Block.asItem` 移除 → `BlockBehaviour.asItem`
- mixin：GlStateManager 纹理压缩修复 mixin 因映射不可解析暂时禁用（上游遗留 @Deprecated）

## 已验证

- ✅ Fabric 1.20.1 + MTR 3.2.2-hotfix-1 dev 环境 `runClient` 完整加载：
  - `The Shanghai Wetro Mod v1.0.1 loaded`
  - 全部 14 个 Blockbench 模型加载
  - DynamicRefreshMixin / MTRSignMixin 应用
  - 方块/物品/方块实体注册完整，资源加载仅剩上游既有警告（本版已修复 door+door 与 wall_pillar 的 blockstate 缺陷）
- ✅ Forge 1.20.1 (47.3.0) 编译产出

## 许可

MIT（随上游）

## 下载

| 渠道 | 地址 |
|---|---|
| Modrinth（推荐） | https://modrinth.com/mod/shwetro-1201 |
| GitHub Release | https://github.com/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases |

文件命名：`shmetro-<模组版本>-<游戏版本>-<加载器>.jar`

## 常见问题

**放置方块时游戏崩溃？**
请使用 1.0.2 及以上版本（放置朝向崩溃已在 1.0.2 修复）。

**信仰灯/空调外机下方贴图消失？**
请使用 1.0.3 及以上版本（方块遮挡剔除属性已在 1.0.3 修复）。

**指示牌连锁破坏异常？**
1.0.3 修复了方向语义。旧版本放置的指示牌数据是坏的，请拆掉重新放置。

**屏蔽门被活塞推动？**
1.0.4 已修复（活塞行为属性适配 1.20.1 API）。