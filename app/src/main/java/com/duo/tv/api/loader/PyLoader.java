package com.duo.tv.api.loader;

import com.duo.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PyLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private volatile String recent;

    public PyLoader() {
        spiders = new ConcurrentHashMap<>();
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        spiders.clear();
        recent = null;
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext) {
        return spiders.computeIfAbsent(key, k -> {
            try {
                Spider spider = new SpiderNull();
                spider.siteKey = key;
                spider.init(App.get(), ext);
                return spider;
            } catch (Throwable e) {
                e.printStackTrace();
                return new SpiderNull();
            }
        });
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        if (recent == null) return null;
        Spider spider = spiders.get(recent);
        return spider != null ? spider.proxy(params) : null;
    }
}
