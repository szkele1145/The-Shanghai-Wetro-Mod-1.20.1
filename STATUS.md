# shwetro-1201 项目状态 (2026-10-22 15:25)

## 当前版本: 1.1.70（双端已发布 Modrinth + GitHub Release）

## 版本链 (GitHub 60 Releases)
1.0.0 移植基线 → 1.0.1~1.1.16 早期移植修复 → 1.1.17~1.1.36 审计修复批(98 处反编译产物/碰撞箱/朝向/资源泄漏) → 1.1.37~1.1.43 深度审计(渲染层/贴图/GUI) → 1.1.44~1.1.47 渲染崩溃与文本布局修复 → 1.1.48~1.1.52 协议防护与排序优化 → 1.1.53~1.1.56 输入限制/距离校验/纹理注入 → 1.1.57~1.1.63 内存泄漏扫尾(NativeImage/InputStream/ResourceLocation/GD 编号清空)

## 验证矩阵
- Fabric/Forge 双端: BUILD SUCCESSFUL, 0 mixin 错误
- 全部 100 个 .java 已与上游反编译原件逐文件核对完毕 (1.1.63 审计收官)
- jar 对称性: 双端 assets/data/sign 声明/告示牌资源全一致

## 待办
- 交接文档: C:\Users\一只屑\Desktop\shwetro-1201交接文档.md (1608 行)
- SH_PASS_2008 闸机: 无美术资源(bbmodel/贴图/blockstate 全缺), 上游同款, 不激活

## 构建
- 工程内自带 tools/jdk17 + tools/gradle-8.11-dist
- 构建命令: tools\gradle-8.11-dist\bin\gradle.bat build --console=plain (需 JAVA_HOME=tools\jdk17, GRADLE_USER_HOME=C:\gradle-home)
- 构建产物: shmetro-1.1.63-1.20.1-{fabric,forge}.jar
