# FitnessRecordApp

一款本地优先的健身记录 Android App。目标是让训练数据始终优先保存在用户本地，同时支持后续自建服务器备份、多设备恢复，并结合大模型给出结构化训练建议。

## 当前 MVP

- Kotlin + Jetpack Compose + Material 3
- MVVM 分层
- Room 本地数据库保存训练日期、动作、组数、次数、重量
- 首页支持月/周日历、训练日期标记、出勤趋势图
- 点击日期进入训练编辑，可添加动作、组数、次数、重量
- 首页设置支持添加、保存、删除自定义动作
- AI 建议页使用 mock 大模型返回结构化建议
- AI 设置支持配置厂商、Base URL、模型名、API Key
- DataStore 保存本地配置和自定义主题色
- 支持深色模式和全局主题色切换

## 本地优先策略

训练记录先写入 Room，本地展示优先读取 Room。数据模型预留 `syncStatus`、`updatedAt`、`deletedAt`、`remoteId` 等字段，后续接入服务器同步时以更新时间较新的记录作为冲突处理依据，避免服务器随意覆盖本地数据。

## 开发环境

详细安装步骤见 `docs/DEVELOPMENT_ENV.md`。

建议环境：

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- Android SDK Platform-Tools

## 构建

项目内脚本会优先使用 D 盘项目目录下的工具链缓存：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-env.ps1 -Build
```

如需首次配置 SDK 和 Gradle Wrapper：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-env.ps1 -InstallSdk -GenerateWrapper -Build
```

生成的 debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```
