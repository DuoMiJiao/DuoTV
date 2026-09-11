package com.duo.tv;


import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatDelegate;
import com.github.catvod.utils.Prefers;

public class Setting {

    public static String getDoh() {
        return Prefers.getString("doh");
    }

    public static void putDoh(String doh) {
        Prefers.put("doh", doh);
    }

    public static String getKeyword() {
        return Prefers.getString("keyword");
    }

    public static void putKeyword(String keyword) {
        Prefers.put("keyword", keyword);
    }

    public static String getHot() {
        return Prefers.getString("hot");
    }

    public static void putHot(String hot) {
        Prefers.put("hot", hot);
    }

    public static String getUa() {
        return Prefers.getString("ua");
    }

    public static void putUa(String ua) {
        Prefers.put("ua", ua);
    }

    public static int getReset() {
        return Prefers.getInt("reset", 0);
    }

    public static void putReset(int reset) {
        Prefers.put("reset", reset);
    }

    public static int getRender() {
        return Prefers.getInt("render", 0);
    }

    public static void putRender(int render) {
        Prefers.put("render", render);
    }

    public static int getSize() {
        return Prefers.getInt("size", 2);
    }

    public static void putSize(int size) {
        Prefers.put("size", size);
    }

    public static int getScale() {
        return Prefers.getInt("scale");
    }

    public static void putScale(int scale) {
        Prefers.put("scale", scale);
    }

    public static int getLiveScale() {
        return Prefers.getInt("scale_live", getScale());
    }

    public static void putLiveScale(int scale) {
        Prefers.put("scale_live", scale);
    }

    public static int getBuffer() {
        return Math.min(Math.max(Prefers.getInt("buffer"), 1), 10);
    }

    public static void putBuffer(int buffer) {
        Prefers.put("buffer", buffer);
    }

    public static int getBackground() {
        return Prefers.getInt("background", 2);
    }

    public static void putBackground(int background) {
        Prefers.put("background", background);
    }

    public static int getSiteMode() {
        return Prefers.getInt("site_mode");
    }

    public static void putSiteMode(int mode) {
        Prefers.put("site_mode", mode);
    }

    public static int getSyncMode() {
        return Prefers.getInt("sync_mode");
    }

    public static void putSyncMode(int mode) {
        Prefers.put("sync_mode", mode);
    }

    public static boolean isIncognito() {
        return Prefers.getBoolean("incognito");
    }

    public static void putIncognito(boolean incognito) {
        Prefers.put("incognito", incognito);
    }

    public static boolean isBootLive() {
        return Prefers.getBoolean("boot_live");
    }

    public static void putBootLive(boolean boot) {
        Prefers.put("boot_live", boot);
    }

    public static boolean isInvert() {
        return Prefers.getBoolean("invert");
    }

    public static void putInvert(boolean invert) {
        Prefers.put("invert", invert);
    }

    public static boolean isAcross() {
        return Prefers.getBoolean("across", true);
    }

    public static void putAcross(boolean across) {
        Prefers.put("across", across);
    }

    public static boolean isChange() {
        return Prefers.getBoolean("change", true);
    }

    public static void putChange(boolean change) {
        Prefers.put("change", change);
    }

    public static boolean getUpdate() {
        return Prefers.getBoolean("update", true);
    }

    public static void putUpdate(boolean update) {
        Prefers.put("update", update);
    }

    public static boolean isCaption() {
        return Prefers.getBoolean("caption");
    }

    public static void putCaption(boolean caption) {
        Prefers.put("caption", caption);
    }

    public static boolean isTunnel() {
        return Prefers.getBoolean("tunnel");
    }

    public static void putTunnel(boolean tunnel) {
        Prefers.put("tunnel", tunnel);
    }

    public static boolean isAudioPrefer() {
        return Prefers.getBoolean("audio_prefer");
    }

    public static void putAudioPrefer(boolean audioPrefer) {
        Prefers.put("audio_prefer", audioPrefer);
    }

    public static boolean isVideoPrefer() {
        return Prefers.getBoolean("video_prefer");
    }

    public static void putVideoPrefer(boolean videoPrefer) {
        Prefers.put("video_prefer", videoPrefer);
    }

    public static boolean isPreferAAC() {
        return Prefers.getBoolean("prefer_aac");
    }

    public static void putPreferAAC(boolean preferAAC) {
        Prefers.put("prefer_aac", preferAAC);
    }

    public static boolean isDanmakuLoad() {
        return Prefers.getBoolean("danmaku_load", true);
    }

    public static void putDanmakuLoad(boolean danmakuLoad) {
        Prefers.put("danmaku_load", danmakuLoad);
    }

    public static boolean isAdblock() {
        return Prefers.getBoolean("adblock", true);
    }

    public static void putAdblock(boolean adblock) {
        Prefers.put("adblock", adblock);
    }

    public static boolean isDanmakuShow() {
        return Prefers.getBoolean("danmaku_show", true);
    }

    public static void putDanmakuShow(boolean danmakuShow) {
        Prefers.put("danmaku_show", danmakuShow);
    }

    public static int getDanmakuSize() {
        return Prefers.getInt("danmaku_size", 1);
    }

    public static void putDanmakuSize(int size) {
        Prefers.put("danmaku_size", size);
    }

    public static int getDanmakuAreaPct() {
        int legacy = Prefers.getInt("danmaku_area_pct", Prefers.getInt("danmaku_area", 1) == 0 ? 25 : Prefers.getInt("danmaku_area", 1) == 2 ? 100 : 50);
        return legacy;
    }

    public static void putDanmakuAreaPct(int pct) {
        Prefers.put("danmaku_area_pct", Math.max(25, Math.min(100, pct)));
    }

    public static int getDanmakuArea() {
        int pct = getDanmakuAreaPct();
        return pct <= 30 ? 0 : pct <= 70 ? 1 : 2;
    }

    public static void putDanmakuArea(int area) {
        putDanmakuAreaPct(area == 0 ? 25 : area == 1 ? 50 : 100);
    }

    public static float getDanmakuAreaF() {
        return getDanmakuAreaPct() / 100f;
    }

    public static int getDanmakuSpeed() {
        return Prefers.getInt("danmaku_speed", 1);
    }

    public static void putDanmakuSpeed(int speed) {
        Prefers.put("danmaku_speed", speed);
    }

    public static float getDanmakuSpeedF() {
        switch (getDanmakuSpeed()) {
            case 0:
                return 0.6f;
            case 2:
                return 1.6f;
            default:
                return 1.0f;
        }
    }

    public static float getDanmakuScale() {
        switch (getDanmakuSize()) {
            case 0:
                return 0.75f;
            case 2:
                return 1.25f;
            default:
                return 1.0f;
        }
    }

    public static int getDanmakuAlpha() {
        return Prefers.getInt("danmaku_alpha", 100);
    }

    public static boolean isDanmakuBlockScroll() {
        return Prefers.getBoolean("danmaku_block_scroll");
    }

    public static void putDanmakuBlockScroll(boolean value) {
        Prefers.put("danmaku_block_scroll", value);
    }

    public static boolean isDanmakuBlockTop() {
        return Prefers.getBoolean("danmaku_block_top");
    }

    public static void putDanmakuBlockTop(boolean value) {
        Prefers.put("danmaku_block_top", value);
    }

    public static boolean isDanmakuBlockBottom() {
        return Prefers.getBoolean("danmaku_block_bottom");
    }

    public static void putDanmakuBlockBottom(boolean value) {
        Prefers.put("danmaku_block_bottom", value);
    }

    public static List<String> getDanmakuBlockKeywords() {
        List<String> list = new ArrayList<>();
        for (String word : Prefers.getString("danmaku_block_keywords").split(",")) if (!word.isEmpty()) list.add(word);
        return list;
    }

    public static void putDanmakuBlockKeywords(List<String> list) {
        Prefers.put("danmaku_block_keywords", String.join(",", list));
    }

    public static void putDanmakuAlpha(int alpha) {
        Prefers.put("danmaku_alpha", alpha);
    }

    public static boolean isDanmakuColor() {
        return Prefers.getBoolean("danmaku_color", true);
    }

    public static void putDanmakuColor(boolean value) {
        Prefers.put("danmaku_color", value);
    }

    public static boolean isDanmakuStroke() {
        return Prefers.getBoolean("danmaku_stroke", true);
    }

    public static void putDanmakuStroke(boolean value) {
        Prefers.put("danmaku_stroke", value);
    }

    public static boolean isZhuyin() {
        return Prefers.getBoolean("zhuyin");
    }

    public static void putZhuyin(boolean zhuyin) {
        Prefers.put("zhuyin", zhuyin);
    }

    public static float getSpeed() {
        return Math.min(Math.max(Prefers.getFloat("speed", 3), 2), 5);
    }

    public static void putSpeed(float speed) {
        Prefers.put("speed", speed);
    }

    public static float getSubtitleTextSize() {
        return Prefers.getFloat("subtitle_text_size");
    }

    public static void putSubtitleTextSize(float value) {
        Prefers.put("subtitle_text_size", value);
    }

    public static float getSubtitlePosition() {
        return Prefers.getFloat("subtitle_position");
    }

    public static void putSubtitlePosition(float value) {
        Prefers.put("subtitle_position", value);
    }

    public static boolean isBackgroundOff() {
        return getBackground() == 0;
    }

    public static boolean isBackgroundOn() {
        return getBackground() == 1 || getBackground() == 2;
    }

    public static boolean isBackgroundPiP() {
        return getBackground() == 2;
    }

    public static boolean hasCaption() {
        return new Intent(Settings.ACTION_CAPTIONING_SETTINGS).resolveActivity(App.get().getPackageManager()) != null;
    }

    public static boolean hasFileManager() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + App.get().getPackageName())).resolveActivity(App.get().getPackageManager()) != null || new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).resolveActivity(App.get().getPackageManager()) != null);
    }
    public static int getThemeMode() {
        return Prefers.getInt("theme_mode", 0);
    }

    public static void putThemeMode(int mode) {
        Prefers.put("theme_mode", mode);
        applyTheme();
    }

    public static void applyTheme() {
        switch (getThemeMode()) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

}
