package com.genersoft.iot.vmp.web.custom.conf;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Custom request wrapper for caching request body content
 * Solve the problem that the stream can only be read once
 */
@Slf4j
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    
    private byte[] cachedBody;
    private String cachedBodyString;

    public CachedBodyHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBody == null) {
            cacheInputStream();
        }
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (cachedBodyString == null) {
            if (cachedBody == null) {
                cacheInputStream();
            }
            cachedBodyString = new String(cachedBody, StandardCharsets.UTF_8);
        }
        return new BufferedReader(new StringReader(cachedBodyString));
    }

    /**
     * Get the cached request body content
     */
    public String getCachedBody() {
        if (cachedBodyString == null) {
            if (cachedBody == null) {
                try {
                    cacheInputStream();
                } catch (IOException e) {
                    log.warn("Failed to cache request body: {}", e.getMessage());
                    return "";
                }
            }
            if (cachedBody != null) {
                cachedBodyString = new String(cachedBody, StandardCharsets.UTF_8);
            } else {
                cachedBodyString = "";
            }
        }
        return cachedBodyString;
    }

    /**
     * Get the cached request body byte array
     */
    public byte[] getCachedBodyBytes() {
        if (cachedBody == null) {
            try {
                cacheInputStream();
            } catch (IOException e) {
                log.warn("Failed to cache request body: {}", e.getMessage());
                return new byte[0];
            }
        }
        return cachedBody != null ? cachedBody : new byte[0];
    }

    private void cacheInputStream() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             InputStream inputStream = super.getInputStream()) {
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            cachedBody = baos.toByteArray();
            log.debug("Successfully cached request body, length: {}", cachedBody.length);
        } catch (Exception e) {
            log.error("An exception occurred while caching the request body: ", e);
            cachedBody = new byte[0];
        }
    }

    /**
     * Customize ServletInputStream realize
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] body) {
            // Handling null value situations
            this.inputStream = new ByteArrayInputStream(body != null ? body : new byte[0]);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // No need to implement
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}
