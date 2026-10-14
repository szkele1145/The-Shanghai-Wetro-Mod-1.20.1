# 贡献指南：如何给 shwetro-1201 添加新方块

以 1.0.5 添加的 `prohibited_list`（禁止携带物品告示牌）为例，完整链路：

## 1. 方块类 `common/src/main/java/com/xhg78999/shmetro/blocks/`

- 继承 `BlockDirectionalMapper`（MTR 水平朝向基类）或原版 `BlockBehaviour`
- 构造里用 `Properties.of().mapColor().sound().noOcclusion()`（1.20.1 已无 Material）
- 覆写 `getStateForPlacement`（用 `ctx.getHorizontalDirection()`，**不要**用 `getClickedFace`——会返回 UP/DOWN 导致非法状态崩溃）
- 想要刷子右键交互：`use()` 里调 `IBlock.checkHoldingBrush(...)`

## 2. 注册（三处）

- `Blocks.java`：`RegistryObject<Block> XXX = new RegistryObject(YourBlock::new)`
- `Main.java`：`registries.registerBlockAndItem("xxx", Blocks.XXX, CreativeTab.SHFACILITIES)`
- 若有 BlockEntity：`BlockEntity.java` + `MainClient.java`（渲染器）

## 3. 资源

- 贴图：`assets/shmetro/textures/block/`（512×512 正方形；非方形需先 pad）
- 模型：`assets/shmetro/models/block/`（elements 自定义几何 + uv 映射，uv = px/16）
- 物品模型：`assets/shmetro/models/item/`（parent 指向方块模型 + display 变换）
- blockstate：`assets/shmetro/blockstates/`（四朝向 y 旋转；**不要**加 inventory 键——用物品模型）
- 战利品表：`data/shmetro/loot_tables/blocks/`
- 语言：`assets/shmetro/lang/` 的 en_us + zh_cn 各加 `block.shmetro.xxx`

## 4. 踩过的坑（必读）

| 坑 | 解法 |
|---|---|
| mixin AP 对未混淆 GL 方法报错 | 注解加 `remap = false` |
| `gradle clean` 后首次构建 mixin 失败 | 先跑 `:fabric:configureLaunch` 预热（build.ps1 已内置） |
| 方块名游戏里显示原始 key | 检查 lang 文件 `block.shmetro.*` 条目 |
| 下方方块贴图被剔除 | 构造链加 `.noOcclusion()` |

## 5. 构建验证

```powershell
.\build.ps1
```

游戏内冒烟：`gradlew :fabric:runClient`，看日志 `Wetro loaded` / 无 `Unable to load model`。