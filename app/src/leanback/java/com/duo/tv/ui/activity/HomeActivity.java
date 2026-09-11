package com.duo.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.App;
import com.duo.tv.Product;
import com.duo.tv.R;
import com.duo.tv.Updater;
import com.duo.tv.api.config.LiveConfig;
import com.duo.tv.api.config.VodConfig;
import com.duo.tv.api.config.WallConfig;
import com.duo.tv.bean.Cache;
import com.duo.tv.bean.Config;
import com.duo.tv.bean.Func;
import com.duo.tv.bean.History;
import com.duo.tv.bean.Result;
import com.duo.tv.bean.Site;
import com.duo.tv.bean.Style;
import com.duo.tv.bean.Vod;
import com.duo.tv.databinding.ActivityHomeBinding;
import com.duo.tv.db.AppDatabase;
import com.duo.tv.event.CastEvent;
import com.duo.tv.event.ConfigEvent;
import com.duo.tv.event.RefreshEvent;
import com.duo.tv.event.ServerEvent;
import com.duo.tv.impl.Callback;
import com.duo.tv.model.SiteViewModel;
import com.duo.tv.player.Source;
import com.duo.tv.server.Server;
import com.duo.tv.service.DLNARendererService;
import com.duo.tv.service.PlaybackService;
import com.duo.tv.ui.adapter.BaseDiffCallback;
import com.duo.tv.ui.base.BaseActivity;
import com.duo.tv.ui.custom.CustomRowPresenter;
import com.duo.tv.ui.custom.CustomSelector;
import com.duo.tv.ui.custom.CustomTitleView;
import com.duo.tv.ui.dialog.SiteDialog;
import com.duo.tv.ui.presenter.FuncPresenter;
import com.duo.tv.ui.presenter.HeaderPresenter;
import com.duo.tv.ui.presenter.HistoryPresenter;
import com.duo.tv.ui.presenter.ProgressPresenter;
import com.duo.tv.ui.presenter.VodPresenter;
import com.duo.tv.utils.Clock;
import com.duo.tv.utils.FileChooser;
import com.duo.tv.utils.ImgUtil;
import com.duo.tv.utils.KeyUtil;
import com.duo.tv.utils.Notify;
import com.duo.tv.utils.PermissionUtil;
import com.duo.tv.utils.ResUtil;
import com.duo.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HomeActivity extends BaseActivity implements CustomTitleView.Listener, VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mHistoryAdapter;
    private ArrayObjectAdapter mFuncAdapter;
    private ArrayObjectAdapter mAdapter;
    private HistoryPresenter mPresenter;
    private SiteViewModel mViewModel;
    private Result mResult;
    private Clock mClock;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mResult = Result.empty();
        mClock = Clock.create(mBinding.clock);
        mBinding.progressLayout.showProgress();
        DLNARendererService.start(this);
        Updater.create().start(this);
        setRecyclerView();
        setViewModel();
        setAdapter();
        initConfig();
        setTitle();
        setLogo();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.toolbar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                if (mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(text)) VideoActivity.push(this, text);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            loadLive("file:/" + FileChooser.getPathFromUri(intent.getData()));
        } else {
            VideoActivity.push(this, intent.getData().toString());
        }
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_SMALL, HorizontalGridView.FOCUS_SCROLL_ALIGNED), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            mAdapter.remove("progress");
            addVideo(mResult = result);
            Cache.clear().put(result);
        });
    }

    private void setAdapter() {
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        mAdapter.add(new ListRow(mFuncAdapter = new ArrayObjectAdapter(new FuncPresenter(this))));
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
    }

    private void setTitle() {
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.title.setText(s));
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                showContent();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                showContent();
            }
        };
    }

    private void showContent() {
        mBinding.progressLayout.showContent();
        checkAction(getIntent());
        setFocus();
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    private void setFocus() {
        mBinding.title.setSelected(true);
        App.post(() -> mBinding.title.setFocusable(true), 500);
        if (!mBinding.title.hasFocus()) mBinding.recycler.requestFocus();
    }

    private void getVideo() {
        mResult = Result.empty();
        int index = getRecommendIndex();
        boolean gone = mAdapter.indexOf("progress") == -1;
        boolean hasItem = gone && mAdapter.size() > index;
        if (hasItem) mAdapter.removeItems(index, mAdapter.size() - index);
        if (gone) mAdapter.add("progress");
        mViewModel.homeContent();
    }

    private void addVideo(Result result) {
        Style style = result.getStyle(getHome().getStyle());
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
    }

    private void addGrid(List<Vod> items, Style style) {
        List<ListRow> rows = new ArrayList<>();
        VodPresenter presenter = new VodPresenter(this, style);
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            adapter.addAll(0, part);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private void setFunc() {
        List<Func> items = new ArrayList<>();
        items.add(Func.create(R.string.home_vod));
        if (LiveConfig.hasUrl()) items.add(Func.create(R.string.home_live));
        items.add(Func.create(R.string.home_search));
        items.add(Func.create(R.string.home_keep));
        items.add(Func.create(R.string.home_push));
        items.add(Func.create(R.string.home_setting));
        mFuncAdapter.setItems(items, new BaseDiffCallback<Func>());
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean exist = recommendIndex - historyIndex == 2;
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        if ((items.isEmpty() && exist) || (renew && exist)) mAdapter.removeItems(historyIndex, 1);
        if ((!items.isEmpty() && !exist) || (renew && exist)) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, new BaseDiffCallback<History>());
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        mAdapter.removeItems(getHistoryIndex(), 1);
        History.delete(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private int getHistoryIndex() {
        return mAdapter.indexOf(R.string.home_history) + 1;
    }

    private int getRecommendIndex() {
        return mAdapter.indexOf(R.string.home_recommend) + 1;
    }

    private void setLogo() {
        ImgUtil.logo(mBinding.logo);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.history();
                RefreshEvent.home();
                setLogo();
                break;
            case COMMON:
                setFunc();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                getVideo();
                setTitle();
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history().save(VodConfig.getCid()));
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public void onItemClick(Func item) {
        if (item.getResId() == R.string.home_vod) VodActivity.start(this, mResult);
        else if (item.getResId() == R.string.home_live) LiveActivity.start(this);
        else if (item.getResId() == R.string.home_keep) KeepActivity.start(this);
        else if (item.getResId() == R.string.home_push) PushActivity.start(this);
        else if (item.getResId() == R.string.home_search) SearchActivity.start(this);
        else if (item.getResId() == R.string.home_setting) SettingActivity.start(this);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) mViewModel.action(getHome().getKey(), item.getAction());
        else if (getHome().isIndex()) CollectActivity.start(this, item.getName());
        else VideoActivity.start(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).show();
    }

    @Override
    public void onRefresh() {
        getVideo();
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        if (KeyUtil.isActionDown(event) & KeyUtil.isDownKey(event) && getCurrentFocus() == mBinding.title) return mBinding.recycler.getChildAt(0).requestFocus();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.progressLayout.isProgress()) {
            showContent();
        } else if (mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else {
            if (PlaybackService.isRunning()) moveTaskToBack(true);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            DLNARendererService.stop(this);
            LiveConfig.get().clear();
            VodConfig.get().clear();
            AppDatabase.backup();
            OkHttp.get().clear();
            Source.get().exit();
            Server.get().stop();
        } else {
            mClock.stop();
        }
        super.onDestroy();
    }
}