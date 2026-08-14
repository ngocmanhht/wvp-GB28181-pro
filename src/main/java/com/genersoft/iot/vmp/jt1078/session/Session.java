package com.genersoft.iot.vmp.jt1078.session;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:54
 * @email qingtaij@163.com
 */
@Slf4j
public class Session {

    public static final AttributeKey<Session> KEY = AttributeKey.newInstance(Session.class.getName());

    // Nettyofchannel
    protected final Channel channel;

    // increment of atomic classID
    private final AtomicInteger serialNo = new AtomicInteger(0);

    // Is the registration successful?
    @Getter
    private boolean registered = false;

    // Device mobile phone number
    @Getter
    private String phoneNumber;

    // Device mobile phone number
    @Setter
    @Getter
    private String authenticationCode;

    // creation time
    @Getter
    private final long creationTime;

    // Protocol version number
    @Getter
    private Integer protocolVersion;

    @Getter
    private Header header;

    protected Session(Channel channel) {
        this.channel = channel;
        this.creationTime = System.currentTimeMillis();
    }

    public void writeObject(Object message) {
        log.info("<<<<<<<<<< cmd{},{}", this, message);
        channel.writeAndFlush(message);
    }

    /**
     * Get the next serial number
     *
     * @return serial number
     */
    public int nextSerialNo() {
        int current;
        int next;
        do {
            current = serialNo.get();
            next = current > 0xffff ? 0 : current;
        } while (!serialNo.compareAndSet(current, next + 1));
        return next;
    }

    /**
     * Registersession
     *
     * @param devId EquipmentID
     */
    public void register(String devId, Integer version, Header header) {
        this.phoneNumber = devId;
        this.registered = true;
        this.protocolVersion = version;
        this.header = header;
        SessionManager.INSTANCE.put(devId, this);
    }

    @Override
    public String toString() {
        return "[" +
                "phoneNumber=" + phoneNumber +
                ", reg=" + registered +
                ", version=" + protocolVersion +
                ",ip=" + channel.remoteAddress() +
                ']';
    }

    public void unregister() {
        channel.close();
        SessionManager.INSTANCE.remove(this.phoneNumber);
    }

    public InetSocketAddress getLoadAddress() {
        return (InetSocketAddress)channel.localAddress();
    }
}
