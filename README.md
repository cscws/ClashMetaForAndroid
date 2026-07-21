## Clash Meta for Android

A Graphical user interface of [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta) for Android

### 关于此 Fork / About this fork

本仓库是 [MetaCubeX/ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid) 的**独立维护分支**,携带一批性能、稳定性与耗电修复。本 fork 的改动**仅在此仓库独立维护,不会向原项目(上游)提交 PR,也不会以任何形式回馈、贡献或合并回上游**。
This is an **independently maintained fork** of CMFA carrying performance, stability and battery fixes. These changes are **maintained solely in this repository and will not be submitted upstream as pull requests, nor contributed or merged back to the original project in any form**.

**改动摘要 / What's changed** (branch `fix/leaks-anr-build-hygiene`):

- 修复广播接收器重复注册且永不注销的问题(每次前后台切换后,状态事件被成倍重复处理)
- 修复两处原生层资源泄漏:单组延迟测试泄漏 JNI GlobalRef;日志页退订协议失效导致每次开关日志都永久泄漏一条转发 goroutine
- 修复服务进程死亡瞬间可能崩溃 UI 进程的窗口(跨进程首跳未纳入重试循环)
- 安装/升级后的首次启动不再于主线程解压几十 MB geo 资源(消除白屏与 ANR 风险),并发解压原子化
- 耗电:退后台即停每秒流量轮询;通知内容未变化时不再每秒重发;拆除亮灭屏的空转唤醒链路;被禁用级别的核心日志不再跨 cgo 转发
- 构建:geo 文件增量下载(不再每次构建全量重下)、移除无用的 Jetifier、启用 Gradle 构建缓存、CMake 版本探测对浅克隆子模块健壮化

**下载 / Download:** [cmfa-2.11.31-meta-arm64-v8a-release.apk](https://github.com/cscws/ClashMetaForAndroid/raw/main/download/cmfa-2.11.31-meta-arm64-v8a-release.apk)(arm64,适配绝大多数手机 / fits most phones。其它 ABI 请按下文 Build 章节自行构建 / build other ABIs yourself)。

> [!IMPORTANT]
> 本仓库发布的 APK 为本地构建、debug 签名,与上游官方发布的签名不同:**无法覆盖安装官方版本**,需先卸载官方版再安装(反之亦然)。
> Release APKs here are built locally with a debug signature, which differs from the official upstream signature: they **cannot be installed over official builds** — uninstall first (and vice versa).

### Feature

Feature of [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.metacubex.clash.meta/)

### Requirement

- Android 5.0+ (minimum)
- Android 7.0+ (recommend)
- `armeabi-v7a` , `arm64-v8a`, `x86` or `x86_64` Architecture

### Build

1. Update submodules

   ```bash
   git submodule update --init --recursive
   ```

2. Install **OpenJDK 11**, **Android SDK**, **CMake** and **Golang**

3. Create `local.properties` in project root with

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. (Optional) Custom app package name. Add the following configuration to `local.properties`.

   ```properties
   # config your ownn applicationId, or it will be 'com.github.metacubex.clash'
   custom.application.id=com.my.compile.clash
   # remove application id suffix, or the applicaion id will be 'com.github.metacubex.clash.alpha'
   remove.suffix=true

5. Create `signing.properties` in project root with

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<key store password>
   key.alias=<key alias>
   key.password=<key password>
   ```

6. Build

   ```bash
   ./gradlew app:assembleAlphaRelease
   ```

### Automation

APP package name is `com.github.metacubex.clash.meta`

- Toggle Clash.Meta service status
  - Send intent to activity `com.github.kr328.clash.ExternalControlActivity` with action `com.github.metacubex.clash.meta.action.TOGGLE_CLASH`
- Start Clash.Meta service
  - Send intent to activity `com.github.kr328.clash.ExternalControlActivity` with action `com.github.metacubex.clash.meta.action.START_CLASH`
- Stop Clash.Meta service
  - Send intent to activity `com.github.kr328.clash.ExternalControlActivity` with action `com.github.metacubex.clash.meta.action.STOP_CLASH`
- Import a profile
  - URL Scheme `clash://install-config?url=<encoded URI>` or `clashmeta://install-config?url=<encoded URI>`

### Contribution and Project Maintenance

#### Meta Kernel

- CMFA uses the kernel from `android-real` branch under `MetaCubeX/Clash.Meta`, which is a merge of the main `Alpha` branch and `android-open`.
  - If you want to contribute to the kernel, make PRs to `Alpha` branch of the Meta kernel repository.
  - If you want to contribute Android-specific patches to the kernel, make PRs to  `android-open` branch of the Meta kernel repository.

#### Maintenance

- When `MetaCubeX/Clash.Meta` kernel is updated to a new version, the `Update Dependencies` actions in this repo will be triggered automatically.
  - It will pull the new version of the meta kernel, update all the golang dependencies, and create a PR without manual intervention.
  - If there is any compile error in PR, you need to fix it before merging. Alternatively, you may merge the PR directly.
- Manually triggering `Build Pre-Release` actions will compile and publish a `PreRelease` version.
- Manually triggering `Build Release` actions will compile, tag and publish a `Release` version.
  - You must fill the blank `Release Tag` with the tag you want to release in the format of `v1.2.3`.
  - `versionName` and `versionCode` in `build.gradle.kts` will be automatically bumped to the tag you filled above.
