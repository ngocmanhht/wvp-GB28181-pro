package com.genersoft.iot.vmp.conf.redis;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcClassHandler;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisRpcConfig implements MessageListener {

    public final static String REDIS_REQUEST_CHANNEL_KEY = "WVP_REDIS_REQUEST_CHANNEL_KEY";

    private final Random random = new Random();

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private TaskExecutor taskExecutor;

    private final static Map<String, RedisRpcClassHandler> protocolHash = new HashMap<>();

    public void addHandler(String path, RedisRpcClassHandler handler) {
        protocolHash.put(path, handler);
    }

//    @Override
//    public void run(String... args) throws Exception {
//        List<Class<?>> classList = ClassUtil.getClassList("com.genersoft.iot.vmp.service.redisMsg.control", RedisRpcController.class);
//        for (Class<?> handlerClass : classList) {
//            String controllerPath = handlerClass.getAnnotation(RedisRpcController.class).value();
//            Object bean = ClassUtil.getBean(controllerPath, handlerClass);
//            // Scan the method below
//            Method[] methods = handlerClass.getDeclaredMethods();
//            for (Method method : methods) {
//                RedisRpcMapping annotation = method.getAnnotation(RedisRpcMapping.class);
//                if (annotation != null) {
//                    String methodPath =  annotation.value();
//                    if (methodPath != null) {
//                        protocolHash.put(controllerPath + "/" + methodPath, new RedisRpcClassHandler(bean, method));
//                    }
//                }
//
//            }
//
//        }
//        for (String s : protocolHash.keySet()) {
//            System.out.println(s);
//        }
//        if (log.isDebugEnabled()) {
//            log.debug("Message ID cache table protocolHash:{}", protocolHash);
//        }
//    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        RedisRpcMessage redisRpcMessage = JSON.parseObject(new String(msg.getBody()), RedisRpcMessage.class);
                        if (redisRpcMessage.getRequest() != null) {
                            handlerRequest(redisRpcMessage.getRequest());
                        } else if (redisRpcMessage.getResponse() != null){
                            handlerResponse(redisRpcMessage.getResponse());
                        } else {
                            log.error("[redis-rpc]Parsing failed {}", JSON.toJSONString(redisRpcMessage));
                        }
                    } catch (Exception e) {
                        log.error("[redis-rpc]parsing exception {}",new String(msg.getBody()), e);
                    }
                }
            });
        }
    }

    private void handlerResponse(RedisRpcResponse response) {
        if (userSetting.getServerId().equals(response.getToId())) {
            return;
        }
        log.info("[redis-rpc] << {}", response);
        response(response);
    }

    private void handlerRequest(RedisRpcRequest request) {
        try {
            if (userSetting.getServerId().equals(request.getFromId())) {
                return;
            }
            log.info("[redis-rpc] << {}", request);
            RedisRpcClassHandler redisRpcClassHandler = protocolHash.get(request.getUri());
            if (redisRpcClassHandler == null) {
                log.error("[redis-rpc] path: {}does not exist", request.getUri());
                return;
            }
            RpcController controller = redisRpcClassHandler.getController();
            Method method = redisRpcClassHandler.getMethod();
            // If the target ID is not carried, it can be understood that whichever wvp has the result will reply, and the target ID is carried, but if it is a uri that does not exist, it will reply directly.404
            if (userSetting.getServerId().equals(request.getToId())) {
                if (method == null) {
                    // Reply 404 result
                    RedisRpcResponse response = request.getResponse();
                    response.setStatusCode(ErrorCode.ERROR404.getCode());
                    sendResponse(response);
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(controller, request);
                if(response != null) {
                    sendResponse(response);
                }
            }else {
                if (method == null) {
                    // Reply 404 result
                    RedisRpcResponse response = request.getResponse();
                    response.setStatusCode(ErrorCode.ERROR404.getCode());
                    sendResponse(response);
                    return;
                }
                RedisRpcResponse response = (RedisRpcResponse)method.invoke(controller, request);
                if (response != null) {
                    sendResponse(response);
                }
            }
        }catch (Exception e) {
            log.error("[redis-rpc ] Processing request failed ", e);
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            sendResponse(response);
        }
    }

    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }

    private void sendRequest(RedisRpcRequest request){
        log.info("[redis-rpc] >> {}", request);
        RedisRpcMessage message = new RedisRpcMessage();
        message.setRequest(request);
        redisTemplate.convertAndSend(REDIS_REQUEST_CHANNEL_KEY, message);
    }

    private final Map<Long, SynchronousQueue<RedisRpcResponse>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<Long, CommonCallback<RedisRpcResponse>> callbacks = new ConcurrentHashMap<>();

    public RedisRpcResponse request(RedisRpcRequest request, long timeOut) {
        return request(request, timeOut, TimeUnit.SECONDS);
    }

    public RedisRpcResponse request(RedisRpcRequest request, long timeOut, TimeUnit timeUnit) {
        request.setSn((long) random.nextInt(1000) + 1);
        SynchronousQueue<RedisRpcResponse> subscribe = subscribe(request.getSn());

        try {
            sendRequest(request);
            return subscribe.poll(timeOut, timeUnit);
        } catch (InterruptedException e) {
            log.warn("[redis rpc timeout] uri: {}, sn: {}", request.getUri(), request.getSn(), e);
            RedisRpcResponse redisRpcResponse = new RedisRpcResponse();
            redisRpcResponse.setStatusCode(ErrorCode.ERROR486.getCode());
            return redisRpcResponse;
        } finally {
            this.unsubscribe(request.getSn());
        }
    }

    public void request(RedisRpcRequest request, CommonCallback<RedisRpcResponse> callback) {
        request.setSn((long) random.nextInt(1000) + 1);
        setCallback(request.getSn(), callback);
        sendRequest(request);
    }

    public Boolean response(RedisRpcResponse response) {
        SynchronousQueue<RedisRpcResponse> queue = topicSubscribers.get(response.getSn());
        CommonCallback<RedisRpcResponse> callback = callbacks.get(response.getSn());
        if (queue != null) {
            try {
                return queue.offer(response, 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage(), e);
            }
        }else if (callback != null) {
            callback.run(response);
            callbacks.remove(response.getSn());
        }
        return false;
    }

    private void unsubscribe(long key) {
        topicSubscribers.remove(key);
    }


    private SynchronousQueue<RedisRpcResponse> subscribe(long key) {
        SynchronousQueue<RedisRpcResponse> queue = null;
        if (!topicSubscribers.containsKey(key))
            topicSubscribers.put(key, queue = new SynchronousQueue<>());
        return queue;
    }

    private void setCallback(long key, CommonCallback<RedisRpcResponse> callback)  {
        // TODO There will be problems if multiple superiors request the same channel.
        callbacks.put(key, callback);
    }

    public void removeCallback(long key)  {
        callbacks.remove(key);
    }


    public int getCallbackCount(){
        return callbacks.size();
    }




//    @Scheduled(fixedRate = 1000)   //Executed every 1 second
//    public void execute(){
//        logger.info("callbackslength: " + callbacks.size());
//        logger.info("Queue length: " + topicSubscribers.size());
//        logger.info("HOOKlength of listening: " + hookSubscribe.size());
//        logger.info("");
//    }
}
