package com.duo.tv.player;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MediaTitle;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;

import com.duo.tv.App;
import com.duo.tv.Constant;
import com.duo.tv.R;
import com.duo.tv.Setting;
import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.Danmaku;
import com.duo.tv.bean.Result;
import com.duo.tv.bean.Sub;
import com.duo.tv.bean.Track;
import com.duo.tv.impl.ParseCallback;
import com.duo.tv.player.danmaku.DanPlayer;
import com.duo.tv.player.danmaku.DanmakuOverlay;
import com.duo.tv.player.engine.ExoPlayerEngine;
import com.duo.tv.player.engine.PlaySpec;
import com.duo.tv.player.engine.PlayerEngine;
import com.duo.tv.utils.Notify;
import com.duo.tv.utils.ResUtil;
import com.duo.tv.utils.Task;
import com.duo.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;
import com.google.common.net.HttpHeaders;

import okhttp3.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



public class PlayerManager implements ParseCallback {

    private final Runnable runnable;
    private final Callback callback;
    private PlayerEngine engine;
    private DanPlayer danPlayer;
    private VideoSize videoSize;
    private ParseJob parseJob;
    private PlaySpec spec;
    private Player player;

    private boolean initTrack;   // 旧版本用于"轨道偏好只套用一次"的守卫，现已废弃（见 onTracksChanged）
    private int retry;

    public PlayerManager(Callback callback) {
        this.runnable = () -> callback.onError(ResUtil.getString(R.string.error_play_timeout));
        this.engine = new ExoPlayerEngine(PlayerEngine.HARD, listener);
        this.player = engine.getPlayer();
        this.callback = callback;
    }

    public void release() {
        stopParse();
        App.removeCallbacks(runnable);
        if (danPlayer != null) {
            danPlayer.release();
            danPlayer = null;
        }
        if (engine != null) {
            player.removeListener(listener);
            engine.release();
            engine = null;
            player = null;
        }
    }

    public Player getPlayer() {
        return player;
    }

    public Tracks getCurrentTracks() {
        return engine.getCurrentTracks();
    }

    public List<MediaTitle> getCurrentMediaTitles() {
        return engine.getCurrentMediaTitles();
    }

    public MediaItem getCurrentMediaItem() {
        return player.getCurrentMediaItem();
    }

    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public String getUrl() {
        return spec != null ? spec.getUrl() : null;
    }

    public String getKey() {
        return spec != null ? spec.getKey() : null;
    }

    public List<Danmaku> getDanmakus() {
        return spec != null ? spec.getDanmakus() : null;
    }

    public MediaMetadata getMetadata() {
        return spec != null ? spec.getMetadata() : null;
    }

    public Map<String, String> getHeaders() {
        return spec == null || spec.getHeaders() == null ? new HashMap<>() : spec.getHeaders();
    }

    public float getSpeed() {
        return player.getPlaybackParameters().speed;
    }

    public boolean isEmpty() {
        return spec == null || TextUtils.isEmpty(spec.getUrl());
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public boolean isLandscape() {
        return getVideoWidth() > getVideoHeight();
    }

    public boolean isLive() {
        return engine.isLive();
    }

    public boolean isVod() {
        return engine.isVod();
    }

    public boolean haveTrack(int type) {
        return engine.haveTrack(type);
    }

    public boolean haveTitle() {
        return engine.haveTitle();
    }

    public boolean haveDanmaku() {
        return getDanmakus() != null && getDanmakus().stream().anyMatch(Danmaku::isSelected);
    }

    public boolean canSetOpening(long position, long duration) {
        return position > 0 && duration > 0 && position <= Constant.getOpEdLimit(duration);
    }

    public boolean canSetEnding(long position, long duration) {
        return position > 0 && duration > 0 && duration - position <= Constant.getOpEdLimit(duration);
    }

    public int getVideoWidth() {
        return videoSize == null ? 0 : videoSize.width;
    }

    public int getVideoHeight() {
        return videoSize == null ? 0 : videoSize.height;
    }

    public long getPosition() {
        return player.getCurrentPosition();
    }

    public String getSizeText() {
        return (getVideoWidth() == 0 && getVideoHeight() == 0) ? "" : getVideoWidth() + " x " + getVideoHeight();
    }

    public String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", getSpeed());
    }

    public String getDecodeText() {
        return engine.getDecodeText();
    }

    public String getPositionTime(long delta) {
        long time = Math.max(0, Math.min(getPosition() + delta, Math.max(0, getDuration())));
        return Util.timeMs(time);
    }

    public long getDuration() {
        return player.getDuration();
    }

    public String getDurationTime() {
        return Util.timeMs(Math.max(0, getDuration()));
    }

    public void setSub(Sub sub) {
        if (spec != null) spec.setSub(sub);
        setMediaItem();
    }

    public void setFormat(String format) {
        if (spec != null) spec.setFormat(format);
        setMediaItem();
    }

    public void setTitle(MediaTitle title) {
        if (spec != null) spec.setUrl(spec.getUri().buildUpon().fragment("title=" + title.index).build().toString());
        setMediaItem();
        seekTo(0);
    }

    public static MediaMetadata buildMetadata(String title, String artist, String artUri) {
        Uri artwork = TextUtils.isEmpty(artUri) ? null : Uri.parse(artUri);
        return new MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(artwork).build();
    }

    public void setMetadata(MediaMetadata data) {
        if (spec != null) spec.setMetadata(data);
        engine.setMetadata(data);
    }

    public void setDanmakuView(DanmakuOverlay view) {
        danPlayer = new DanPlayer(view);
        danPlayer.attachPlayer(player);
        if (spec != null) setDanmakus(spec.getDanmakus());
    }

    public void setDanmakuArea(float fraction) {
        if (danPlayer != null) danPlayer.setArea(fraction);
    }

    public void setDanmakuSpeed(float multiplier) {
        if (danPlayer != null) danPlayer.setSpeed(multiplier);
    }

    public void setDanmakuShow(boolean show) {
        if (danPlayer != null) danPlayer.setShow(show);
    }

    public void setDanmakuAlpha(int percent) {
        if (danPlayer != null) danPlayer.setAlpha(percent);
    }

    public void setDanmakuBlock(boolean scroll, boolean top, boolean bottom, java.util.List<String> keywords) {
        if (danPlayer != null) danPlayer.setBlock(scroll, top, bottom, keywords);
    }

    public void setDanmakuSize(float size) {
        if (danPlayer != null) danPlayer.setTextSize(size);
    }

    public void setDanmakuColor(boolean enabled) {
        if (danPlayer != null) danPlayer.setColor(enabled);
    }

    public void setDanmakuStroke(boolean enabled) {
        if (danPlayer != null) danPlayer.setStroke(enabled);
    }

    public String setSpeed(float speed) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return getSpeedText();
        player.setPlaybackParameters(player.getPlaybackParameters().withSpeed(speed));
        return getSpeedText();
    }

    public String addSpeed() {
        float speed = getSpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.25f : Math.min(speed + addon, 5.0f);
        return setSpeed(speed);
    }

    public String addSpeed(float value) {
        return setSpeed(Math.min(getSpeed() + value, 5));
    }

    public String subSpeed(float value) {
        return setSpeed(Math.max(getSpeed() - value, 0.25f));
    }

    public String toggleSpeed() {
        return setSpeed(getSpeed() == 1 ? Setting.getSpeed() : 1);
    }

    public void setTrack(List<Track> tracks) {
        if (!tracks.isEmpty()) engine.setTrack(tracks);
    }

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void stop() {
        if (danPlayer != null) danPlayer.stop();
        player.stop();
        stopParse();
    }

    public void setRepeatOne(boolean repeat) {
        player.setRepeatMode(repeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    public void seekTo(long time) {
        player.seekTo(time);
    }

    public void reset() {
        App.removeCallbacks(runnable);
        retry = 0;
    }

    public void clear() {
        spec = null;
    }

    public void resetTrack() {
        engine.resetTrack();
    }

    public void toggleDecode() {
        engine.setDecode(engine.isHard() ? PlayerEngine.SOFT : PlayerEngine.HARD);
        rebuildPlayer();
        setMediaItem();
    }

    private void rebuildPlayer() {
        player = engine.rebuild(listener);
        if (danPlayer != null) danPlayer.attachPlayer(player);
        callback.onPlayerRebuild(player);
    }

    public void start(PlaySpec spec, long timeout) {
        this.spec = spec;
        setMediaItem(timeout);
    }

    public void parse(String key, Result result, boolean useParse, MediaMetadata metadata) {
        stopParse();
        spec = PlaySpec.fromParse(result, key, metadata);
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
    }

    public void setMediaItem() {
        setMediaItem(Constant.TIMEOUT_PLAY);
    }

    private void setMediaItem(long timeout) {
        if (spec == null || spec.getUrl() == null) return;
        setDanmakus(spec.getDanmakus());
        engine.start(spec.checkUa());
        App.post(runnable, timeout);
        callback.onPrepare();
    }

    public void startBrowse(PlaySpec spec) {
        reset();
        clear();
        stopParse();
        start(spec, Constant.TIMEOUT_PLAY);
    }

    private void setDanmakus(List<Danmaku> items) {
        if (danPlayer == null) return;
        if (autoDanmaku(items)) return;
        if (items != null && !items.isEmpty()) setDanmaku(items.get(0));
    }

    /** Built-in proxy (config danmaku template) first; source-provided candidates only as fallback. */
    private boolean autoDanmaku(List<Danmaku> fallback) {
        if (danPlayer == null || spec == null || spec.getMetadata() == null || !Setting.isDanmakuLoad()) return false;
        String template = VodConfig.get().getConfig().getDanmaku();
        CharSequence title = spec.getMetadata().title;
        CharSequence artist = spec.getMetadata().artist;
        String name = title == null ? "" : title.toString().trim();
        String episode = artist == null ? "" : artist.toString().trim();
        if (TextUtils.isEmpty(template) || TextUtils.isEmpty(name)) return false;
        String url = template.replace("{name}", Trans.t2s(name)).replace("{episode}", episode);
        String tag = "danmaku_auto";
        OkHttp.cancel(tag);
        final PlaySpec currentSpec = spec;
        Task.submit(() -> {
            List<Danmaku> list = null;
            try (Response response = OkHttp.newCall(url, tag).execute()) {
                // 【修复·P1】补状态码判断：404/500 返回的是错误页，直接当 JSON 解析只会抛异常
                if (response.isSuccessful() && response.body() != null) list = Danmaku.arrayFrom(response.body().string());
            } catch (Throwable e) {
                // 【修复·P1 站点自带弹幕被吞】原实现只 printStackTrace 就结束，
                // 导致"内置弹幕接口一失败，站点自带弹幕也一起没了（整集无弹幕且无提示）"。
                // 现在异常与"空结果"走同一条回退路径。
                android.util.Log.w("PlayerManager", "auto danmaku failed: " + url, e);
            }
            final List<Danmaku> result = list;
            App.post(() -> {
                if (spec != currentSpec) return;
                if (result != null && !result.isEmpty()) setDanmaku(result.get(0));
                else if (fallback != null && !fallback.isEmpty()) setDanmaku(fallback.get(0));
            });
        });
        return true;
    }

    public void setDanmaku(Danmaku item) {
        if (spec != null) spec.setDanmaku(item);
        if (danPlayer != null) danPlayer.setDanmaku(item);
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!TextUtils.isEmpty(from)) Notify.show(ResUtil.getString(R.string.parse_from, from));
        if (headers != null) headers.remove(HttpHeaders.RANGE);
        if (spec != null) spec.setHeaders(headers);
        if (spec != null) spec.setUrl(url);
        setMediaItem();
    }

    @Override
    public void onParseError() {
        callback.onError(ResUtil.getString(R.string.error_play_parse));
    }

    public interface Callback {

        void onPrepare();

        void onTracksChanged();

        void onTitlesChanged();

        void onError(String msg);

        void onPlayerRebuild(Player newPlayer);
    }

    private final Player.Listener listener = new Player.Listener() {

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state != Player.STATE_IDLE) App.removeCallbacks(runnable);
            // 【修复·P0 无限重试】真正就绪后让引擎恢复重试额度（否则计数会一直累计到"不重试"）。
            if (state == Player.STATE_READY) engine.onReady();
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize size) {
            videoSize = size;
        }

        @Override
        public void onTracksChanged(@NonNull Tracks tracks) {
            if (tracks.isEmpty()) return;
            // 【修复·P1 主线程查库】把 Track.find 从主线程移到后台，避免高频换台卡顿
            Task.execute(() -> {
                List<Track> saved = Track.find(getKey());
                if (!saved.isEmpty()) App.post(() -> setTrack(saved));
            });
            callback.onTracksChanged();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException e) {
            PlayerEngine.ErrorAction action = engine.handleError(e);
            if (action == PlayerEngine.ErrorAction.RECOVERED) return;
            if (++retry > 2) {
                callback.onError(engine.getErrorMessage(e));
                return;
            }
            switch (action) {
                case DECODE:
                    toggleDecode();
                    break;
                case FATAL:
                    callback.onError(engine.getErrorMessage(e));
                    break;
            }
        }
    };
}
