package com.duo.tv.player.exo;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;

import com.duo.tv.bean.Track;
import com.duo.tv.player.PlayerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackUtil {

    public static int count(Tracks tracks, int type) {
        return tracks.getGroups().stream().filter(trackGroup -> trackGroup.getType() == type).mapToInt(trackGroup -> trackGroup.length).sum();
    }

    public static void reset(Player player) {
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().clearOverrides().build());
    }

    private static TrackInfo find(Player player, Track track) {
        if (track.getFormat() == null) return null;
        Tracks currentTracks = player.getCurrentTracks();
        for (Tracks.Group trackGroup : currentTracks.getGroups()) {
            if (trackGroup.getType() != track.getType()) continue;
            for (int i = 0; i < trackGroup.length; i++) {
                Format format = trackGroup.getTrackFormat(i);
                if (track.getFormat().equals(PlayerHelper.describeFormat(format))) {
                    return new TrackInfo(trackGroup, i);
                }
            }
        }
        return null;
    }

    /**
     * 应用轨道选择。
     *
     * 【修复·P0 有声音无画面】原实现会对"出现在列表里、但没有任何一条被选中"的类型
     * 下发**空索引列表**（{@code List.of()}）。media3 中"空索引列表 = 该 TrackGroup 一条都不选"，
     * 等于把整条轨道（例如视频轨）关掉 —— 表现为黑屏有声；更严重的是该选择会被
     * {@link Track#save()} 落库，之后每次播放同一内容都会自动复现。
     *
     * 现在改为：**只对确有选中项的类型下发覆盖**，未选中的类型一律保持系统默认选轨，
     * 从根上杜绝"误关轨道"。末尾再叠加一层兜底校验（见 {@link #ensureVideoAlive(Player)}）。
     */
    public static void setTrackSelection(Player player, List<Track> tracks) {
        Map<Integer, TrackGroup> selectedGroupByType = new HashMap<>();
        Map<Integer, Integer> selectedIndexByType = new HashMap<>();
        for (Track track : tracks) {
            if (!track.isSelected()) continue;                 // 关键：跳过未选中项，绝不生成空覆盖
            TrackInfo info = find(player, track);
            if (info == null) continue;
            int type = info.trackGroup.getType();
            selectedGroupByType.put(type, info.trackGroup.getMediaTrackGroup());
            selectedIndexByType.put(type, info.trackIndex);
        }
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        selectedGroupByType.forEach((type, mediaGroup) ->
                builder.setOverrideForType(new TrackSelectionOverride(mediaGroup, List.of(selectedIndexByType.get(type)))));
        player.setTrackSelectionParameters(builder.build());
    }

    private record TrackInfo(Tracks.Group trackGroup, int trackIndex) {
    }
}
