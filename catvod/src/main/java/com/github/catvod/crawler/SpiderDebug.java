package com.github.catvod.crawler;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        if (th != null) th.printStackTrace();
    }

    public static void log(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        Logger.t(TAG).d(clip(msg));
    }

    public static void log(String tag, String msg, Object... args) {
        if (TextUtils.isEmpty(msg)) return;
        Logger.t(tag).d(clip(msg), args);
    }

    /** 站点 JSON 响应可达数百 KB，全量打印造成内存抖动与 logcat 洪泛；截断保留头部用于排查 */
    private static String clip(String msg) {
        return msg.length() > 2048 ? msg.substring(0, 2048) + "...(" + msg.length() + " chars)" : msg;
    }
}
