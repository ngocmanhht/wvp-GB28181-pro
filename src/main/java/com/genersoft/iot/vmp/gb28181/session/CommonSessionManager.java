package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.CommonCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * General callback management
 */
@Component
public class CommonSessionManager {

    public static Map<String, CommonSession> callbackMap = new ConcurrentHashMap<>();

    /**
     * Store callback-related information
     */
    class CommonSession{
        public String session;
        public long createTime;
        public int timeout;

        public CommonCallback<Object> callback;
        public CommonCallback<String> timeoutCallback;
    }

    /**
     * Add callback
     * @param sessionId unique identifier
     * @param callback callback
     * @param timeout Timeout time, in minutes
     */
    public void add(String sessionId, CommonCallback<Object> callback, CommonCallback<String> timeoutCallback,
                    Integer timeout) {
        CommonSession commonSession = new CommonSession();
        commonSession.session = sessionId;
        commonSession.callback = callback;
        commonSession.createTime = System.currentTimeMillis();
        if (timeoutCallback != null) {
            commonSession.timeoutCallback = timeoutCallback;
        }
        if (timeout != null) {
            commonSession.timeout = timeout;
        }
        callbackMap.put(sessionId, commonSession);
    }

    public void add(String sessionId, CommonCallback<Object> callback) {
        add(sessionId, callback, null, 1);
    }

    public CommonCallback<Object> get(String sessionId, boolean destroy) {
        CommonSession commonSession = callbackMap.get(sessionId);
        if (destroy) {
            callbackMap.remove(sessionId);
        }
        return commonSession.callback;
    }

    public CommonCallback<Object> get(String sessionId) {
        return get(sessionId, false);
    }

    public void delete(String sessionID) {
        callbackMap.remove(sessionID);
    }

    @Scheduled(fixedRate= 60)   //Execute once every minute
    public void execute(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -1);
        for (Map.Entry<String, CommonSession> entry : callbackMap.entrySet()) {
            CommonSession session = entry.getValue();
            if (session == null) {
                continue;
            }
            if (session.createTime < cal.getTimeInMillis()) {
                // timeout
                if (session.timeoutCallback != null) {
                    session.timeoutCallback.run("timeout");
                }
                callbackMap.remove(entry.getKey());
            }
        }
    }
}
