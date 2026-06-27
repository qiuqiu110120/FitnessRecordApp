# Android 开发环境配置

## 项目内一键配置

如果不想先手动安装全局环境，可以在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-env.ps1 -InstallSdk -GenerateWrapper -Build
```

脚本会下载到项目内 `.tools` 目录，并生成 `local.properties`。`.tools` 和 `local.properties` 都不会提交到版本库。脚本还会把本项目的 Gradle 缓存放到 `.tools/gradle-user-home`，避免继续占用用户目录下的全局 `.gradle` 缓存。

## 推荐安装

1. 安装 Android Studio Ladybug 或更新版本。
2. 安装 JDK 17。Android Studio 通常自带 JetBrains Runtime，但命令行构建建议单独配置 JDK 17。
3. 在 Android Studio 的 SDK Manager 中安装：
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.0.0
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools
4. 用 Android Studio 打开本项目根目录。
5. 等待 Gradle Sync 完成。

## 本项目使用的关键版本

版本集中管理在 `gradle/libs.versions.toml`：

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Compose BOM 2024.12.01
- Room 2.6.1
- WorkManager 2.10.0
- DataStore 1.1.1
- Retrofit 2.11.0

## local.properties

Android Studio 通常会自动生成 `local.properties`。如果需要手动创建，内容类似：

```properties
sdk.dir=D\\:\\Project\\FitnessRecordApp\\.tools\\android-sdk
```

`local.properties` 已被 `.gitignore` 忽略，不应提交到版本库。

## 命令行构建

配置好项目内工具链后，在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-android-env.ps1 -Build
```

如果你已经全局安装并配置好 JDK，也可以直接运行 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 迁移说明

本项目已经迁移到 `D:\Project\FitnessRecordApp` 更适合长期开发。迁移脚本为：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-to-d.ps1 -DestinationRoot D:\Project -ProjectFolderName FitnessRecordApp -CopyUserGradleCache
```

## 当前机器检测结果

本次配置时，当前终端没有检测到全局：

- `java`
- `gradle`
- `adb`
- `ANDROID_HOME` / `ANDROID_SDK_ROOT`
- Android Studio 常见安装目录

因此已采用项目内工具链方案，JDK、Gradle、Android SDK 都位于项目 `.tools` 目录。
