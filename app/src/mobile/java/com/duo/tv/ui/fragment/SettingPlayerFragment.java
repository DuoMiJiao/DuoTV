package com.duo.tv.ui.fragment;

import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.R;
import com.duo.tv.Setting;
import com.duo.tv.databinding.FragmentSettingPlayerBinding;
import com.duo.tv.impl.BufferCallback;
import com.duo.tv.impl.SpeedCallback;
import com.duo.tv.impl.UaCallback;
import com.duo.tv.ui.base.BaseFragment;
import com.duo.tv.ui.dialog.BufferDialog;
import com.duo.tv.ui.dialog.SpeedDialog;
import com.duo.tv.ui.dialog.UaDialog;
import com.duo.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;

public class SettingPlayerFragment extends BaseFragment implements UaCallback, BufferCallback, SpeedCallback {

    private FragmentSettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] background;
    private String[] caption;
    private String[] render;
    private String[] danmakuSize;
    private String[] danmakuArea;
    private String[] danmakuSpeed;
    private String[] scale;

    public static SettingPlayerFragment newInstance() {
        return new SettingPlayerFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        format = new DecimalFormat("0.#");
        danmakuSize = ResUtil.getStringArray(R.array.select_danmaku_size);
        danmakuArea = ResUtil.getStringArray(R.array.select_danmaku_area);
        danmakuSpeed = ResUtil.getStringArray(R.array.select_danmaku_speed);
        mBinding.danmakuAreaText.setText(danmakuArea[Setting.getDanmakuArea()]);
        mBinding.danmakuSpeedText.setText(danmakuSpeed[Setting.getDanmakuSpeed()]);
        mBinding.uaText.setText(Setting.getUa());
        mBinding.aacText.setText(getSwitch(Setting.isPreferAAC()));
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        mBinding.adblockText.setText(getSwitch(Setting.isAdblock()));
        mBinding.speedText.setText(format.format(Setting.getSpeed()));
        mBinding.bufferText.setText(String.valueOf(Setting.getBuffer()));
        mBinding.audioDecodeText.setText(getSwitch(Setting.isAudioPrefer()));
        mBinding.videoDecodeText.setText(getSwitch(Setting.isVideoPrefer()));
        mBinding.danmakuLoadText.setText(getSwitch(Setting.isDanmakuLoad()));
        mBinding.danmakuShowText.setText(getSwitch(Setting.isDanmakuShow()));
        mBinding.danmakuSizeText.setText(danmakuSize[Setting.getDanmakuSize()]);

        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[Setting.isCaption() ? 1 : 0]);
        mBinding.backgroundText.setText((background = ResUtil.getStringArray(R.array.select_background))[Setting.getBackground()]);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.aac.setOnClickListener(this::setAAC);
        mBinding.scale.setOnClickListener(this::onScale);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.adblock.setOnClickListener(this::setAdblock);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.audioDecode.setOnClickListener(this::setAudioDecode);
        mBinding.videoDecode.setOnClickListener(this::setVideoDecode);
        mBinding.danmakuLoad.setOnClickListener(this::setDanmakuLoad);
        mBinding.danmakuShow.setOnClickListener(this::setDanmakuShow);
        mBinding.danmakuSize.setOnClickListener(this::setDanmakuSize);
        mBinding.danmakuArea.setOnClickListener(this::setDanmakuArea);
        mBinding.danmakuSpeed.setOnClickListener(this::setDanmakuSpeed);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }

    private void setAAC(View view) {
        Setting.putPreferAAC(!Setting.isPreferAAC());
        mBinding.aacText.setText(getSwitch(Setting.isPreferAAC()));
    }

    private void onScale(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Setting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Setting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void onSpeed(View view) {
        SpeedDialog.create(this).show();
    }

    @Override
    public void setSpeed(float speed) {
        mBinding.speedText.setText(format.format(speed));
        Setting.putSpeed(speed);
    }

    private void onBuffer(View view) {
        BufferDialog.create(this).show();
    }

    @Override
    public void setBuffer(int times) {
        mBinding.bufferText.setText(String.valueOf(times));
        Setting.putBuffer(times);
    }

    private void setRender(View view) {
        if (Setting.isTunnel() && Setting.getRender() == 0) setTunnel(view);
        int index = (Setting.getRender() + 1) % render.length;
        mBinding.renderText.setText(render[index]);
        Setting.putRender(index);
    }

    private void setTunnel(View view) {
        Setting.putTunnel(!Setting.isTunnel());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        if (Setting.isTunnel() && Setting.getRender() == 1) setRender(view);
    }

    private void setCaption(View view) {
        Setting.putCaption(!Setting.isCaption());
        mBinding.captionText.setText(caption[Setting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (Setting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return Setting.isCaption();
    }

    private void setAdblock(View view) {
        Setting.putAdblock(!Setting.isAdblock());
        mBinding.adblockText.setText(getSwitch(Setting.isAdblock()));
    }

    private void onBackground(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_background).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(background, Setting.getBackground(), (dialog, which) -> {
            mBinding.backgroundText.setText(background[which]);
            Setting.putBackground(which);
            dialog.dismiss();
        }).show();
    }

    private void setAudioDecode(View view) {
        Setting.putAudioPrefer(!Setting.isAudioPrefer());
        mBinding.audioDecodeText.setText(getSwitch(Setting.isAudioPrefer()));
    }

    private void setVideoDecode(View view) {
        Setting.putVideoPrefer(!Setting.isVideoPrefer());
        mBinding.videoDecodeText.setText(getSwitch(Setting.isVideoPrefer()));
    }

    private void setDanmakuLoad(View view) {
        Setting.putDanmakuLoad(!Setting.isDanmakuLoad());
        mBinding.danmakuLoadText.setText(getSwitch(Setting.isDanmakuLoad()));
    }

    private void setDanmakuShow(View view) {
        Setting.putDanmakuShow(!Setting.isDanmakuShow());
        mBinding.danmakuShowText.setText(getSwitch(Setting.isDanmakuShow()));
    }

    private void setDanmakuSize(View view) {
        int index = (Setting.getDanmakuSize() + 1) % danmakuSize.length;
        mBinding.danmakuSizeText.setText(danmakuSize[index]);
        Setting.putDanmakuSize(index);
    }

    private void setDanmakuArea(View view) {
        int index = (Setting.getDanmakuArea() + 1) % danmakuArea.length;
        mBinding.danmakuAreaText.setText(danmakuArea[index]);
        Setting.putDanmakuArea(index);
    }

    private void setDanmakuSpeed(View view) {
        int index = (Setting.getDanmakuSpeed() + 1) % danmakuSpeed.length;
        mBinding.danmakuSpeedText.setText(danmakuSpeed[index]);
        Setting.putDanmakuSpeed(index);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) initView();
    }
}
