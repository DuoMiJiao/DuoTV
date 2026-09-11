package com.duo.tv.player.extractor;

import android.net.Uri;

import com.duo.tv.player.Source;
import com.duo.tv.utils.UrlUtil;

public class Video implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "video".equals(UrlUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) throws Exception {
        return url.substring(8);
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
