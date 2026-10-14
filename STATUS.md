# shwetro-1201 项目状态 (2026-10-15 05:15)

## 当前版本: 1.1.2 (双端已验证, 生产 refmap intermediary 映射确认)

## 版本链 (GitHub 10 Releases)
1.0.0 移植基线 → 1.0.1 语言补全 → 1.0.2 放置崩溃修复 → 1.0.3 贴图剔除+方向语义 →
1.0.4 活塞API+命名规范 → 1.0.5 新方块告示牌+日志化 → 1.0.5.1 刷子旋转 →
1.0.6 shm_route 接入 MTR 编辑器 → 1.0.7 GL mixin 复活 → 1.0.8 println 统一 →
1.0.9 告示牌 UV 全幅 → 1.1.0 镜像路线图 → 1.1.1 元数据链接 → 1.1.2 displayURL+参数美化

## 验证矩阵
- Fabric/Forge 双端: BUILD SUCCESSFUL + 游戏内 Wetro loaded + 2 custom signs + 0 mixin 错误
- remap 审计: 102 个已知误报, 无新增
- jar 对称性: 双端 assets/data/sign 声明/告示牌资源全一致

## 待办 (需用户操作)
- Modrinth token 已失效 (稳定 401), 项目在审核队列 → 需要新 token 补发 1.0.2~1.1.0
- 用户复测反馈: 信仰灯贴图 / 指示牌连锁 (1.0.3+ 已修, 待确认)

## 构建
- 工程内自包含: tools/jdk17 + tools/gradle-8.11-dist + build.ps1
- ./gradlew build 或 powershell build.ps1