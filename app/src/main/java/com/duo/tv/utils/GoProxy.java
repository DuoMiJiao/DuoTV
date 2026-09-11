package com.duo.tv.utils;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.duo.tv.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Runs the bundled Go services that the spider jars rely on: pvideo (danmaku, port 1314) and go_proxy_video (portal, port 7777). */
public class GoProxy {

    private static final String TAG = "GoProxy";
    private static final String PORTAL = "go_proxy_video";
    private static final String PVIDEO = "pvideo-arm64-v8a";

    public static void start() {
        if (!isArm64()) return;
        Task.execute(() -> {
            try {
                File bin = extract(PVIDEO);
                start(bin, "-port", "1314", "-iptv-auth-token", token());
                start(extract(PORTAL));
            } catch (Throwable e) {
                Log.e(TAG, "start", e);
            }
        });
    }

    private static void start(File file, String... args) throws Exception {
        exec("pkill -f " + file.getName());
        Thread.sleep(200);
        List<String> cmd = new ArrayList<>();
        cmd.add(file.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        // 子进程输出不消费会撑满管道缓冲(约64KB)导致进程卡死、弹幕服务静默死亡；重定向到文件（每次启动截断）
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(new File(App.get().getFilesDir(), "goproxy.log")));
        pb.start();
        Log.e(TAG, "started " + TextUtils.join(" ", cmd));
    }

    private static boolean isArm64() {
        String[] abis = Build.SUPPORTED_ABIS;
        for (String abi : abis) if (TextUtils.equals(abi, "arm64-v8a")) return true;
        return false;
    }

    private static File extract(String name) throws Exception {
        File file = new File(App.get().getFilesDir(), name);
        if (!file.exists() || file.length() == 0) {
            try (InputStream is = App.get().getAssets().open(name); OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            }
        }
        file.setExecutable(true, false);
        return file;
    }

    private static String token() throws Exception {
        File file = new File(App.get().getFilesDir(), "iptv_auth_token");
        if (file.isFile() && file.length() >= 64) {
            byte[] bytes = new byte[64];
            try (FileInputStream in = new FileInputStream(file)) {
                if (in.read(bytes) >= 64) return new String(bytes, 0, 64).trim();
            }
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        StringBuilder builder = new StringBuilder(64);
        for (byte b : random) builder.append(String.format(Locale.US, "%02x", b));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(builder.toString().getBytes());
        }
        return builder.toString();
    }

    private static void exec(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception ignored) {
        }
    }
}
