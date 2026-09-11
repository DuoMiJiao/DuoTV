# quickjs — QuickJS JavaScript 引擎模块

JS 爬虫运行时，通过 QuickJS 嵌入式引擎执行 JavaScript 爬虫脚本。

## 包结构

| 包 | 职责 |
|---|------|
| `crawler/` | Spider 接口的 JS 实现，加载和执行 JS 爬虫脚本 |
| `method/` | JS→Java 桥接方法：`_http` (HTTP 请求)、`aesX/md5X/rsaX` (加密)、`getProxy` (代理 URL)、`s2t/t2s` (繁简翻译) |
| `utils/` | Connect (OkHttp 请求封装)、Crypto (AES/RSA/MD5)、JSUtil (模块加载) |
| `bean/` | Req (JS 请求参数映射) |

## 内置 JS 库 (assets/js/lib/)

| 文件 | 用途 |
|------|------|
| `drpy2.min.js` | drpy 爬虫引擎 (3200+ 行) |
| `crypto-js.js` | CryptoJS 加密库 |
| `gbk.js` | GBK 编码支持 |
| `http.js` | HTTP 工具函数 |
| `similarity.js` | 字符串相似度计算 |
