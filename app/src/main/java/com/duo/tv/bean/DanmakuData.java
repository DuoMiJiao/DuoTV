package com.duo.tv.bean;

import android.graphics.Color;
import android.text.TextUtils;

import com.github.catvod.utils.Trans;

import java.util.regex.Matcher;

public class DanmakuData {

    private int type;
    private int color;
    private int shadow;
    private long time;
    private float size;
    private String text;

    public DanmakuData(Matcher matcher) throws Exception {
        this(matcher.group(1), matcher.group(2));
    }

    public DanmakuData(String param, String text) throws Exception {
        String[] params = param.split(",");
        if (params.length < 4) throw new Exception();
        this.type = Integer.parseInt(params[1]);
        this.time = (long) (Float.parseFloat(params[0]) * 1000);
        this.size = Float.parseFloat(params[2]);
        this.color = (int) ((0x00000000FF000000L | Long.parseLong(params[3])) & 0x00000000FFFFFFFFL);
        this.shadow = color <= Color.BLACK ? Color.WHITE : Color.BLACK;
        this.text = decode(text);
        this.trans();
    }

    /** XML 实体解码，&amp; 必须最后替换避免双重解码 */
    private static String decode(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.replace("&quot;", "\"").replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&");
    }

    public int getType() {
        return type;
    }

    public int getShadow() {
        return shadow;
    }

    public int getColor() {
        return color;
    }

    public long getTime() {
        return time;
    }

    public float getSize(float density) {
        return size * (density - 0.6f);
    }

    public String getText() {
        return text == null ? "" : text;
    }

    public void trans() {
        if (Trans.pass()) return;
        this.text = Trans.s2t(text);
    }
}
