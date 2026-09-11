# app — 主应用模块

Android 应用主体，包含手机版和电视版两个 flavor。

## 目录结构

| 目录 | 说明 |
|------|------|
| `src/main/` | 共用逻辑：播放器引擎、网络层、数据库、本地服务器、工具类 |
| `src/leanback/` | Android TV 版 UI（遥控器焦点导航） |
| `src/mobile/` | 手机/平板版 UI（Material Design 底部导航） |
| `libs/` | 预编译 AAR 依赖（forcetech、hook、jianpian、thunder、tvbus） |
| `schemas/` | Room 数据库迁移 schema 导出 |

## 关键包路径 (src/main/java/com/duo/tv/)

| 包 | 职责 |
|---|------|
| `api/` | 配置加载 (VodConfig/LiveConfig)、站点 API 调用、EPG 解析 |
| `db/` | Room 数据库 (AppDatabase)、DAO、数据迁移 |
| `player/` | ExoPlayer 引擎封装、弹幕 DanmakuOverlay、解析任务 ParseJob |
| `server/` | NanoHTTPD 本地服务器 (端口 9978-9998)、token 鉴权 |
| `service/` | PlaybackService 后台播放、DLNARendererService 投屏 |
| `ui/` | 共用 Adapter、Dialog、自定义 View、BaseActivity |
| `utils/` | 下载、文件操作、网络、权限、通知等工具类 |
| `model/` | ViewModel (SiteViewModel、LiveViewModel) |
| `event/` | EventBus 事件定义 (ConfigEvent、RefreshEvent 等) |
| `bean/` | 数据模型 (Vod、History、Keep、Config、Site 等) |
