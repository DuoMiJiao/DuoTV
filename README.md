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
├── app/                           # 主应用模块
│   ├── src/main/                  # 共用逻辑（所有 flavor 共享）
│   │   ├── java/.../api/          #   配置加载（VodConfig/LiveConfig）、站点 API、EPG/直播源解析
│   │   ├── java/.../db/           #   Room 数据库（观看历史、收藏、配置、轨道偏好、站点）
│   │   ├── java/.../player/       #   播放器引擎（ExoPlayer + 弹幕 DanmakuOverlay + 解析任务）
│   │   ├── java/.../server/       #   本地 HTTP 服务器（NanoHTTPD，token 鉴权，端口 9978-9998）
│   │   ├── java/.../service/      #   后台播放服务 PlaybackService + DLNA 渲染器
│   │   ├── java/.../ui/           #   共用 Dialog、Adapter、自定义 View、BaseActivity
│   │   └── java/.../utils/        #   下载、文件操作、网络请求、权限、通知等工具类
│   ├── src/leanback/              # Android TV 版 UI（遥控器焦点导航，leanback 库）
│   │   ├── java/.../ui/activity/  #   HomeActivity、VideoActivity、LiveActivity、SearchActivity 等
│   │   ├── java/.../ui/dialog/    #   配置、弹幕、字幕、轨道、历史等对话框
│   │   └── res/                   #   TV 版布局、样式、动画资源
│   ├── src/mobile/                # 手机/平板版 UI（Material Design + 底部导航）
│   │   ├── java/.../ui/           #   Activity、Fragment、Dialog、Adapter（含投屏控制）
│   │   ├── java/.../dlna/         #   DLNA 投屏（设备发现、推送、轮询状态）
│   │   └── res/                   #   手机版布局、深浅色主题、图标资源
│   └── build.gradle               # App 构建配置（flavor mobile/leanback、签名、依赖版本）
│
├── catvod/                        # CatVod 网络与爬虫框架（核心依赖库）
│   └── src/main/java/.../catvod/
│       ├── crawler/               #   Spider 基类、SpiderDebug 日志、爬虫加载器（Jar/JS/PY）
│       ├── net/                   #   OkHttp 封装（全局 client、拦截器、DNS、代理选择、认证）
│       ├── bean/                  #   网络请求 Req/响应 Res 数据模型
│       └── utils/                 #   Path 路径安全、JSON 解析、编码转换、翻译、加密工具
│
├── quickjs/                       # QuickJS JavaScript 引擎模块（JS 爬虫运行时）
│   ├── src/main/java/.../quickjs/
│   │   ├── crawler/               #   Spider 接口的 JS 实现（加载/执行 JS 爬虫脚本）
│   │   ├── method/                #   JS→Java 桥接方法（HTTP 请求、AES/RSA/MD5 加密、代理、翻译）
│   │   └── utils/                 #   连接管理、Crypto 加密、JS 模块加载器
│   └── src/main/assets/js/lib/    #   内置 JS 库（drpy 引擎、crypto-js、gbk 编码、similarity 相似度）
│
├── chaquo/                        # Chaquopy Python 运行时模块（PY 爬虫支持）
│   ├── requirements.txt           #   Python 依赖（requests、pycryptodome、lxml、beautifulsoup4 等）
│   └── src/main/java/             #   Python Spider 桥接（Chaquopy 嵌入 CPython）
│
├── thunder/                       # 迅雷下载引擎（P2SP + BT + 磁力链接加速）
│   └── src/main/
│       ├── java/.../downloadlib/  #   Java 封装（XLDownloadManager 任务管理、参数配置、错误码）
│       └── jniLibs/               #   迅雷 native SO 库（arm64-v8a + armeabi-v7a）
│
├── tvbus/                         # TVBus P2P 直播引擎（去中心化直播源）
│   └── src/main/java/.../engine/  #   TVCore 直播流加载与播放
│
├── zlive/                         # ZLive 直播引擎（另一种 P2P 直播协议）
│   └── src/main/
│       ├── java/.../zlive/        #   ZLive 直播流加载器
│       └── jniLibs/               #   native SO 库（armeabi-v7a）
│
├── jianpian/                      # 简片 P2P 下载引擎（ed2k/磁力资源下载）
│   └── src/main/
│       ├── java/.../p2p/          #   P2PClass 下载任务接口
│       └── jniLibs/               #   native SO 库（arm64-v8a + armeabi-v7a）
│
├── forcetech/                     # ForceTech 小米电视专用模块（系统级 Binder 接口）
│   └── src/main/java/.../mitv/    #   LocalBinder 本地服务、MainActivity 入口
│
├── hook/                          # 模块占位（AndroidManifest 注册，无实际代码）
│
├── docs/                          # 开发与配置文档
│   ├── CONFIG.md                  #   配置字段完整字典（JSON 格式、站点/直播/解析/壁纸字段）
│   ├── LIVE.md                    #   直播源格式说明（M3U/TXT/EPG/JSON 四种格式）
│   ├── LOCAL.md                   #   本地 HTTP API 完整端点文档（播放控制/推送/文件/同步/缓存）
│   └── SPIDER.md                  #   爬虫开发指南（Java/JS/Python 接口定义、方法签名、返回格式）
│
├── other/                         # 辅助资源
│   ├── image/                     #   App 图标、Logo、截图素材
│   └── tools/                     #   构建辅助脚本（BFG 清理、cleaner 批处理）
│
├── build.gradle                   # 项目根构建配置（Android Gradle Plugin、仓库源）
├── settings.gradle                # 模块注册表（包含所有子模块声明）
├── gradle.properties              # Gradle 属性（JVM 内存、AndroidX、版本号）
├── gradlew / gradlew.bat          # Gradle Wrapper（自动下载 Gradle 8.14.2）
└── .gitignore                     # Git 忽略规则（build/、签名文件、local.properties、APK）
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
