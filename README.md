<p align="center">
  <img src="https://img.shields.io/badge/Android-28%2B-green?logo=android" />
  <img src="https://img.shields.io/badge/Java-17-blue?logo=openjdk" />
  <img src="https://img.shields.io/badge/media3-1.10.0-purple" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange" />
</p>

# Duo影视 (DuoTV)

基于 [FongMi/TV](https://github.com/FongMi/TV) 精简重构的安卓影视聚合应用，支持手机版与电视版双端。

**App 本身不内置任何内容来源。** 需自行配置 TVBox / Spider / JS / PY 等外部源。

## 功能特性

- **多源聚合**：支持 TVBox JSON、Spider Jar、JavaScript (drpy/quickjs)、Python 爬虫源
- **弹幕系统**：内嵌弹幕引擎，支持滚动/顶部/底部弹幕、屏蔽词、透明度/速度/区域调节
- **播放器**：基于 media3 (ExoPlayer)，支持 HLS/DASH/RTMP/RTSP 等协议，自动重试与硬软解切换
- **记忆续播**：自动记录播放进度，下次打开从上次位置继续
- **本地配置发现**：启动自动扫描 `/sdcard/TVBoxOSC` 和 `/sdcard/TVBox` 目录下的配置文件
- **DLNA 投屏**：局域网投屏到电视/盒子
- **外部播放器**：支持调用第三方播放器播放
- **双端适配**：
  - `mobile`：手机/平板版，Material Design 风格
  - `leanback`：Android TV 版，遥控器优化
- **安全加固**：本地 HTTP 服务 token 鉴权，防中间人攻击

## 截图

> 欢迎提 Issue 补充截图

## 构建

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Android SDK | compileSdk 35 |
| Gradle | 8+ (项目自带 wrapper) |
| Python | 3.10+ (可选，用于 PY 爬虫源) |

### 构建命令

```bash
# 克隆
git clone https://github.com/DuoMiJiao/DuoTV.git
cd DuoTV

# 手机版 ARM64
./gradlew assembleMobileArm64_v8aRelease

# 手机版 ARMv7
./gradlew assembleMobileArmeabi_v7aRelease

# 电视版 ARM64
./gradlew assembleLeanbackArm64_v8aRelease

# 电视版 ARMv7
./gradlew assembleLeanbackArmeabi_v7aRelease
```

APK 输出到 `app/build/outputs/apk/` 目录。

### 签名配置

在项目根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk

storeFile=/path/to/your.keystore
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```

> `local.properties` 已在 `.gitignore` 中，不会被提交。

## 配置使用

将配置文件放到手机存储，App 启动时自动发现：

| 优先级 | 路径 | 说明 |
|--------|------|------|
| 1 | `TVBoxOSC/tvbox/api.json` | TVBox 标准目录 |
| 2 | `TVBoxOSC/api.json` | TVBoxOSC 根目录 |
| 3 | `TVBox/api.json` | TVBox 目录 |
| 4 | `tvbox/api.json` | 根目录 |

配置格式参考 [TVBox](https://github.com/CatVod/CatVodOpen) 标准 JSON 格式。

也支持通过 App 内扫码/手动输入远程配置 URL。

## 项目结构

```
├── app/
│   ├── src/main/          # 共用逻辑（播放器、网络、数据库、服务器）
│   ├── src/leanback/      # TV 版 UI
│   ├── src/mobile/        # 手机版 UI
│   └── build.gradle       # App 构建配置
├── catvod/                # 网络层、爬虫框架、工具类
├── quickjs/               # QuickJS JavaScript 引擎
├── docs/                  # 文档（CONFIG.md、LIVE.md、LOCAL.md、SPIDER.md）
├── thunder/               # 迅雷下载引擎
├── tvbus/                 # TVBus P2P 引擎
├── zlive/                 # 直播引擎
└── build.gradle           # 项目根构建配置
```

## 安全说明

| 机制 | 说明 |
|------|------|
| Token 鉴权 | 本地 HTTP 服务（端口 9978-9998）首次启动生成随机 token，外部请求必须携带 |
| localhost 免检 | 本机回环请求（127.0.0.1）免 token，保证播放/弹幕/配置加载正常 |
| 路径限制 | `/file` 端点限制在存储根目录内，防止任意文件读取 |
| HTML 转义 | `/parse` 端点参数转义，防 XSS 注入 |
| 权限 | Android 11+ 需 `MANAGE_EXTERNAL_STORAGE` 权限访问本地配置 |

## 版本历史

### v1.4.2 (当前)
- 安全加固：本地 HTTP 服务 token 鉴权
- 修复 `clan://` URL 拼接导致本地配置加载失败
- 修复 EpgParser null channel NPE
- 修复 LiveParser 空属性值数组越界
- 修复 Download 取消后仍回调 success
- 修复弹幕换集竞态污染、失败后无法重试
- 修复换源误伤坏源名单
- 修复 HomeActivity.onDestroy 进程级清理
- 修复 uiMode 变化导致播放中断
- Android 11+ MANAGE_EXTERNAL_STORAGE 权限适配

### v1.4.0
- 弹幕引擎升级
- 外部播放按钮优化
- 本地配置自动发现

## 致谢

- [FongMi/TV](https://github.com/FongMi/TV) - 原始项目
- [CatVod](https://github.com/CatVod/CatVodOpen) - 爬虫框架
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) - 弹幕引擎

## 许可证

本项目基于 [GPL-3.0](LICENSE.md) 许可证开源，仅供学习交流。

**App 本身不内置或提供任何内容来源，不承担任何因使用本软件产生的法律责任。**
