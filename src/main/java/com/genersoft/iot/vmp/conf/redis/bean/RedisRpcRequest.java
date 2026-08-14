package com.genersoft.iot.vmp.conf.redis.bean;

/**
 * Send request through redis
 */
public class RedisRpcRequest {

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
     * access path
     */
    private String uri;

    /**
     * parameters
     */
    private Object param;

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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public long getSn() {
        return sn;
    }

    public void setSn(long sn) {
        this.sn = sn;
    }

    @Override
    public String toString() {
        return "RedisRpcRequest{" +
                "uri='" + uri + '\'' +
                ", fromId='" + fromId + '\'' +
                ", toId='" + toId + '\'' +
                ", sn=" + sn +
                ", param=" + param +
                '}';
    }

    public RedisRpcResponse getResponse() {
        RedisRpcResponse response = new RedisRpcResponse();
        response.setFromId(fromId);
        response.setToId(toId);
        response.setSn(sn);
        response.setUri(uri);
        return response;
    }
}
