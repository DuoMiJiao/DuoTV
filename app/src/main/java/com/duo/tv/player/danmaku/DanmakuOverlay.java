package com.duo.tv.player.danmaku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.duo.tv.bean.DanmakuData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Self-contained danmaku renderer (no external library).
 *
 * 设计原则:
 * - spawnPtr 只进不退: 配置变更(area/scale/speed)只清视觉状态,不重置spawnPtr
 * - 只有 setItems/clearItems/seek 才做完整重置
 * - 车道分配基于 actives 实时计算,不依赖外部laneFreeAt数组
 */
public class DanmakuOverlay extends View {

    public interface TimeSource {
        boolean isPlaying();
        long positionMs();
    }

    private static class Active {
        final DanmakuData data;
        final int index;
        final long spawn;
        final long duration;
        final int lane;
        final float textWidth;

        Active(DanmakuData data, int index, long spawn, long duration, int lane, float textWidth) {
            this.data = data;
            this.index = index;
            this.spawn = spawn;
            this.duration = duration;
            this.lane = lane;
            this.textWidth = textWidth;
        }
    }

    private static final long FIXED_DURATION = 5000;
    private static final long SCROLL_DURATION = 6000;
    private static final float ROW_FACTOR = 1.4f;
    private static final float STROKE = 2.5f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density = getResources().getDisplayMetrics().density;
    private final List<DanmakuData> items = new ArrayList<>();
    private final List<Active> actives = new ArrayList<>();
    private float[] sizes = new float[0];
    private TimeSource timeSource;
    private boolean playing;
    private boolean show = true;
    private boolean blockScroll;
    private boolean blockTop;
    private boolean blockBottom;
    private boolean colorEnabled = true;
    private boolean strokeEnabled = true;
    private final Set<String> blockedKeywords = new HashSet<>();
    private float scale = 1f;
    private float speed = 1f;
    private float area = 0.5f;
    private long lastPosition = -1;
    private int spawnPtr;

    public DanmakuOverlay(Context context) { super(context); }
    public DanmakuOverlay(Context context, AttributeSet attrs) { super(context, attrs); }
    public DanmakuOverlay(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    // ── 公开 API ──────────────────────────────────────────────

    public void setTimeSource(TimeSource source) { timeSource = source; }

    public void setPlaying(boolean value) {
        playing = value;
        if (playing && show && isShown()) postInvalidateOnAnimation();
    }

    public void setShow(boolean value) {
        show = value;
        setVisibility(value ? VISIBLE : INVISIBLE);
        if (value) requestLayout();
    }

    public void setBlockFilter(boolean scroll, boolean top, boolean bottom, Set<String> keywords) {
        blockScroll = scroll;
        blockTop = top;
        blockBottom = bottom;
        blockedKeywords.clear();
        for (String word : keywords) if (!word.isEmpty()) blockedKeywords.add(word.toLowerCase());
        actives.removeIf(a -> isBlocked(a.data));
        postInvalidateOnAnimation();
    }

    private boolean isBlocked(DanmakuData data) {
        int type = data.getType();
        if ((type == 1 || type == 3 || type == 6) && blockScroll) return true;
        if (type == 5 && blockTop) return true;
        if (type == 4 && blockBottom) return true;
        if (!blockedKeywords.isEmpty()) {
            String text = data.getText().toLowerCase();
            for (String keyword : blockedKeywords) if (text.contains(keyword)) return true;
        }
        return false;
    }

    public void setColorEnabled(boolean value) { colorEnabled = value; postInvalidateOnAnimation(); }
    public void setStrokeEnabled(boolean value) { strokeEnabled = value; postInvalidateOnAnimation(); }

    /** 换源: 完整重置 */
    public void setItems(List<DanmakuData> list) {
        items.clear();
        items.addAll(list);
        items.sort(Comparator.comparingLong(DanmakuData::getTime));
        recomputeSizes();
        fullReset();
        postInvalidateOnAnimation();
    }

    /** 清源: 完整重置 */
    public void clearItems() {
        items.clear();
        fullReset();
    }

    /** 字号变更: 清视觉+重算尺寸, spawnPtr 不退 */
    public void setScale(float value) {
        scale = value;
        recomputeSizes();
        clearVisual();
        postInvalidateOnAnimation();
    }

    /** 速度变更: 清视觉(车道时序变了), spawnPtr 不退 */
    public void setSpeed(float multiplier) {
        speed = Math.max(0.25f, multiplier);
        clearVisual();
        postInvalidateOnAnimation();
    }

    /** 区域变更: 清视觉+重算车道, spawnPtr 不退 */
    public void setArea(float fraction) {
        area = fraction;
        clearVisual();
        postInvalidateOnAnimation();
    }

    // ── 内部: 重置策略 ────────────────────────────────────────

    /** 完整重置: 换源/seek时用 */
    private void fullReset() {
        actives.clear();
        spawnPtr = 0;
        lastPosition = -1;
    }

    /** 清视觉状态: 配置变更时用, spawnPtr 不动 */
    private void clearVisual() {
        actives.clear();
    }

    private void recomputeSizes() {
        sizes = new float[items.size()];
        for (int i = 0; i < items.size(); i++) sizes[i] = items.get(i).getSize(density) * scale;
    }

    // ── 内部: 布局计算 ────────────────────────────────────────

    private int laneCount() {
        if (getHeight() == 0) return 1;
        int lanes = (int) (getHeight() * area / (textSizeBase() * ROW_FACTOR));
        // 【修复·P2 旧问题6】原实现把车道数硬限制为 20（Math.min(lanes, 20)），
        // 导致大屏/电视把"显示区域"设为全屏时用不满整屏、行距被 height*area/lanes 拉得很稀，
        // 高浓度弹幕被大量静默丢弃。改为按"物理上能容纳的行数"作护栏（仍防止极端参数）。
        int cap = Math.max(1, (int) (getHeight() / Math.max(1f, textSizeBase())));
        return Math.max(1, Math.min(lanes, cap));
    }

    private float textSizeBase() { return 25f * (density - 0.6f) * scale; }
    private float rowHeight() { return Math.max(textSizeBase() * ROW_FACTOR, getHeight() * area / Math.max(1, laneCount())); }
    private float scrollSpeed() { int w = getWidth(); return w == 0 ? 0 : w * speed / SCROLL_DURATION; }
    private boolean isFixed(int type) { return type == 4 || type == 5; }

    // ── 内部: 车道分配(基于 actives 实时计算) ─────────────────

    /**
     * 车道分配。
     *
     * 【修复·P2 旧问题4】原实现用两套互不检测的占用表：
     * - 滚动弹幕：{@code long[] freeAt}，统计时**显式跳过** type 4/5（顶/底弹幕）；
     * - 顶/底弹幕：{@code boolean[] occupied}，且只统计**同类型**。
     * 于是同一行可以同时被"滚动弹幕"与"置顶弹幕"占用 → 高浓度时置顶文字被滚动弹幕从中间穿过。
     *
     * 现在统一为"一行一张表"：滚动弹幕计算该行下一条可入场时间 freeAt[]，
     * 顶/底弹幕计算该行被占用的截止时间 fixedUntil[]；
     * 分配滚动弹幕时要求该行既无在屏滚动弹幕、也无未过期顶/底弹幕；
     * 分配顶/底弹幕时要求该行没有同类型在展示、且没有仍在屏内的滚动弹幕。
     */
    private int findFreeLane(long now, float textWidth, int type) {
        int lanes = laneCount();
        long[] freeAt = new long[lanes];
        long[] fixedUntil = new long[lanes];
        for (Active a : actives) {
            if (a.lane >= lanes) continue;
            if (isFixed(a.data.getType())) {
                long until = a.spawn + FIXED_DURATION;
                if (until > fixedUntil[a.lane]) fixedUntil[a.lane] = until;
            } else {
                long free = a.spawn + (long) ((getWidth() + a.textWidth) / scrollSpeed());
                if (free > freeAt[a.lane]) freeAt[a.lane] = free;
            }
        }
        if (isFixed(type)) {
            for (int i = 0; i < lanes; i++) {
                boolean sameTypeBusy = false;
                for (Active a : actives) {
                    if (a.lane != i || a.data.getType() != type) continue;
                    if (now - a.spawn < FIXED_DURATION) {
                        sameTypeBusy = true;
                        break;
                    }
                }
                if (sameTypeBusy) continue;
                if (now < freeAt[i]) continue;      // 该行还压着在屏的滚动弹幕 → 不占用
                return i;
            }
        } else {
            for (int i = 0; i < lanes; i++) {
                if (now < freeAt[i]) continue;      // 上一条滚动弹幕必须完全离场（保留原有的保守策略）
                if (now < fixedUntil[i]) continue;  // 该行有未过期的顶/底弹幕 → 不再放滚动弹幕
                return i;
            }
        }
        return -1;
    }

    // ── 绘制循环 ──────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!show || items.isEmpty() || getWidth() == 0 || getHeight() == 0) return;
        long now = timeSource != null ? timeSource.positionMs() : 0;

        if (lastPosition >= 0 && now < lastPosition) fullReset();
        advanceSpawn(now);
        lastPosition = now;
        expireActives(now);

        float rowH = rowHeight();
        for (Active active : actives) {
            float size = sizes[active.index];
            paint.setTextSize(size);
            String text = active.data.getText();
            float x, y;
            if (isFixed(active.data.getType())) {
                x = (getWidth() - active.textWidth) / 2f;
                y = active.data.getType() == 5 ? active.lane * rowH + size : getHeight() - active.lane * rowH - size * 0.2f;
            } else {
                float progress = (float) (now - active.spawn) / active.duration;
                x = getWidth() - progress * (getWidth() + active.textWidth);
                y = active.lane * rowH + size;
            }
            if (y - size > getHeight() || y < 0) continue;
            if (strokeEnabled) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(STROKE * density);
                paint.setColor(active.data.getShadow());
                canvas.drawText(text, x, y, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colorEnabled ? active.data.getColor() : android.graphics.Color.WHITE);
            canvas.drawText(text, x, y, paint);
        }
        if (playing && show && isShown()) postInvalidateOnAnimation();
    }

    private void advanceSpawn(long now) {
        while (spawnPtr < items.size()) {
            DanmakuData data = items.get(spawnPtr);
            if (data.getTime() > now) break;
            int index = spawnPtr++;
            if (isBlocked(data)) continue;
            float size = sizes[index];
            paint.setTextSize(size);
            float textWidth = paint.measureText(data.getText());
            long duration = isFixed(data.getType()) ? FIXED_DURATION : (long) ((getWidth() + textWidth) / scrollSpeed());
            if (now - data.getTime() > duration) continue;
            int lane = findFreeLane(now, textWidth, data.getType());
            if (lane < 0) continue;
            actives.add(new Active(data, index, now, duration, lane, textWidth));
        }
    }

    private void expireActives(long now) {
        Iterator<Active> it = actives.iterator();
        while (it.hasNext()) {
            Active active = it.next();
            if (now - active.spawn > active.duration) it.remove();
        }
    }
}
