package com.duo.tv.player.engine;

import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MediaTitle;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;

import com.duo.tv.bean.Track;

import java.util.Collections;
import java.util.List;

public interface PlayerEngine {

    int SOFT = 0;
    int HARD = 1;

    Player getPlayer();

    void release();

    Player rebuild(Player.Listener listener);

    int getDecode();

    void setDecode(int decode);

    boolean isHard();

    String getDecodeText();

    void start(PlaySpec spec);

    void setMetadata(MediaMetadata data);

    boolean isLive();

    boolean isVod();

    void setTrack(List<Track> tracks);

    void resetTrack();

    boolean haveTrack(int type);

    Tracks getCurrentTracks();

    /**
     * 【新增·P0 无限重试修复配套】播放真正就绪（STATE_READY）时回调，
     * 引擎在此恢复内部重试额度；默认空实现，兼容其它引擎。
     */
    default void onReady() {
    }

    default boolean haveTitle() {
        return false;
    }

    default List<MediaTitle> getCurrentMediaTitles() {
        return Collections.emptyList();
    }

    String getErrorMessage(PlaybackException e);

    ErrorAction handleError(PlaybackException e);

    enum ErrorAction {
        RECOVERED,
        DECODE,
        FATAL
    }
}
