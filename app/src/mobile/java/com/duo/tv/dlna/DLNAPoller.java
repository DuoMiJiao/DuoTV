package com.duo.tv.dlna;

import com.duo.tv.App;
import com.duo.tv.bean.Device;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.support.avtransport.callback.GetPositionInfo;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// ponytail: single global scheduler, per-device pollers if throughput matters
public class DLNAPoller {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "DLNAPoller"); t.setDaemon(true); return t; });
    private static ScheduledFuture<?> future;
    private static volatile Listener listener;
    private static volatile RemoteService service;
    private static volatile ControlPoint control;

    public interface Listener {
        void onPositionUpdate(long positionMs, long durationMs);
    }

    public static void start(Device device, Listener l) {
        stop();
        listener = l;
        DLNACastManager mgr = DLNACastManager.get();
        control = mgr.getControlPoint();
        service = mgr.findAVTransport(device);
        if (service == null || control == null) return;
        future = scheduler.scheduleAtFixedRate(DLNAPoller::poll, 0, 2, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (future != null) { future.cancel(false); future = null; }
        listener = null;
        service = null;
        control = null;
    }

    private static void poll() {
        if (control == null || service == null) return;
        try {
            control.execute(new GetPositionInfo(service) {
                @Override
                public void received(ActionInvocation invocation, org.jupnp.support.model.PositionInfo info) {
                    long dur = parseTime(info.getTrackDuration());
                    long pos = parseTime(info.getRelTime());
                    Listener l = listener;
                    if (l != null) App.post(() -> l.onPositionUpdate(pos, dur));
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static long parseTime(String time) {
        if (time == null || time.isEmpty() || time.equals("NOT_IMPLEMENTED") || time.equals("00:00:00")) return 0;
        try {
            String[] parts = time.split(":");
            if (parts.length == 3) {
                return (Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2])) * 1000;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
