# The Shanghai Wetro Mod — 1.20.1 移植版

[![Mod Environment](https://img.shields.io/badge/environment-Fabric%20%26%20Forge-1976d2)](https://modrinth.com/mod/shwetro-1201)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62b472)](https://modrinth.com/mod/shwetro-1201)
[![Requires MTR 3.x](https://img.shields.io/badge/requires-MTR%203.x-e0484a)](https://modrinth.com/mod/minecraft-transit-railway)
[![License MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

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
## 更新历史（1.20.1 移植版）

| 版本 | 内容 |
|---|---|
| 1.1.16 | 修复 Fence / PSDTop mirror / UndersignTip / Wall 的 rotate/mirror 覆盖 |
| 1.1.15 | 版本号递增 |
| 1.1.14 | 版本号递增 |
| 1.1.13 | 版本号递增 |
| 1.1.12 | 版本号递增 |
| 1.1.11 | 为 BlockShanghaiStationNameOnWall 补 FACING 守卫 + rotate/mirror 覆盖 |
| 1.1.10 | 同步 Main.MOD_VERSION 与 gradle.properties |
| 1.1.9 | 版本号递增 |
| 1.1.8 | 版本号递增；Gradle 堆内存降至 -Xmx2G（适配构建机） |
| 1.1.7 | 修复 Fence 斜面镜像 blockstates；同步 MOD_VERSION；新增 checkModVersion 校验 |
| 1.1.6 | 性能 batch 3：RMG 缓存键改用字符串拼接（消除 3× String.format）；getSHRoutes 键改用 StringBuilder（消除逐帧 MD5）；注意：缺字形症状仅出现在 headless runClient，正式 jar 无此问题 |
| 1.1.5 | 性能 batch 2：PSD 门 / 栏杆门 / 指示牌渲染器静态纹理缓存；热路径 19→1 分配 |
| 1.1.4 | 性能：缓存 RenderSpecialPassing 热路径全部 12 个逐帧 ResourceLocation 分配 |
| 1.1.3 | 清理全部 12 处反编译 (Object) 强转产物；build.ps1 loom 预热 |
| 1.1.2 | Forge mods.toml displayURL + 描述增强；实体工厂参数名修正 |
| 1.1.1 | 版本号递增 |
| 1.1.0 | shm_route_flipped 镜像路线图指示牌（对向站台） |
| 1.0.9 | 禁止携带物品告示牌全幅 UV 显示修复 |
| 1.0.8 | 控制台输出统一 slf4j |
| 1.0.7 | GL 纹理压缩 mixin 恢复（动态贴图省显存） |
| 1.0.6 | shm_route 路线图指示牌正式接入 MTR 自定义指示牌编辑器 |
| 1.0.5.1 | 禁止携带物品告示牌支持刷子右键旋转 |
| 1.0.5 | 新方块：上海地铁禁止携带物品告示牌；异常日志化 |
| 1.0.4 | 活塞行为适配 1.20.1 API；产物命名规范化 |
| 1.0.3 | 信仰灯贴图剔除修复；方向语义批量修正 |
| 1.0.2 | 放置崩溃修复（朝向取值错配） |
| 1.0.1 | 语言条目补全 |