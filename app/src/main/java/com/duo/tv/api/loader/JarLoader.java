package com.duo.tv.api.loader;

import android.content.Context;

import com.duo.tv.App;
import com.duo.tv.utils.Download;
import com.duo.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

import android.content.ContextWrapper;
import android.util.Log;

public class JarLoader {

    /**
     * TVBoxOSC jar 的混淆成员名（由 spider.jar 反编译确认）。
     * 更换/升级 jar 后若"配置中心"空白，优先核对这些名字是否被作者改掉。
     */
    private static final String TAG = "JarInit";
    private static final String INIT_CLASS = "com.github.catvod.spider.Init";
    private static final String INIT_CONTEXT_FIELD = "c";
    private static final String INIT_SAVE_CONFIG = "saveConfig";
    private static final String SPOOF_PACKAGE = "com.github.tvbox.osc.placeholder";

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Method> methods;
    private final ConcurrentHashMap<String, Spider> spiders;
    private final ConcurrentHashMap<String, Object> locks;
    private volatile String recent;

    public JarLoader() {
        loaders = new ConcurrentHashMap<>();
        methods = new ConcurrentHashMap<>();
        spiders = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        loaders.clear();
        methods.clear();
        spiders.clear();
        locks.clear();
        recent = null;
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private void load(String key, File file) {
        if (Thread.interrupted()) return;
        if (!Path.exists(file) || !file.setReadOnly()) return;
        String cachePath = Path.jar().getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), cachePath, cachePath, App.get().getClassLoader());
        invokeInit(loader);
        invokeProxy(key, loader);
        loaders.put(key, loader);
    }

    private void invokeInit(DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass(INIT_CLASS);
            try {
                Method method = clz.getMethod("init", Context.class);
                method.invoke(clz, new ContextWrapper(App.get()) {
                    @Override
                    public String getPackageName() {
                        // 未安装的包名让 jar 包名白名单校验抛 NameNotFoundException，绕过"加载失败"提示
                        return SPOOF_PACKAGE;
                    }
                });
            } catch (Throwable e) {
                Log.i(TAG, "whitelist bypassed: " + e.getCause());
            }
            Object init = clz.getMethod("get").invoke(null);
            Field field = clz.getDeclaredField(INIT_CONTEXT_FIELD);
            field.setAccessible(true);
            field.set(init, App.get());
            invoke(clz, INIT_SAVE_CONFIG);
            File config = new File(new File(App.get().getFilesDir(), "Pizazz"), "config.json");
            if (config.exists() && config.length() > 0) Log.i(TAG, "config.json ready");
            else Log.w(TAG, "config.json missing — jar member names may have changed");
        } catch (Throwable e) {
            Log.e(TAG, "invokeInit", e);
        }
    }

    private void invoke(Class<?> clz, String name) {
        try {
            Method method = clz.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(null);
        } catch (Throwable e) {
            Log.w(TAG, "invoke " + name + " failed", e);
        }
    }

    private void invokeProxy(String key, DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Proxy");
            Method method = clz.getMethod("proxy", Map.class);
            methods.put(key, method);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void parseJar(String key, String jar) {
        if (loaders.containsKey(key)) return;
        if (jar.startsWith("assets")) jar = UrlUtil.convert(jar);
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            if (loaders.containsKey(key)) return;
            String[] texts = jar.split(";md5;");
            String md5 = texts.length > 1 ? texts[1].trim() : "";
            if (md5.startsWith("http")) md5 = OkHttp.string(md5).trim();
            jar = texts[0];
            if (!md5.isEmpty() && Util.equals(jar, md5)) {
                load(key, Path.jar(jar));
            } else if (jar.startsWith("http")) {
                File file = Download.create(jar, Path.jar(jar)).get();
                // 【修复·P0/P1 供应链完整性】原实现只在"缓存命中"时校验 md5，**下载路径直接加载**，
                // 配合全局关闭 TLS 校验，配置里的 spider 可被中间人替换成任意 jar 执行。
                // 这里补上"下载完成后同样校验 md5"，不匹配则删除文件并放弃加载。
                if (!md5.isEmpty() && !Util.md5(file).equalsIgnoreCase(md5)) {
                    Log.e(TAG, "jar md5 mismatch, drop it: " + jar);
                    Path.clear(file);
                    return;
                }
                load(key, file);
            } else if (jar.startsWith("file")) {
                load(key, Path.local(jar));
            }
        }
    }

    public DexClassLoader dex(String jar) {
        try {
            String jaKey = Util.md5(jar);
            parseJar(jaKey, jar);
            return loaders.get(jaKey);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        String jaKey = Util.md5(jar);
        String spKey = jaKey + key;
        return spiders.computeIfAbsent(spKey, k -> {
            try {
                parseJar(jaKey, jar);
                DexClassLoader loader = loaders.get(jaKey);
                if (loader == null) return new SpiderNull();
                Spider spider = (Spider) loader.loadClass("com.github.catvod.spider." + api.split("csp_")[1]).newInstance();
                spider.siteKey = key;
                spider.init(App.get(), ext);
                return spider;
            } catch (Throwable e) {
                e.printStackTrace();
                return new SpiderNull();
            }
        });
    }

    private DexClassLoader requireRecentLoader() {
        DexClassLoader loader = loaders.get(recent);
        if (loader == null) throw new IllegalStateException("No jar loaded for recent key: " + recent);
        return loader;
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Json" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class);
        return (JSONObject) method.invoke(null, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Mix" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
        return (JSONObject) method.invoke(null, jxs, name, flag, url);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        Method method = recent != null ? methods.get(recent) : null;
        Object[] result = proxyInvoke(method, params);
        if (result != null) return result;
        return tryOthers(params);
    }

    private Object[] tryOthers(Map<String, String> p) {
        return methods.entrySet().stream().filter(e -> !e.getKey().equals(recent)).map(e -> proxyInvoke(e.getValue(), p)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Object[] proxyInvoke(Method method, Map<String, String> params) {
        try {
            return method == null ? null : (Object[]) method.invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
