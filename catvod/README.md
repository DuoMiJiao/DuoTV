# catvod — CatVod 网络与爬虫框架

核心依赖库，提供网络请求、爬虫加载、路径安全、编码转换等基础能力。

## 包结构

| 包 | 职责 |
|---|------|
| `crawler/` | Spider 基类 (所有爬虫的父类)、SpiderDebug 日志、JarLoader/JsLoader/PyLoader 爬虫加载器 |
| `net/` | OkHttp 全局客户端封装、AuthInterceptor 认证拦截器、RequestInterceptor/ResponseInterceptor、OkDns、OkProxySelector |
| `bean/` | Req (请求参数)、Res (响应数据) 数据模型 |
| `utils/` | Path (路径安全校验 safe/safeName)、Json 解析、Trans 繁简翻译、UriUtil、Prefers SharedPreferences |

## 关键类

- **OkHttp** — 全局 OkHttp 单例，`client()` 返回共享客户端，`client(timeout)` 派生超时变体
- **Path** — `safe(relative)` 限制路径在存储根目录内，`safeName(name)` 防路径穿越
- **Spider** — 爬虫基类，定义 homeContent/detailContent/playerContent 等接口
- **Proxy** — 本地代理端口管理
