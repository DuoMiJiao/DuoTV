package com.duo.tv.api;

import android.util.Base64;

import com.duo.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.Response;

public class Decoder {

    private static final Pattern JS_URI = Pattern.compile("\"(\\.|\\.\\.)/(.?|.+?)\\.js\\?(.?|.+?)\"");
    private static final Pattern EXTRACT = Pattern.compile("[A-Za-z0-9]{8}\\*\\*");

    public static String getJson(String url, String tag) throws Exception {
        try (Response res = OkHttp.newCall(url, tag).execute()) {
            // 【修复·P1 状态码不判】原实现不检查 HTTP 状态码：404/500 返回的错误页会被
            // 当作配置内容解析（配合 OkHttp.string 吞异常 → 静默"0 个源"，用户完全不知道原因）。
            // 这里显式抛出带状态码的异常，交由 BaseConfig 统一转成可读提示。
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code() + (res.message() == null || res.message().isEmpty() ? "" : " " + res.message()));
            if (res.body() == null) throw new IOException("HTTP " + res.code() + " empty body");
            HttpUrl httpUrl = res.request().url();
            HttpUrl parsed = HttpUrl.parse(url);
            int size = parsed != null ? parsed.querySize() : 0;
            if (httpUrl.querySize() == size) url = httpUrl.toString();
            return verify(url, res.body().string());
        }
    }

    private static String verify(String url, String data) throws Exception {
        if (data.isEmpty()) throw new Exception();
        if (Json.isObj(data)) return fix(url, data);
        if (data.contains("**")) data = base64(data);
        if (data.startsWith("2423")) data = cbc(data.replaceAll("\\s+", ""));
        return fix(url, data);
    }

    private static String fix(String url, String data) {
        Matcher matcher = JS_URI.matcher(data);
        while (matcher.find()) data = replace(url, data, matcher.group());
        if (data.contains("../")) data = data.replace("../", UrlUtil.resolve(url, "../"));
        if (data.contains("./")) data = data.replace("./", UrlUtil.resolve(url, "./"));
        if (data.contains("__JS1__")) data = data.replace("__JS1__", "./");
        if (data.contains("__JS2__")) data = data.replace("__JS2__", "../");
        return data;
    }

    private static String replace(String url, String data, String ext) {
        String t = ext.replace("\"./", "\"" + UrlUtil.resolve(url, "./"));
        t = t.replace("\"../", "\"" + UrlUtil.resolve(url, "../"));
        t = t.replace("./", "__JS1__").replace("../", "__JS2__");
        return data.replace(ext, t);
    }

    private static String cbc(String data) throws Exception {
        String decode = new String(Util.hex2byte(data)).toLowerCase();
        String key = padEnd(decode.substring(decode.indexOf("$#") + 2, decode.indexOf("#$")));
        String iv = padEnd(decode.substring(decode.length() - 13));
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        data = data.substring(data.indexOf("2324") + 4, data.length() - 26);
        byte[] decryptData = cipher.doFinal(Util.hex2byte(data));
        return new String(decryptData, StandardCharsets.UTF_8);
    }

    private static String base64(String data) {
        String extract = extract(data);
        if (extract.isEmpty()) return data;
        return new String(Base64.decode(extract, Base64.DEFAULT));
    }

    private static String extract(String data) {
        Matcher matcher = EXTRACT.matcher(data);
        return matcher.find() ? data.substring(data.indexOf(matcher.group()) + 10) : "";
    }

    private static String padEnd(String key) {
        if (key.length() >= 16) return key.substring(0, 16);
        return key + "0000000000000000".substring(key.length());
    }
}
