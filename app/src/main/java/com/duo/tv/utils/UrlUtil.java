package com.duo.tv.utils;

import android.net.Uri;

import com.duo.tv.server.Server;
import com.github.catvod.utils.UriUtil;
import com.google.common.net.HttpHeaders;

public class UrlUtil {

    public static Uri uri(String url) {
        return Uri.parse(url.trim().replace("\\", ""));
    }

    public static String scheme(String url) {
        return url == null ? "" : scheme(Uri.parse(url));
    }

    public static String scheme(Uri uri) {
        String scheme = uri.getScheme();
        return scheme == null ? "" : scheme.toLowerCase().trim();
    }

    public static String host(String url) {
        return url == null ? "" : host(Uri.parse(url));
    }

    public static String host(Uri uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase().trim();
    }

    public static String path(String url) {
        return url == null ? "" : path(Uri.parse(url));
    }

    public static String path(Uri uri) {
        String path = uri.getLastPathSegment();
        return path == null ? "" : path.trim();
    }

    public static String resolve(String baseUri, String referenceUri) {
        return UriUtil.resolve(baseUri, referenceUri);
    }

    public static String convert(String url) {
        String scheme = scheme(url);
        if ("clan".equals(scheme)) return clan(url);
        if ("proxy".equals(scheme)) {
            String base = Server.get().getAddress("/proxy");
            String rest = url.substring("proxy://".length());
            return base + (base.contains("?") ? "&" : "?") + rest;
        }
        String path = null;
        if ("assets".equals(scheme)) path = "/";
        else if ("local".equals(scheme)) path = "/";
        else if ("file".equals(scheme)) path = "/file/";
        return path != null ? url.replace(scheme + "://", Server.get().getAddress(true) + path) : url;
    }

    /** TVBox subscription scheme: clan://localhost/PATH maps to local storage, clan://HOST/PATH proxies over http. */
    private static String clan(String url) {
        String rest = url.substring("clan://".length());
        if (rest.startsWith("localhost/")) return Server.get().getAddress(true) + "/file/" + rest.substring("localhost/".length());
        if (rest.indexOf('/') > 0) return "http://" + rest;
        return Server.get().getAddress(true) + "/file/" + rest;
    }

    public static String getName(String url) {
        Uri uri = Uri.parse(url);
        String path = path(uri);
        String host = host(uri);
        return !path.isEmpty() ? path : !host.isEmpty() ? host : url;
    }

    public static String fixHeader(String key) {
        if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key)) return HttpHeaders.USER_AGENT;
        if (HttpHeaders.REFERER.equalsIgnoreCase(key)) return HttpHeaders.REFERER;
        if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) return HttpHeaders.COOKIE;
        return key;
    }
}
