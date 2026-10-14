# tools/ 移植方法论脚本说明

本目录保存了 shwetro 1.19.4 → 1.20.1 移植工程的全部核心脚本，是"反编译 + 双版本官方映射桥接"方法论的完整记录。

## 脚本清单（按工作流顺序）

| 脚本 | 用途 |
|---|---|
| `remap.ps1` | 初版映射转换：解析 1.19.4 Mojang client.txt + yarn intermediary tiny，合成 intermediary→named 映射 |
| `remap-final.ps1` | 终版转换：CFR 反编译输出（纯 intermediary）→ Mojang named 源码，长键优先防前缀污染 |
| `fix-residual.ps1` | 语义变量名上下文修复（owner 推断 + 全局兜底表） |
| `fix-final.ps1` | 手工验证后的成员终表（Block.box/asItem 等错配修正） |
| `fix-material.ps1` | 1.19.4 Material → 1.20.1 Properties.of().mapColor().sound() 批量转换 |
| `fix-lang.ps1` | 语言文件缺失条目补齐 |
| `audit-remap.ps1` / `audit2.ps1` | 系统性审计：原版 method_xxx 位点 vs 当前 named 调用逐项核对（发现 4 批错配的关键工具） |

## 方法论要点

1. **为什么需要双版本映射**：反编译 jar 是 intermediary 名（class_2248），目标是 1.20.1 Mojang named（Block）；中间要经过 1.19.4 intermediary → 1.19.4 named（同版本命中率高）→ 视为 1.20.1 named（Mojang API 跨这两个版本基本稳定，个别差异编译期修）
2. **跨版本陷阱**：official 混淆名会漂移（1.19.4 `d` ≠ 1.20.1 `d`），所以绝对不能拿 intermediary 的 official 名直接查 1.20.1 映射
3. **审计防错配**：宽松候选集会产生误报，人工核验每个候选（本项目因此发现 getClickedFace/getCounterClockWise/noOcclusion/RenderType 四批严重错配）