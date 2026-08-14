package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.utils.SSLSocketClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AssistRESTfulUtils {

    private OkHttpClient client;


    public interface RequestCallback{
        void run(JSONObject response);
    }

    private OkHttpClient getClient(){
        return getClient(null);
    }

    private OkHttpClient getClient(Integer readTimeOut){
        if (client == null) {
            if (readTimeOut == null) {
                readTimeOut = 10;
            }
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            // Set connection timeout
            httpClientBuilder.connectTimeout(8, TimeUnit.SECONDS);
            // Set read timeout
            httpClientBuilder.readTimeout(readTimeOut,TimeUnit.SECONDS);
            // Set up connection pool
            httpClientBuilder.connectionPool(new ConnectionPool(16, 5, TimeUnit.MINUTES));
            if (log.isDebugEnabled()) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                    log.debug("httpRequest parameters：" + message);
                });
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                // OkHttpAdd interceptorloggingInterceptor
                httpClientBuilder.addInterceptor(logging);
            }
            X509TrustManager manager = SSLSocketClientUtil.getX509TrustManager();
            // settingsssl
            httpClientBuilder.sslSocketFactory(SSLSocketClientUtil.getSocketFactory(manager), manager);
            httpClientBuilder.hostnameVerifier(SSLSocketClientUtil.getHostnameVerifier());//Ignore validation
            client = httpClientBuilder.build();
        }
        return client;

    }


    public JSONObject sendGet(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        OkHttpClient client = getClient();

        if (mediaServerItem == null) {
            return null;
        }
        if (mediaServerItem.getRecordAssistPort() <= 0) {
            log.warn("Assist service is not enabled");
            return null;
        }
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(api);
        JSONObject responseJSON = null;

        if (param != null && !param.keySet().isEmpty()) {
            stringBuffer.append("?");
            int index = 1;
            for (String key : param.keySet()){
                if (param.get(key) != null) {
                    stringBuffer.append(key + "=" + param.get(key));
                    if (index < param.size()) {
                        stringBuffer.append("&");
                    }
                }
                index++;
            }
        }

        String url = stringBuffer.toString();
        log.info("[visitassist]： {}", url);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
            if (callback == null) {
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String responseStr = responseBody.string();
                            responseJSON = JSON.parseObject(responseStr);
                        }
                    }else {
                        response.close();
                        Objects.requireNonNull(response.body()).close();
                    }
                } catch (ConnectException e) {
                    log.error(String.format("Failed to connect to Assist: %s, %s", e.getCause().getMessage(), e.getMessage()));
                    log.info("Please check the media configuration and confirm that Assist is started...");
                }catch (IOException e) {
                    log.error(String.format("[ %s ]Request failed: %s", url, e.getMessage()));
                }
            }else {
                client.newCall(request).enqueue(new Callback(){

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response){
                        if (response.isSuccessful()) {
                            try {
                                String responseStr = Objects.requireNonNull(response.body()).string();
                                callback.run(JSON.parseObject(responseStr));
                            } catch (IOException e) {
                                log.error(String.format("[ %s ]Request failed: %s", url, e.getMessage()));
                            }

                        }else {
                            response.close();
                            Objects.requireNonNull(response.body()).close();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        log.error(String.format("Failed to connect to Assist: %s, %s", e.getCause().getMessage(), e.getMessage()));
                        log.info("Please check the media configuration and confirm that Assist is started...");
                    }
                });
            }



        return responseJSON;
    }

    public JSONObject sendPost(MediaServer mediaServerItem, String url,
                               JSONObject param, ZLMRESTfulUtils.RequestCallback callback,
                               Integer readTimeOut) {
        OkHttpClient client = getClient(readTimeOut);

        if (mediaServerItem == null) {
            return null;
        }
        log.info("[visitassist]： {}, parameters： {}", url, param);
        JSONObject responseJSON = new JSONObject();
        //-2Custom streaming media call error code
        responseJSON.put("code",-2);
        responseJSON.put("msg","ASSISTcall failed");

        RequestBody requestBodyJson = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), param.toString());

        Request request = new Request.Builder()
                .post(requestBodyJson)
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();
        if (callback == null) {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String responseStr = responseBody.string();
                        responseJSON = JSON.parseObject(responseStr);
                    }
                }else {
                    response.close();
                    Objects.requireNonNull(response.body()).close();
                }
            }catch (IOException e) {
                log.error(String.format("[ %s ]ASSISTRequest failed: %s", url, e.getMessage()));

                if(e instanceof SocketTimeoutException){
                    //Read timeout exception
                    log.error(String.format("Failed to read ASSIST data: %s, %s", url, e.getMessage()));
                }
                if(e instanceof ConnectException){
                    //It is determined that the connection is abnormal. Here is the report.Failed to connect to 10.7.5.144
                    log.error(String.format("Failed to connect to ASSIST: %s, %s", url, e.getMessage()));
                }

            }catch (Exception e){
                log.error(String.format("Access to ASSIST failed: %s, %s", url, e.getMessage()));
            }
        }else {
            client.newCall(request).enqueue(new Callback(){

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response){
                    if (response.isSuccessful()) {
                        try {
                            String responseStr = Objects.requireNonNull(response.body()).string();
                            callback.run(responseStr);
                        } catch (IOException e) {
                            log.error(String.format("[ %s ]Request failed: %s", url, e.getMessage()));
                        }

                    }else {
                        response.close();
                        Objects.requireNonNull(response.body()).close();
                    }
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    log.error(String.format("Failed to connect to ZLM: %s, %s", call.request().toString(), e.getMessage()));

                    if(e instanceof SocketTimeoutException){
                        //Read timeout exception
                        log.error(String.format("Failed to read ZLM data: %s, %s", call.request().toString(), e.getMessage()));
                    }
                    if(e instanceof ConnectException){
                        //It is determined that the connection is abnormal. Here is the report.Failed to connect to 10.7.5.144
                        log.error(String.format("Failed to connect to ZLM: %s, %s", call.request().toString(), e.getMessage()));
                    }
                }
            });
        }



        return responseJSON;
    }

    public JSONObject getInfo(MediaServer mediaServerItem, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        return sendGet(mediaServerItem, "api/record/info",param, callback);
    }

    public JSONObject addTask(MediaServer mediaServerItem, String app, String stream, String startTime,
                              String endTime, String callId, List<String> filePathList, String remoteHost) {

        JSONObject videoTaskInfoJSON = new JSONObject();
        videoTaskInfoJSON.put("app", app);
        videoTaskInfoJSON.put("stream", stream);
        videoTaskInfoJSON.put("startTime", startTime);
        videoTaskInfoJSON.put("endTime", endTime);
        videoTaskInfoJSON.put("callId", callId);
        videoTaskInfoJSON.put("filePathList", filePathList);
        if (!ObjectUtils.isEmpty(remoteHost)) {
            videoTaskInfoJSON.put("remoteHost", remoteHost);
        }
        String urlStr = String.format("%s/api/record/file/download/task/add",  remoteHost);;
        return sendPost(mediaServerItem, urlStr, videoTaskInfoJSON, null, 30);
    }

    public JSONObject queryTaskList(MediaServer mediaServerItem, String app, String stream, String callId,
                                    String taskId, Boolean isEnd, String scheme) {
        Map<String, Object> param = new HashMap<>();
        if (!ObjectUtils.isEmpty(app)) {
            param.put("app", app);
        }
        if (!ObjectUtils.isEmpty(stream)) {
            param.put("stream", stream);
        }
        if (!ObjectUtils.isEmpty(callId)) {
            param.put("callId", callId);
        }
        if (!ObjectUtils.isEmpty(taskId)) {
            param.put("taskId", taskId);
        }
        if (!ObjectUtils.isEmpty(isEnd)) {
            param.put("isEnd", isEnd);
        }
        String urlStr = String.format("%s://%s:%s/api/record/file/download/task/list",
                scheme, mediaServerItem.getIp(), mediaServerItem.getRecordAssistPort());;
        return sendGet(mediaServerItem, urlStr, param, null);
    }
}
