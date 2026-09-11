package com.duo.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.History;
import com.duo.tv.databinding.DialogReceiveBinding;
import com.duo.tv.event.CastEvent;
import com.duo.tv.impl.Callback;
import com.duo.tv.ui.activity.VideoActivity;
import com.duo.tv.utils.ImgUtil;
import com.duo.tv.utils.Notify;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ReceiveDialog extends BaseDialog {

    private DialogReceiveBinding binding;
    private CastEvent event;

    public static ReceiveDialog create() {
        return new ReceiveDialog();
    }

    public ReceiveDialog event(CastEvent event) {
        this.event = event;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogReceiveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        History item = event.history();
        // 【修复·P1 零点击崩溃】投屏数据可能缺失（例如局域网请求没带 history/device，
        // 或旧版本客户端字段不全）。原实现直接 item.getVodName()/event.device().getName()
        // → 主线程 NPE 崩溃。这里判空后直接关闭对话框，保证不崩。
        if (item == null || event.device() == null) {
            dismissAllowingStateLoss();
            return;
        }
        binding.name.setText(item.getVodName());
        binding.from.setText(event.device().getName());
        ImgUtil.load(item.getVodName(), item.getVodPic(), binding.image);
    }

    @Override
    protected void initEvent() {
        binding.frame.setOnClickListener(v -> onReceiveCast());
    }

    private void showProgress() {
        binding.frame.setEnabled(false);
        binding.play.setVisibility(View.GONE);
        binding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        binding.frame.setEnabled(true);
        binding.play.setVisibility(View.VISIBLE);
        binding.progress.getRoot().setVisibility(View.GONE);
    }

    private void onReceiveCast() {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(requireActivity(), event.history().save(VodConfig.getCid()));
            dismiss();
        } else {
            showProgress();
            VodConfig.load(event.config(), getCallback());
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                onReceiveCast();
                hideProgress();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                hideProgress();
            }
        };
    }
}
