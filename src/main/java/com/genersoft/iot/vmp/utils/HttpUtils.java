package com.genersoft.iot.vmp.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public class HttpUtils {

    public static boolean downLoadFile(String url, ZipOutputStream zos) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Download failed, HTTP status code: {}, URL: {}", response.code(), url);
                return false;
            }

            // Get the input stream of the response body
            InputStream inputStream = null;
            if (response.body() != null) {
                inputStream = response.body().byteStream();
            }
            if (inputStream == null) {
                log.error("The response body is empty and the file cannot be downloaded.: {}", url);
                return false;
            }

            // Write input stream to zip file
            byte[] buffer = new byte[8192]; // 8KB buffer to improve performance
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            log.debug("File downloaded successfully: {}, size: {} bytes", url, response.body().contentLength());
            return true;
        } catch (IOException e) {
            log.error("Error occurred during download: {}, URL: {}", e.getMessage(), url);
            return false;
        }
    }
}
