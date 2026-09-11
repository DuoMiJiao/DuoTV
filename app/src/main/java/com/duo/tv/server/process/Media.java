package com.duo.tv.server.process;

import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;

import com.duo.tv.App;
import com.duo.tv.player.PlayerManager;
import com.duo.tv.server.Nano;
import com.duo.tv.server.Server;
import com.duo.tv.server.impl.Process;
import com.duo.tv.service.PlaybackService;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class Media implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/media");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        PlaybackService service = Server.get().getService();
        if (service == null) return Nano.ok("{}");
        AtomicReference<JsonObject> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        App.post(() -> {
            result.set(build(service.player()));
            latch.countDown();
        });
        try {
            if (latch.await(500, TimeUnit.MILLISECONDS)) return Nano.ok(result.get().toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Nano.ok("{}");
    }

    private JsonObject build(PlayerManager player) {
        MediaItem item = player.getCurrentMediaItem();
        MediaMetadata meta = item != null ? item.mediaMetadata : MediaMetadata.EMPTY;
        JsonObject result = new JsonObject();
        result.addProperty("state", getState(player));
        result.addProperty("speed", player.getSpeed());
        result.addProperty("duration", player.getDuration());
        result.addProperty("position", player.getPosition());
        result.addProperty("url", getString(player.getUrl()));
        result.addProperty("title", getString(meta.title));
        result.addProperty("artist", getString(meta.artist));
        result.addProperty("artwork", getString(meta.artworkUri));
        return result;
    }

    private int getState(PlayerManager player) {
        if (player.isPlaying()) return 3;
        int state = player.getPlaybackState();
        if (state == Player.STATE_BUFFERING) return 6;
        if (state == Player.STATE_READY) return 2;
        return 1;
    }

    private String getString(CharSequence text) {
        return text != null ? text.toString() : "";
    }

    private String getString(Uri uri) {
        return uri != null ? uri.toString() : "";
    }
}