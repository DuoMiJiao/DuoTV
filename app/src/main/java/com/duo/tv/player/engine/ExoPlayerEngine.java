package com.duo.tv.player.engine;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;

import com.duo.tv.R;
import com.duo.tv.bean.Track;
import com.duo.tv.player.exo.ErrorMsgProvider;
import com.duo.tv.player.exo.ExoUtil;
import com.duo.tv.player.exo.TrackUtil;
import com.duo.tv.utils.ResUtil;

import java.util.List;

public class ExoPlayerEngine implements PlayerEngine {

    private final ErrorMsgProvider provider;
    private PlaySpec spec;
    private Player player;
    private int decode;

    public ExoPlayerEngine(int decode, Player.Listener listener) {
        this.player = ExoUtil.buildPlayer(decode, listener);
        this.provider = new ErrorMsgProvider();
        this.decode = decode;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public Player rebuild(Player.Listener listener) {
        player.release();
        return player = ExoUtil.buildPlayer(decode, listener);
    }

    @Override
    public int getDecode() {
        return decode;
    }

    @Override
    public void setDecode(int decode) {
        this.decode = decode;
    }

    @Override
    public boolean isHard() {
        return decode == HARD;
    }

    @Override
    public String getDecodeText() {
        String[] arr = ResUtil.getStringArray(R.array.select_decode);
        return decode >= 0 && decode < arr.length ? arr[decode] : arr[0];
    }

    @Override
    public void start(PlaySpec spec) {
        this.spec = spec;
        // 【修复·P0 无限重试】重试计数只在这里（真正开始新播放/换集换源）清零。
        // 之前清零写在 startInternal() 里，而重试路径正是通过 startInternal() 重试，
        // 于是计数每次都被抹掉、上限判断永远不成立 → IO/解析类错误会无限重试且不报错。
        httpRetry = 0;
        formatRetry = 0;
        startInternal();
    }

    @Override
    public void setMetadata(MediaMetadata data) {
        MediaItem current = player.getCurrentMediaItem();
        if (current != null) player.replaceMediaItem(player.getCurrentMediaItemIndex(), current.buildUpon().setMediaMetadata(data).build());
    }

    @Override
    public boolean isLive() {
        return player.isCurrentMediaItemLive();
    }

    @Override
    public boolean isVod() {
        return player.getMediaItemCount() > 0 && !player.isCurrentMediaItemLive();
    }

    @Override
    public void setTrack(List<Track> tracks) {
        TrackUtil.setTrackSelection(player, tracks);
    }

    @Override
    public void resetTrack() {
        TrackUtil.reset(player);
    }

    @Override
    public boolean haveTrack(int type) {
        return TrackUtil.count(getCurrentTracks(), type) > 0;
    }

    @Override
    public Tracks getCurrentTracks() {
        return player.getCurrentTracks();
    }

    @Override
    public String getErrorMessage(PlaybackException e) {
        return provider.get(e);
    }

    @Override
    public ErrorAction handleError(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> seekToDefaultPosition();
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED -> ErrorAction.DECODE;
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> retryHttp();
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> retryFormat(e.errorCode);
            default -> ErrorAction.FATAL;
        };
    }

    private void startInternal() {
        // 注意：这里**不要**清零重试计数（见 start(PlaySpec) 的说明），否则会重新变成无限重试。
        player.setMediaItem(ExoUtil.getMediaItem(spec, decode));
        player.prepare();
        player.play();
    }

    private ErrorAction seekToDefaultPosition() {
        player.seekToDefaultPosition();
        player.prepare();
        return ErrorAction.RECOVERED;
    }

    private ErrorAction retryFormat(int errorCode) {
        // 【修复·P0 无限重试】解析类错误用独立计数，避免与 http 重试互相清零；
        // 超过上限后返回 FATAL，交给 Manager 弹出可读错误，而不是静默循环。
        if (formatRetry++ >= 2) return ErrorAction.FATAL;
        spec.setFormat(ExoUtil.getMimeType(errorCode));
        startInternal();
        return ErrorAction.RECOVERED;
    }

    private volatile int httpRetry;
    private volatile int formatRetry;

    @Override
    public void onReady() {
        // 【修复·P0 无限重试】播放真正就绪后才恢复重试额度（由 PlayerManager 在 STATE_READY 调用）。
        httpRetry = 0;
        formatRetry = 0;
    }

    private ErrorAction retryHttp() {
        if (httpRetry++ >= 2) return ErrorAction.FATAL;
        startInternal();
        return ErrorAction.RECOVERED;
    }
}
