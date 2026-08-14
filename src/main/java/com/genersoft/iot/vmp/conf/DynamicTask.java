package com.genersoft.iot.vmp.conf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic scheduled tasks
 * @author lin
 */
@Slf4j
@Component
public class DynamicTask {

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();


    /**
     * Tasks to be executed in a loop
     * @param key TaskID
     * @param task Task
     * @param cycleForCatalog Interval milliseconds
     * @return
     */
    public void startCron(String key, Runnable task, int cycleForCatalog) {
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.debug("Task【{}】Exists but closed！！！", key);
            } else {
                log.debug("Task【{}】Exists and started！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay You must wait for the previous task to end before starting the timingperiod， cycleForCatalogIndicates the execution interval

        future = taskScheduler.scheduleAtFixedRate(task, new Date(System.currentTimeMillis() + cycleForCatalog), cycleForCatalog);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            log.debug("Task【{}】Started successfully！！！", key);
        }else {
            log.debug("Task【{}】Startup failed！！！", key);
        }
    }

    /**
     * Delayed tasks
     * @param key TaskID
     * @param task Task
     * @param delay delay /milliseconds
     * @return
     */
    public void startDelay(String key, Runnable task, int delay) {
        if(ObjectUtils.isEmpty(key)) {
            return;
        }
        stop(key);

        // Get execution time
        Instant startInstant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(delay));

        ScheduledFuture future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.debug("Task【{}】Exists but closed！！！", key);
            } else {
                log.debug("Task【{}】Exists and started！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay You must wait for the previous task to end before starting the timingperiod， cycleForCatalogIndicates the execution interval
        future = taskScheduler.schedule(task, startInstant);
        if (future != null){
            futureMap.put(key, future);
            runnableMap.put(key, task);
            log.debug("Task【{}】Started successfully！！！", key);
        }else {
            log.debug("Task【{}】Startup failed！！！", key);
        }
    }

    public boolean stop(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        boolean result = false;
        if (!ObjectUtils.isEmpty(futureMap.get(key)) && !futureMap.get(key).isCancelled() && !futureMap.get(key).isDone()) {
            result = futureMap.get(key).cancel(false);
            futureMap.remove(key);
            runnableMap.remove(key);
        }
        return result;
    }

    public boolean contains(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return false;
        }
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    public Runnable get(String key) {
        if(ObjectUtils.isEmpty(key)) {
            return null;
        }
        return runnableMap.get(key);
    }

    /**
     * Check for expired tasks every five minutes and remove them
     */
    @Scheduled(cron="0 0/5 * * * ?")
    public void execute(){
        if (futureMap.size() > 0) {
            for (String key : futureMap.keySet()) {
                ScheduledFuture<?> future = futureMap.get(key);
                if (future.isDone() || future.isCancelled()) {
                    futureMap.remove(key);
                    runnableMap.remove(key);
                }
            }
        }
    }

    public boolean isAlive(String key) {
        return futureMap.get(key) != null && !futureMap.get(key).isDone() && !futureMap.get(key).isCancelled();
    }
}
