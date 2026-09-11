package com.duo.tv.utils;

import android.text.TextUtils;

import com.duo.tv.db.AppDatabase;

/**
 * 历史 / 收藏 / 直播 keep 的 key 解析工具。
 *
 * 【新增·P1 崩溃修复】这些 key 的格式是 {@code siteKey@@@vodId[@@@cid]}
 * （分隔符常量见 {@link AppDatabase#SYMBOL}）。原实现直接写
 * {@code key.split(AppDatabase.SYMBOL)[1]}：只要 key 里没有分隔符
 * （例如导入第三方备份、或局域网 do=sync 推送了脏数据），就会抛
 * ArrayIndexOutOfBoundsException —— 表现为"打开历史/收藏页直接崩溃，而且条目删不掉"。
 *
 * 本工具统一做"越界返回空串"，并额外提供 isValid() 供入库前校验。
 * 注意：本类与 {@code utils/KeyUtil}（按键事件工具）无关，勿混用。
 */
public final class DbKeyUtil {

    private DbKeyUtil() {
    }

    /** 安全取第 index 段，越界或空 key 返回 ""。 */
    public static String part(String key, int index) {
        if (TextUtils.isEmpty(key) || index < 0) return "";
        String[] parts = key.split(AppDatabase.SYMBOL, -1);
        return index < parts.length ? parts[index] : "";
    }

    /**
     * 校验 key 是否符合 {@code siteKey@@@vodId...} 的最小格式（至少两段且首段非空）。
     * 建议用于"恢复备份 / 远程同步"入库前的脏数据拦截。
     */
    public static boolean isValid(String key) {
        if (TextUtils.isEmpty(key)) return false;
        String[] parts = key.split(AppDatabase.SYMBOL, -1);
        return parts.length >= 2 && !TextUtils.isEmpty(parts[0]);
    }
}
