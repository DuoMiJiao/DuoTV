# Duo影视 (DuoTV)

基于开源项目 [FongMi/TV](https://github.com/FongMi/TV) 精简重构的安卓影视聚合应用。

## 特性

- 多源聚合：支持 TVBox / Spider / JS / PY 多种爬虫源
- 弹幕支持：内嵌弹幕引擎，支持滚动/顶部/底部弹幕
- 本地配置：自动发现 `/sdcard/TVBoxOSC` 和 `/sdcard/TVBox` 下的配置包
- DLNA 投屏：支持局域网投屏
- 双端支持：手机版 (mobile) + 电视版 (leanback)
- 安全加固：本地 HTTP 服务 token 鉴权，防中间人攻击

## 构建

```bash
# 环境要求
# - JDK 17+
# - Android SDK (API 28+)
# - Gradle 8+

# 手机版
./gradlew assembleMobileArm64_v8aRelease

# 电视版
./gradlew assembleLeanbackArm64_v8aRelease
```

## 配置

将配置包放到手机存储：
- `/sdcard/TVBoxOSC/tvbox/api.json` (推荐)
- `/sdcard/TVBox/api.json`

配置格式参考 [TVBox](https://github.com/CatVod/CatVodOpen)。

## 安全说明

- 本地 HTTP 服务（端口 9978-9998）仅绑定本机，外部访问需 token
- 首次启动生成随机 token，QR 码/投屏自动携带
- localhost 回环请求免 token（本机播放/弹幕/配置加载）

## 版本

- v1.4.2：安全加固 + 本地包识别修复 + Android 11+ 存储权限适配
- v1.4.0：弹幕引擎升级 + 外部播放优化 + 本地配置自动发现

## 致谢

- [FongMi/TV](https://github.com/FongMi/TV) - 原始项目
- [CatVod](https://github.com/CatVod/CatVodOpen) - 爬虫框架

## 许可证

本项目仅供学习交流，请勿用于商业用途。
