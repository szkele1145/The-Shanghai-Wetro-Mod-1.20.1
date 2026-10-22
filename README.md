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
| 1.1.68 | 修好 model→texture 链的空检查（425 条引用首次真正被检查）+ 新增 Java 资源引用链 + 资源路径大小写敏感 |
| 1.1.67 | 新增 `checkMixinTargets`（校验 mixin 注入目标的方法还在，MTR 改名时构建期拦截）|
| 1.1.66 | 新增 `checkMixinPackaging` / `checkTooling` 两条验证链（查发布 jar 里的 mixin 声明、类、refmap）|
| 1.1.65 | 补回 4 处被移植退化掉的泛型（原始 List/ArrayList/Map 强转 → 恢复类型检查）|
| 1.1.64 | `checkResourceLinks` 增加 loot table 对应关系检查（漏掉落表 / 悬空掉落表双向拦截）|
| 1.1.63 | 改 `checkResourceLinks` 补齐拼接式翻译 key 的检查（三元表达式 / 前缀拼接里的 key 之前抓不到）|
| 1.1.62 | 新增 `checkResourceLinks` 构建期资源断链检查（注册名↔blockstate→model→texture、lang key 五类链）|
| 1.1.61 | 修 GD 编号文本渲染 null 防护（防渲染批次被打断）+ 格式残渣清理 |
| 1.1.60 | 修 GD 编号「打开界面直接点确定」会把编号清成空格 |
| 1.1.59 | 修 4 个 blockbench 渲染器每帧 new ResourceLocation |
| 1.1.58 | 修文件句柄泄漏（pasteImage 的 InputStream 从不关闭）+ 环线站台数不足时的越界 |
| 1.1.57 | 修 5 处 NativeImage 堆外内存泄漏（每张图 2.9 MB 起）+ 1 处 AWT Graphics2D 泄漏 |
| 1.1.56 | 删 _getTexImage 无效注入（移植引入）+ 去掉每帧 ResourceLocation 重建 |
| 1.1.55 | 修动态纹理的堆外内存泄漏 |
| 1.1.54 | 修 C2S 包缺少距离校验 |
| 1.1.53 | 修 GD 编号输入框的限长从未生效 |
| 1.1.52 | 优化：Collator.getInstance(Locale.CHINA) 提为静态常量（原先写在 sorted() 的 comparator 里 → 每比较一次新建一个 Collator，O(n log n) 次）。影响 RMGMain. |
| 1.1.51 | 修 StringUtil.getCjkAndEngMain：英文段缺位（站名形如 "某某站\ |
| 1.1.50 | 修 BlockShanghaiPSDEnd 缺失的放置空间检查：覆写 MTR 基类 getStateForPlacement 时丢了 IBlock.isReplaceable(ctx, UP, 2) → 2 格高的屏蔽门端头可被塞进 1 格 |
| 1.1.49 | 修两个贴墙方块的朝向（上游 getClickedFace → 移植时被换成玩家朝向 → 贴墙朝向错位）：BlockEmergencyExit + BlockPlatformMonitor，均改用 WallPlacementUtil.getW |
| 1.1.48 | 修 MTRSignMixin 线路图指示牌的宽度单位混用（getMaxWidth 返回格数被当像素 → 线路图压到 1 格宽）+ 贴图非等比压扁改等比缩放 + 垂直居中 |
| 1.1.47 | 修 GraphicsHelper.drawText 的整数除法（ratio=0 → 逆变换除零 → NaN 矩阵污染）+ 手写逆变换改 pushPose/popPose + pasteImage 补 null 检查 |
| 1.1.46 | 修放置类物品「流体判断被反转」（PSD 门/栏门四件套在正常世界无法放置，100% 触发）+ 朝向对齐上游 + SHDestination/SHRunin 的 NPE 与越界防护 |
| 1.1.45 | 修指示牌放置逻辑从未生效 |
| 1.1.44 | 修 GraphicsCache 两处真实缺陷：① DynamicResource.remove() 逐字抄自 MTR 3.2.2 官方源码，但 1.20.1 的 TextureManager.getTexture() 语义已变（找不到时新建 |
| 1.1.42 | 新增 SignCache 消除渲染热路径每帧异常构造 + 修 GUI 缩放分页越界 |
| 1.1.41 | 修 GD 编号包长度失控 + 控制字符注入；GUI 不再直接改 TE |
| 1.1.40 | 修客户端资源缓存三处并发/泄漏缺陷 + 6 个渲染器 NPE 保护 |
| 1.1.39 | 壁挂朝向逻辑收敛为 WallPlacementUtil + 6 类差集扫描收官 |
| 1.1.38 | 修 50 个方块渲染层被整体误替换（translucent→cutout） |
| 1.1.37 | 修移植期全局性 getOpposite→getCounterClockWise 替换事故（9 文件） |
| 1.1.36 | 修 5 个壁挂薄板方块贴墙朝向 + PSD 端头玻璃连接方向 |
| 1.1.35 | 清理 BlockShanghaiPSDDoor 的占位魔数 114514 |
| 1.1.34 | BlockShanghaiPSDTop.setState 合并多余方块更新 |
| 1.1.33 | 站名渲染热路径去重复解析（每帧两次 split 合并为一次） |
| 1.1.32 | 纯审计轮（注册完整性交叉核对），无代码改动，未发版 |
| 1.1.31 | 修 BlockRailingEndCorner.asItem() 无限递归 → SOE 必崩 |
| 1.1.30 | 修 BlockIronFrame 放置时南/西两侧连接判定读错方块 |
| 1.1.29 | 修指示牌包长度字段无上限导致服务端 OOM 崩溃 |
| 1.1.28 | 消除 items 包三份逐字重复的 blocksNotReplaceable（重构） |
| 1.1.27 | 修 GraphicsCache 纹理注册跨线程 + BlockbenchModel 数组越界 |
| 1.1.26 | 修 GraphicsHelper.foreground 线色对比色（上游遗留，两处） |
| 1.1.25 | 修 SHRouteMap.calculateInterval 单站台路线除零（上游遗留） |
| 1.1.24 | 修 SHRunin 上/下一站守卫与绘制数据错位（上游遗留缺陷） |
| 1.1.23 | 修 BlockEntity.SH_PSD_TOP_TILE_ENTITY 泛型声明错误 |
| 1.1.22 | 预编译 StringUtil 站名分隔正则，消除每帧 Pattern.compile |
| 1.1.21 | RenderSpecialPassing 热路径状态缓存 |
| 1.1.20 | 删重复 cutout 注册 + 删死字段 TEXTURES |
| 1.1.19 | 补全 98 个生存模式掉落表（真实缺陷） |
| 1.1.18 | 修 2020 通行闸机 TE 类型错配（真实 bug） |
| 1.1.17 | 补 shm_sign_middle + global_position lang key；关闭死代码审计 |
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