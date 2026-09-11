package com.duo.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaTitle;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.databinding.DialogTitleBinding;
import com.duo.tv.player.PlayerManager;
import com.duo.tv.ui.adapter.TitleAdapter;
import com.duo.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class TitleDialog extends BaseDialog implements TitleAdapter.OnClickListener {

    private final TitleAdapter adapter;
    private DialogTitleBinding binding;
    private PlayerManager player;

    public static TitleDialog create() {
        return new TitleDialog();
    }

    public TitleDialog() {
        this.adapter = new TitleAdapter(this);
    }

    public TitleDialog player(PlayerManager player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogTitleBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.setAdapter(adapter.addAll(player.getCurrentMediaTitles()));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(adapter.getSelected()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(MediaTitle item) {
        player.setTitle(item);
        dismiss();
    }
}