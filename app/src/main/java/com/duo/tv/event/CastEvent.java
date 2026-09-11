package com.duo.tv.event;

import com.duo.tv.bean.Config;
import com.duo.tv.bean.Device;
import com.duo.tv.bean.History;

import org.greenrobot.eventbus.EventBus;

public record CastEvent(Config config, Device device, History history) {

    public static void post(Config config, Device device, History history) {
        EventBus.getDefault().post(new CastEvent(config, device, history));
    }
}
