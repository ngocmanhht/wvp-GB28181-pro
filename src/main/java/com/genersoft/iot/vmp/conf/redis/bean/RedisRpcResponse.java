package com.genersoft.iot.vmp.conf.redis.bean;

/**
 * Send reply via redis
 */
public class RedisRpcResponse {

    /**
     * fromWVP ID
     */
    private String fromId;


    /**
     * TargetedWVP ID
     */
    private String toId;


    /**
     * serial number
     */
    private long sn;

    /**
     * status code
     */
    private int statusCode;

    /**
     * access path
     */
    private String uri;

    /**
     * parameters
     */
    private Object body;

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public long getSn() {
        return sn;
    }

    public void setSn(long sn) {
        this.sn = sn;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "RedisRpcResponse{" +
                "uri='" + uri + '\'' +
                ", fromId='" + fromId + '\'' +
                ", toId='" + toId + '\'' +
                ", sn=" + sn +
                ", statusCode=" + statusCode +
                ", body=" + body +
                '}';
    }
}
