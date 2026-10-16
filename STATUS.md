# shwetro-1201 项目状态 (2026-10-16 20:00)

## 当前版本: 1.1.16 (双端已发布 Modrinth + GitHub Release)

## 版本链 (GitHub 16 Releases)
1.0.0 移植基线 → 1.0.1 语言补全 → 1.0.2 放置崩溃修复 → 1.0.3 贴图剔除+方向语义 →
1.0.4 活塞API+命名规范 → 1.0.5 新方块告示牌+日志化 → 1.0.5.1 刷子旋转 →
1.0.6 shm_route 接入 MTR 编辑器 → 1.0.7 GL mixin 复活 → 1.0.8 println 统一 →
1.0.9 告示牌 UV 全幅 → 1.1.0 镜像路线图 → 1.1.1 元数据链接 → 1.1.2 displayURL+参数美化 →
1.1.3 反编译产物清理 → 1.1.4~1.1.6 性能优化 → 1.1.7 fence 斜面镜像 →
1.1.8~1.1.10 版本递增 → 1.1.11 StationNameOnWall FACING 守卫 →
1.1.12~1.1.15 版本递增 → 1.1.16 方块 rotate/mirror 批量修复

## 验证矩阵
- Fabric/Forge 双端: BUILD SUCCESSFUL + 游戏内 Wetro loaded + 0 mixin 错误
- jar 对称性: 双端 assets/data/sign 声明/告示牌资源全一致
- 42 个方块 rotate/mirror 审计: 21 个已加覆盖（含 BlockShanghaiSign rotate 但缺 mirror —— 已确认为 MTR BlockDirectionalMapper 继承默认行为，非缺陷），21 个继承链自带或继承默认

## 待办
- 交接文档: C:\Users\一只屑\Desktop\shwetro-1201交接文档.md
- README 更新历史表已补全 1.1.1~1.1.16

## 构建
- 工程内自包含: tools/jdk17 + tools/gradle-8.11-dist + build.ps1
- ./gradlew build 或 powershell build.ps1
- 每次 Gradle 调用前必须设置 $env:JAVA_HOME = 'F:\vibe coding工程\shwetro-1201\tools\jdk17'