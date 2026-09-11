package com.duo.tv.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.Danmaku;
import com.duo.tv.databinding.DialogDanmakuBinding;
import com.duo.tv.player.PlayerManager;
import com.duo.tv.ui.adapter.DanmakuAdapter;
import com.duo.tv.ui.custom.SpaceItemDecoration;
import com.duo.tv.utils.FileChooser;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class DanmakuDialog extends BaseDialog implements DanmakuAdapter.OnClickListener {

    private final DanmakuAdapter adapter;
    private DialogDanmakuBinding binding;
    private PlayerManager player;

    public static DanmakuDialog create() {
        return new DanmakuDialog();
    }

    public DanmakuDialog() {
        this.adapter = new DanmakuAdapter(this);
    }

    public DanmakuDialog player(PlayerManager player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDanmakuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(player.getDanmakus()));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(adapter.getSelected()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        binding.search.setVisibility(player.getMetadata() == null || VodConfig.get().getConfig().getDanmaku().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void initEvent() {
        binding.search.setOnClickListener(this::onSearch);
        binding.choose.setOnClickListener(this::onChoose);
    }

    private void onSearch(View view) {
        DanmakuSearchDialog.create().player(player).show(getActivity());
        dismiss();
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show(new String[]{"text/*"});
        player.pause();
    }

    @Override
    public void onItemClick(Danmaku item) {
        player.setDanmaku(item.isSelected() ? Danmaku.empty() : item);
        dismiss();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        player.setDanmaku(Danmaku.from(FileChooser.getPathFromUri(result.getData().getData())));
        dismiss();
    });
}