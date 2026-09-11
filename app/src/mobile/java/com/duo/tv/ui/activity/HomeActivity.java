package com.duo.tv.ui.activity;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.util.Arrays;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.duo.tv.App;
import com.duo.tv.Setting;
import com.duo.tv.R;
import com.duo.tv.api.config.LiveConfig;
import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.Config;
import com.duo.tv.databinding.ActivityHomeBinding;
import com.duo.tv.db.AppDatabase;
import com.duo.tv.event.ConfigEvent;
import com.duo.tv.event.RefreshEvent;
import com.duo.tv.event.ServerEvent;
import com.duo.tv.event.StateEvent;
import com.duo.tv.impl.Callback;
import com.duo.tv.player.Source;
import com.duo.tv.receiver.ShortcutReceiver;
import com.duo.tv.server.Server;
import com.duo.tv.service.PlaybackService;
import com.duo.tv.ui.base.BaseActivity;
import com.duo.tv.ui.custom.FragmentStateManager;
import com.duo.tv.ui.fragment.SettingFragment;
import com.duo.tv.ui.fragment.SettingPlayerFragment;
import com.duo.tv.ui.fragment.VodFragment;
import com.duo.tv.utils.FileChooser;
import com.duo.tv.utils.Notify;
import com.duo.tv.utils.PermissionUtil;
import com.duo.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private ActivityHomeBinding mBinding;
    private int orientation;

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
        orientation = getResources().getConfiguration().orientation;
        initFragment(savedInstanceState);
        // Duo影视: upstream update check disabled
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
        mBinding.navigation.findViewById(R.id.live).setOnLongClickListener(this::addShortcut);
    }

    private void checkAction(Intent intent) {
        if (intent == null) return;
        // 【修复·P1 导出面收敛（非破坏性）】HomeActivity 是 exported=true 且带 SEND/VIEW 过滤器，
        // 第三方 App 可传入任意文本/URI 触发"直接播放"或"导入配置"。这里加一层白名单：
        // 只接受可解析的媒体/订阅地址（http(s)/clan/file/content/smb/rtsp/rtmp/magnet/ed2k/thunder/jianpian），
        // 其它（如 javascript:、自定义私有 scheme、超长脏串）一律忽略，正常"用 Duo 打开视频"不受影响。
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (isAcceptableUrl(text)) VideoActivity.push(this, text);
            else Log.w("HomeActivity", "ignore SEND intent with unacceptable content");
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if (!isAcceptableScheme(intent.getData())) {
                Log.w("HomeActivity", "ignore VIEW intent with unacceptable scheme: " + intent.getData());
                return;
            }
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    /** 允许的外部内容白名单（URL 文本形式）。 */
    private boolean isAcceptableUrl(String text) {
        if (TextUtils.isEmpty(text)) return false;
        String value = text.trim().toLowerCase();
        String[] prefixes = {"http://", "https://", "clan://", "file:/", "content://", "smb:", "rtsp:", "rtmp:", "magnet:", "ed2k:", "thunder:", "jianpian:"};
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    /** 允许的外部内容白名单（Intent data 的 scheme）。 */
    private boolean isAcceptableScheme(Uri data) {
        String scheme = data.getScheme() == null ? "" : data.getScheme().toLowerCase();
        switch (scheme) {
            case "http":
            case "https":
            case "content":
            case "file":
            case "clan":
            case "smb":
            case "rtsp":
            case "rtmp":
            case "magnet":
            case "ed2k":
            case "thunder":
            case "jianpian":
                return true;
            default:
                return false;
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            loadLive("file:/" + FileChooser.getPathFromUri(intent.getData()));
        } else {
            VideoActivity.push(this, intent.getData().toString());
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) return VodFragment.newInstance();
                if (position == 1) return SettingFragment.newInstance();
                if (position == 2) return SettingPlayerFragment.newInstance();
                return null;
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void ensureLocalConfig() {
        if (!TextUtils.isEmpty(Config.vod().getUrl())) return;
        String[] candidates = {
                "TVBoxOSC/tvbox/api.json", "TVBoxOSC/api.json", "TVBoxOSC/config.json", "TVBoxOSC/tvbox.json",
                "TVBox/api.json", "TVBox/config.json", "TVBox/duo_config.json", "TVBox/duo.json",
                "tvbox/api.json"
        };
        for (String candidate : candidates) {
            File file = new File(Environment.getExternalStorageDirectory(), candidate);
            if (file.exists() && isUsableConfig(file)) {
                Config.find("clan://localhost/" + candidate, "Duo", 0).save();
                return;
            }
        }
        File dir = new File(Environment.getExternalStorageDirectory(), "TVBox");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) return;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File file : files) {
            if (!isUsableConfig(file)) continue;
            Config.find("clan://localhost/TVBox/" + file.getName(), "Duo", 0).save();
            return;
        }
    }

    private boolean isUsableConfig(File file) {
        try {
            JsonObject object = JsonParser.parseString(Path.read(file)).getAsJsonObject();
            return object.has("sites") || object.has("lives") || object.has("urls");
        } catch (Throwable e) {
            return false;
        }
    }

    private void initConfig() {
        ensureLocalConfig();
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
            }

            @Override
            public void error(String msg) {
                checkAction(getIntent());
                StateEvent.empty();
                Notify.show(msg);
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                openLive();
            }
        });
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl());
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private boolean addShortcut(View view) {
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, getString(R.string.nav_live)).setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher)).setIntent(new Intent(Intent.ACTION_VIEW, null, this, LiveActivity.class)).setShortLabel(getString(R.string.nav_live)).build();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, ShortcutReceiver.class).setAction(ShortcutReceiver.ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ShortcutManagerCompat.requestPinShortcut(this, info, pendingIntent.getIntentSender());
        return true;
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.home();
                break;
            case COMMON:
                setNavigation();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.type() == ServerEvent.Type.PUSH) VideoActivity.push(this, event.text());
        if (event.type() == ServerEvent.Type.SEARCH) SearchActivity.start(this, event.text());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.live) return openLive();
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.post(() -> checkOrientation(newConfig), 100);
    }

    private void checkOrientation(Configuration newConfig) {
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation;
            RefreshEvent.home();
        }
    }

    @Override
    protected void onBackInvoked() {
        if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (mManager.isVisible(2)) {
            change(1);
        } else if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            if (PlaybackService.isRunning()) moveTaskToBack(true);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            LiveConfig.get().clear();
            VodConfig.get().clear();
            AppDatabase.backup();
            OkHttp.get().clear();
            Source.get().exit();
            Server.get().stop();
        }
        super.onDestroy();
    }
}
