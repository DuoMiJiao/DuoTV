package com.duo.tv.ui.dialog;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.duo.tv.R;
import com.duo.tv.Setting;
import com.duo.tv.player.PlayerManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Bilibili-style danmaku settings panel: display, opacity, size, speed, area, type blocks and keyword blocks. */
public class DanmakuQuickDialog {

    private final PlayerManager player;
    private AlertDialog dialog;
    private LinearLayout root;
    private LinearLayout keywordBox;
    private android.content.Context ctx;
    private EditText keywordInput;

    public static DanmakuQuickDialog create(PlayerManager player) {
        return new DanmakuQuickDialog(player);
    }

    private DanmakuQuickDialog(PlayerManager player) {
        this.player = player;
    }

    public void show(android.app.Activity activity) {
        ctx = activity;
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (ctx.getResources().getDisplayMetrics().density * 18);
        root.setPadding(pad, pad / 2, pad, pad);

        root.addView(switchRow("弹幕显示", Setting.isDanmakuShow(), () -> {
            boolean value = !Setting.isDanmakuShow();
            Setting.putDanmakuShow(value);
            player.setDanmakuShow(value);
        }));
        root.addView(section("基础设置"));
        // 【修复·P2】不透明度下限原为 25%（复用了"显示区域"的量程），用户无法把弹幕调到
        // 25% 以下、也无法完全透明。DanPlayer.setAlpha 本身按 0..100 钳制，这里改为 0 起。
        root.addView(sliderRow("不透明度", Setting.getDanmakuAlpha(), 0, 100, value -> {
            Setting.putDanmakuAlpha(value);
            player.setDanmakuAlpha(value);
            return value + "%";
        }));
        root.addView(cycle("字号", new String[]{"小", "标准", "大"}, Setting.getDanmakuSize(), index -> {
            Setting.putDanmakuSize(index);
            player.setDanmakuSize(Setting.getDanmakuScale());
        }));
        root.addView(cycle("速度", new String[]{"慢", "正常", "快"}, Setting.getDanmakuSpeed(), index -> {
            Setting.putDanmakuSpeed(index);
            player.setDanmakuSpeed(Setting.getDanmakuSpeedF());
        }));
        root.addView(sliderRow("显示区域", Setting.getDanmakuAreaPct(), 25, 100, value -> {
            Setting.putDanmakuAreaPct(value);
            player.setDanmakuArea(value / 100f);
            return value <= 30 ? "1/4屏" : value <= 70 ? "半屏" : value >= 95 ? "全屏" : value + "%";
        }));
        root.addView(switchRow("彩色弹幕", Setting.isDanmakuColor(), () -> {
            boolean value = !Setting.isDanmakuColor();
            Setting.putDanmakuColor(value);
            player.setDanmakuColor(value);
        }));
        root.addView(switchRow("描边", Setting.isDanmakuStroke(), () -> {
            boolean value = !Setting.isDanmakuStroke();
            Setting.putDanmakuStroke(value);
            player.setDanmakuStroke(value);
        }));
        root.addView(section("屏蔽设置"));
        root.addView(switchRow("滚动弹幕", !Setting.isDanmakuBlockScroll(), () -> {
            Setting.putDanmakuBlockScroll(!Setting.isDanmakuBlockScroll());
            applyBlock();
        }));
        root.addView(switchRow("顶部弹幕", !Setting.isDanmakuBlockTop(), () -> {
            Setting.putDanmakuBlockTop(!Setting.isDanmakuBlockTop());
            applyBlock();
        }));
        root.addView(switchRow("底部弹幕", !Setting.isDanmakuBlockBottom(), () -> {
            Setting.putDanmakuBlockBottom(!Setting.isDanmakuBlockBottom());
            applyBlock();
        }));
        root.addView(keywordEditor());
        root.addView(section("屏蔽关键词（点击移除）"));
        keywordBox = new LinearLayout(ctx);
        keywordBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(keywordBox);
        rebuildKeywords();

        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.addView(root);
        dialog = new MaterialAlertDialogBuilder(ctx).setTitle(R.string.danmaku_setting).setView(scroll).setNegativeButton(R.string.dialog_negative, null).create();
        dialog.show();
    }

    private void applyBlock() {
        player.setDanmakuBlock(Setting.isDanmakuBlockScroll(), Setting.isDanmakuBlockTop(), Setting.isDanmakuBlockBottom(), Setting.getDanmakuBlockKeywords());
    }

    private void rebuildKeywords() {
        applyBlock();
        keywordBox.removeAllViews();
        int pad = (int) (ctx.getResources().getDisplayMetrics().density * 8);
        for (String keyword : Setting.getDanmakuBlockKeywords()) {
            TextView view = new TextView(ctx);
            view.setText("✕ " + keyword);
            view.setTextSize(14);
            view.setTextColor(ctx.getColor(R.color.text));
            view.setPadding(pad, pad / 2, pad, pad / 2);
            view.setBackgroundResource(R.drawable.shape_item);
            view.setOnClickListener(v -> {
                List<String> list = Setting.getDanmakuBlockKeywords();
                list.remove(keyword);
                Setting.putDanmakuBlockKeywords(list);
                rebuildKeywords();
            });
            keywordBox.addView(view, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View keywordEditor() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (ctx.getResources().getDisplayMetrics().density * 10), 0, 0);
        keywordInput = new EditText(ctx);
        keywordInput.setHint("输入关键词");
        keywordInput.setTextSize(14);
        keywordInput.setSingleLine(true);
        row.addView(keywordInput, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView add = new TextView(ctx);
        add.setText(" 添加 ");
        add.setTextSize(14);
        add.setTextColor(ctx.getColor(R.color.primary));
        add.setGravity(Gravity.CENTER);
        add.setPadding((int) (ctx.getResources().getDisplayMetrics().density * 10), 6, (int) (ctx.getResources().getDisplayMetrics().density * 10), 6);
        add.setBackgroundResource(R.drawable.shape_item);
        add.setOnClickListener(v -> {
            String word = keywordInput.getText().toString().trim();
            if (word.isEmpty()) return;
            List<String> list = Setting.getDanmakuBlockKeywords();
            if (!list.contains(word)) list.add(word);
            Setting.putDanmakuBlockKeywords(list);
            keywordInput.setText("");
            rebuildKeywords();
        });
        row.addView(add, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private View section(String label) {
        TextView view = new TextView(ctx);
        view.setText(label);
        view.setTextSize(13);
        view.setTextColor(Color.GRAY);
        view.setPadding(0, (int) (ctx.getResources().getDisplayMetrics().density * 14), 0, 0);
        return view;
    }

    private View switchRow(String label, boolean value, Runnable toggle) {
        return cycle(label, new String[]{"开", "关"}, value ? 0 : 1, v -> toggle.run());
    }

    private View sliderRow(String label, int value, int min, int max, java.util.function.Function<Integer, String> onChange) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, (int) (ctx.getResources().getDisplayMetrics().density * 12), 0, 0);
        TextView name = new TextView(ctx);
        name.setText(label + "：" + onChange.apply(value));
        name.setTextSize(15);
        name.setTextColor(ctx.getColor(R.color.text));
        row.addView(name);
        SeekBar seek = new SeekBar(ctx);
        seek.setMax((max - min) / 5);
        seek.setProgress((value - min) / 5);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                name.setText(label + "：" + onChange.apply((progress * 5) + min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        row.addView(seek, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private View cycle(String label, String[] values, int current, java.util.function.Consumer<Integer> onNext) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (ctx.getResources().getDisplayMetrics().density * 12), 0, 0);
        TextView name = new TextView(ctx);
        name.setText(label);
        name.setTextSize(15);
        name.setTextColor(ctx.getColor(R.color.text));
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView value = new TextView(ctx);
        value.setText(values[current]);
        value.setTextSize(14);
        value.setTextColor(ctx.getColor(R.color.primary));
        value.setGravity(Gravity.END);
        value.setClickable(true);
        value.setFocusable(true);
        value.setBackgroundResource(R.drawable.shape_item);
        value.setPadding((int) (ctx.getResources().getDisplayMetrics().density * 12), 6, (int) (ctx.getResources().getDisplayMetrics().density * 12), 6);
        value.setOnClickListener(v -> {
            int index = 0;
            for (int i = 0; i < values.length; i++) if (values[i].contentEquals(value.getText())) index = (i + 1) % values.length;
            value.setText(values[index]);
            onNext.accept(index);
        });
        row.addView(value, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }
}
