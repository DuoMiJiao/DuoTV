package com.duo.tv.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.duo.tv.R;
import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.Class;
import com.duo.tv.bean.Config;
import com.duo.tv.bean.Result;
import com.duo.tv.bean.Site;
import com.duo.tv.bean.Value;
import com.duo.tv.databinding.FragmentVodBinding;
import com.duo.tv.event.CastEvent;
import com.duo.tv.event.ConfigEvent;
import com.duo.tv.event.RefreshEvent;
import com.duo.tv.event.StateEvent;
import com.duo.tv.impl.Callback;
import com.duo.tv.impl.ConfigCallback;
import com.duo.tv.impl.FilterCallback;
import com.duo.tv.impl.SiteCallback;
import com.duo.tv.model.SiteViewModel;
import com.duo.tv.ui.activity.HistoryActivity;
import com.duo.tv.ui.activity.KeepActivity;
import com.duo.tv.ui.activity.SearchActivity;
import com.duo.tv.ui.activity.VideoActivity;
import com.duo.tv.ui.adapter.TypeAdapter;
import com.duo.tv.ui.base.BaseFragment;
import com.duo.tv.ui.dialog.FilterDialog;
import com.duo.tv.ui.dialog.HistoryDialog;
import com.duo.tv.ui.dialog.LinkDialog;
import com.duo.tv.ui.dialog.ReceiveDialog;
import com.duo.tv.ui.dialog.SiteDialog;
import com.duo.tv.utils.FileChooser;
import com.duo.tv.utils.ImgUtil;
import com.duo.tv.utils.Notify;
import com.duo.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VodFragment extends BaseFragment implements ConfigCallback, SiteCallback, FilterCallback, TypeAdapter.OnClickListener {

    private FragmentVodBinding mBinding;
    private SiteViewModel mViewModel;
    private TypeAdapter mAdapter;
    private Result mResult;
    /**
     * 【新增·P1 空态文案】当前空列表的原因对应的字符串资源：
     * 默认为 config_empty（配置有效但没有内容）；加载失败时切换为 error_config_get，
     * 避免"断网/404"也提示"请检查配置包是否有效"。
     */
    private int emptyRes = R.string.config_empty;

    public static VodFragment newInstance() {
        return new VodFragment();
    }

    private FolderFragment getFragment() {
        return (FolderFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        mBinding.title.setSelected(true);
        setRecyclerView();
        setViewModel();
        showProgress();
        setTitle();
        homeContent();
    }

    @Override
    protected void initEvent() {
        mBinding.top.setOnClickListener(this::onTop);
        mBinding.title.setOnClickListener(this::onSite);
        mBinding.searchBar.setOnClickListener(this::onSearchBar);
        mBinding.filter.setOnClickListener(this::onFilter);
        mBinding.filter.setOnLongClickListener(this::onLink);
        mBinding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        mBinding.appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            float factor = Math.abs(verticalOffset * 1f / appBarLayout.getTotalScrollRange());
            int padding = (int) (ResUtil.dp2px(12) * factor);
            if (mBinding.type.getPaddingTop() == padding) return;
            mBinding.type.setPadding(mBinding.type.getPaddingStart(), padding, mBinding.type.getPaddingEnd(), mBinding.type.getPaddingBottom());
        });
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.type.smoothScrollToPosition(position);
                mAdapter.setActivated(position);
                setFabVisible(position);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.setItemAnimator(null);
        mBinding.type.setAdapter(mAdapter = new TypeAdapter(this));
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(getViewLifecycleOwner(), this::setAdapter);
    }

    private void setAdapter(Result result) {
        mAdapter.addAll(mResult = result);
        mBinding.pager.getAdapter().notifyDataSetChanged();
        setFabVisible(0);
        // 【修复·P1 空态文案误用】布局里的空态文字原本写死 config_empty（"请检查配置包是否有效"），
        // 于是"断网/HTTP 404/配置解析失败"也会显示这句话，误导用户。这里按 emptyRes 动态设置。
        mBinding.empty.setText(emptyRes);
        mBinding.empty.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        hideProgress();
        showContent();
    }

    private void setFabVisible(int position) {
        // CustomFabBehavior 对 VISIBLE/INVISIBLE 的按钮做滚动显隐；非活动按钮必须 GONE，
        // 否则上滑时筛选和链接会同时弹出叠在同一角落
        boolean filter = mAdapter.getItemCount() > 0 && !mAdapter.get(position).getFilters().isEmpty();
        mBinding.top.setVisibility(View.INVISIBLE);
        mBinding.filter.setVisibility(filter ? View.VISIBLE : View.GONE);
        mBinding.link.setVisibility(filter ? View.GONE : View.VISIBLE);
    }

    private void setTitle() {
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.title.setText(s));
    }

    private void onTop(View view) {
        getFragment().scrollToTop();
        mBinding.top.setVisibility(View.INVISIBLE);
        if (mBinding.filter.getVisibility() == View.INVISIBLE) mBinding.filter.show();
        else if (mBinding.link.getVisibility() == View.INVISIBLE) mBinding.link.show();
    }

    private boolean onLink(View view) {
        LinkDialog.create(this).launcher(launcher).show();
        return true;
    }

    private void onSite(View view) {
        SiteDialog.create(this).change().show();
    }

    private void onFilter(View view) {
        if (mAdapter.getItemCount() > 0) FilterDialog.create().filter(mAdapter.get(mBinding.pager.getCurrentItem()).getFilters()).show(this);
    }

    private void onSearchBar(View view) {
        SearchActivity.start(requireActivity());
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.keep) KeepActivity.start(requireActivity());
        else if (item.getItemId() == R.id.search) SearchActivity.start(requireActivity());
        else if (item.getItemId() == R.id.history) HistoryActivity.start(requireActivity());
        return true;
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    private void hideContent() {
        mBinding.type.setVisibility(View.INVISIBLE);
        mBinding.pager.setVisibility(View.INVISIBLE);
    }

    private void showContent() {
        mBinding.type.setVisibility(View.VISIBLE);
        mBinding.pager.setVisibility(View.VISIBLE);
    }

    private void homeContent() {
        showProgress();
        setFabVisible(0);
        mAdapter.clear();
        mViewModel.homeContent();
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    public Result getResult() {
        return mResult == null ? new Result() : mResult;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (event.type() == ConfigEvent.Type.VOD) setTitle();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                setTitle();
            case SIZE:
                homeContent();
                break;
            case CATEGORY:
                getFragment().onRefresh();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStateEvent(StateEvent event) {
        switch (event.type()) {
            case EMPTY:
                hideProgress();
                break;
            case PROGRESS:
                showProgress();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        ReceiveDialog.create().event(event).show(this);
    }

    @Override
    public void setConfig(Config config) {
        VodConfig.load(config, new Callback() {
            @Override
            public void start() {
                showProgress();
                hideContent();
                setTitle();
                    }

            @Override
            public void success() {
                emptyRes = R.string.config_empty;   // 【修复·P1 空态文案】正常加载但无内容
            }

            @Override
            public void error(String msg) {
                emptyRes = R.string.error_config_get;   // 【修复·P1 空态文案】加载失败（网络/状态码/解析）
                Notify.dismiss();
                Notify.show(msg);
                showContent();
            }
        });
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public void onItemClick(int position, Class item) {
        mBinding.pager.setCurrentItem(position);
        mAdapter.setActivated(position);
    }

    @Override
    public void setFilter(String key, Value value) {
        getFragment().setFilter(key, value);
    }

    @Override
    public boolean canBack() {
        if (mBinding.pager.getAdapter() == null || mBinding.pager.getAdapter().getCount() == 0) return true;
        if (!getFragment().canBack()) return true;
        getFragment().goBack();
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        VideoActivity.file(requireActivity(), FileChooser.getPathFromUri(result.getData().getData()));
    });

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return FolderFragment.newInstance(getHome().getKey(), type, 4);
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
