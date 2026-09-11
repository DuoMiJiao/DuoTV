package com.duo.tv.player.danmaku;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;

import com.duo.tv.Setting;
import com.duo.tv.bean.Danmaku;
import com.duo.tv.bean.DanmakuData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Bridges media3 playback state with the self-contained danmaku renderer. */
public class DanPlayer implements Player.Listener {

    private static final Pattern XML = Pattern.compile("p=\"([^\"]+)\"[^>]*>([^<]+)<");
    private static final Pattern BRACKET = Pattern.compile("\\[(.*?)\\](.*)");

    private final DanmakuOverlay view;
    private final OkHttpClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Player player;
    private Call call;
    private String loaded;

    public DanPlayer(DanmakuOverlay view) {
        this.view = view;
        this.client = new OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build();
        this.view.setTimeSource(new DanmakuOverlay.TimeSource() {
            @Override
            public boolean isPlaying() {
                return player != null && player.isPlaying();
            }

            @Override
            public long positionMs() {
                return player == null ? 0 : player.getCurrentPosition();
            }
        });
        view.setShow(Setting.isDanmakuShow());
        view.setAlpha(Setting.getDanmakuAlpha() / 100f);
        applyConfig();
    }

    public void attachPlayer(Player player) {
        if (this.player != null) this.player.removeListener(this);
        this.player = player;
        if (player != null) {
            player.addListener(this);
            view.setPlaying(player.isPlaying());
        }
    }

    private void applyConfig() {
        view.setScale(Setting.getDanmakuScale());
        view.setSpeed(Setting.getDanmakuSpeedF());
        view.setArea(Setting.getDanmakuAreaF());
        view.setColorEnabled(Setting.isDanmakuColor());
        view.setStrokeEnabled(Setting.isDanmakuStroke());
        // 【修复·P1 屏蔽设置重启后失效】原来"屏蔽滚动/顶部/底部弹幕 + 屏蔽词"只由
        // 弹幕设置弹窗（DanmakuQuickDialog.applyBlock）在打开时推送一次，播放页新建
        // DanPlayer 时不会回读，导致重启 App / 重进播放后屏蔽设置静默失效（UI 仍显示已开启）。
        // 这里在初始化配置时统一回读一次，与弹窗行为保持一致。
        view.setBlockFilter(Setting.isDanmakuBlockScroll(),
                Setting.isDanmakuBlockTop(),
                Setting.isDanmakuBlockBottom(),
                new java.util.HashSet<>(Setting.getDanmakuBlockKeywords()));
    }

    public void setTextSize(float scale) {
        view.setScale(scale);
    }

    public void setColor(boolean enabled) {
        view.setColorEnabled(enabled);
    }

    public void setStroke(boolean enabled) {
        view.setStrokeEnabled(enabled);
    }

    public void setArea(float fraction) {
        view.setArea(fraction);
    }

    public void setSpeed(float multiplier) {
        view.setSpeed(multiplier);
    }

    public void setShow(boolean show) {
        view.setShow(show);
    }

    public void setAlpha(int percent) {
        view.setAlpha(Math.max(0, Math.min(100, percent)) / 100f);
    }

    public void setBlock(boolean scroll, boolean top, boolean bottom, List<String> keywords) {
        view.setBlockFilter(scroll, top, bottom, new java.util.HashSet<>(keywords));
    }

    public void setDanmaku(Danmaku item) {
        if (item == null || item.isEmpty()) {
            stop();
            return;
        }
        String url = item.getRealUrl();
        if (url.equals(loaded)) return;
        load(url);
    }

    public void stop() {
        loaded = null;
        if (call != null) call.cancel();
        view.clearItems();
    }

    public void release() {
        attachPlayer(null);
        stop();
    }

    private void load(String url) {
        loaded = url;
        if (call != null) call.cancel();
        // 【修复·P1 跨集弹幕残留】换源（切集/换线路）时立即清屏：
        // 原实现既不在此处清屏，又不处理"返回成功但列表为空"的情况，
        // 于是新集没有弹幕（或请求失败）时，屏幕上会一直滚动上一集的弹幕。
        view.clearItems();
        call = client.newCall(new Request.Builder().url(url).build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 【修复·P1】失败也要清屏 + 清除 loaded 允许重试
                android.util.Log.w("DanPlayer", "danmaku load failed: " + url, e);
                handler.post(() -> {
                    if (url.equals(loaded)) {
                        loaded = null;
                        view.clearItems();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                okhttp3.ResponseBody body = response.body();
                try (InputStream stream = body != null ? body.byteStream() : null) {
                    if (stream == null) return;
                    List<DanmakuData> items = read(stream);
                    handler.post(() -> {
                        // 【修复·P1】去掉 !items.isEmpty()：空列表同样要 setItems（等于清屏），
                        // 否则旧弹幕会继续留在屏幕上。
                        if (url.equals(loaded)) view.setItems(items);
                    });
                } catch (Exception e) {
                    android.util.Log.w("DanPlayer", "danmaku parse failed: " + url, e);
                    handler.post(() -> {
                        if (url.equals(loaded)) view.clearItems();
                    });
                }
            }
        });
    }

    public static List<DanmakuData> read(InputStream input) {
        List<DanmakuData> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            // 【修复·P1 弹幕乱码】原实现硬编码 UTF-8（new InputStreamReader(input, UTF_8)），
            // 遇到 GB2312/GBK 编码的弹幕 XML（新版声明里常见 encoding="gb2312"）会整篇乱码。
            // 现在先预读头部探测编码（BOM / XML 声明 / UTF-8 合法性），再按真实编码解码。
            byte[] head = readHead(input, 1024);
            Charset charset = detectCharset(head);
            InputStream merged = new SequenceInputStream(new ByteArrayInputStream(head), input);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(merged, charset))) {
                Pattern pattern = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("{")) continue;
                    if (pattern == null) pattern = line.startsWith("<") ? XML : BRACKET;
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        try {
                            DanmakuData data = new DanmakuData(matcher);
                            String key = data.getTime() + "|" + data.getType() + "|" + data.getText();
                            if (seen.add(key)) items.add(data);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    /** 预读头部字节，用于探测编码（不消费后续流，调用方用 SequenceInputStream 拼回）。 */
    private static byte[] readHead(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        while (out.size() < max) {
            int read = in.read(buf, 0, Math.min(buf.length, max - out.size()));
            if (read <= 0) break;
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * 编码探测顺序：BOM → XML 声明 encoding="..." → 头部严格按 UTF-8 解码是否成功（失败按 GBK）。
     * 只做保守判断，绝大多数源命中的是前两种。
     */
    private static Charset detectCharset(byte[] head) {
        if (head.length >= 3 && (head[0] & 0xFF) == 0xEF && (head[1] & 0xFF) == 0xBB && (head[2] & 0xFF) == 0xBF)
            return StandardCharsets.UTF_8;
        if (head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xFE)
            return StandardCharsets.UTF_16LE;
        if (head.length >= 2 && (head[0] & 0xFF) == 0xFE && (head[1] & 0xFF) == 0xFF)
            return StandardCharsets.UTF_16BE;
        Matcher matcher = Pattern.compile("encoding=[\"']([A-Za-z0-9_\\-]+)[\"']").matcher(new String(head, StandardCharsets.ISO_8859_1));
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1));
            } catch (Exception ignored) {
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(head));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return Charset.forName("GBK");
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        view.setPlaying(isPlaying);
    }
}
