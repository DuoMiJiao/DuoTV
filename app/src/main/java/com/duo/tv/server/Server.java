package com.duo.tv.server;

import com.duo.tv.service.PlaybackService;
import com.duo.tv.utils.Task;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;

public class Server {

    private volatile PlaybackService service;
    private volatile Nano nano;
    private volatile String token;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public PlaybackService getService() {
        return service;
    }

    public String getToken() {
        return token;
    }

    public void setService(PlaybackService service) {
        this.service = service;
    }

    public String getAddress() {
        return appendToken(getAddress(false));
    }

    public String getAddress(int tab) {
        return appendToken(getAddress(false) + "?tab=" + tab);
    }

    public String getAddress(String path) {
        return appendToken(getAddress(true) + path);
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Util.getIp()) + ":" + Proxy.getPort();
    }

    private String appendToken(String url) {
        if (token == null || token.isEmpty()) return url;
        return url + (url.contains("?") ? "&" : "?") + "token=" + token;
    }

    public synchronized void start() {
        if (nano != null) return;
        token = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        for (int i = 9978; i < 9999; i++) {
            try {
                nano = new Nano(i);
                nano.start(500);
                Proxy.set(i);
                break;
            } catch (Throwable e) {
                nano = null;
            }
        }
    }

    public void stop() {
        Task.execute(() -> {
            if (nano != null) nano.stop();
            service = null;
            nano = null;
            token = null;
        });
    }
}
