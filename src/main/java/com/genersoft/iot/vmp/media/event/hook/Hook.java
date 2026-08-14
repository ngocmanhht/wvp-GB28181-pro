package com.genersoft.iot.vmp.media.event.hook;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hookevent parameters
 * @author lin
 */
@Getter
@Setter
public class Hook {

    private HookType hookType;

    private String app;

    private String stream;

    private Long expireTime;


    public static Hook getInstance(HookType hookType, String app, String stream) {
        Hook hookSubscribe = new Hook();
        hookSubscribe.setApp(app);
        hookSubscribe.setStream(stream);
        hookSubscribe.setHookType(hookType);
        hookSubscribe.setExpireTime(System.currentTimeMillis() + 5 * 60 * 1000);
        return hookSubscribe;
    }

    public static Hook getInstance(HookType hookType, String app, String stream, String mediaServer) {
        // TODO All subsequent modification methods
        return Hook.getInstance(hookType, app, stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hook) {
            Hook param = (Hook) obj;
            return param.getHookType().equals(this.hookType)
                    && param.getApp().equals(this.app)
                    && param.getStream().equals(this.stream);
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getHookType() + this.getApp() + this.getStream();
    }
}
