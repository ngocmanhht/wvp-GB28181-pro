package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.factory.CodecFactory;
import com.genersoft.iot.vmp.jt1078.proc.request.Re;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:10
 * @email qingtaij@163.com
 */
@Slf4j
public class Jt808Decoder extends ByteToMessageDecoder {

    private ApplicationEventPublisher applicationEventPublisher = null;
    private Ijt1078Service service = null;

    public Jt808Decoder(ApplicationEventPublisher applicationEventPublisher, Ijt1078Service service ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.service = service;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();
        log.info("> {} hex: 7e{}7e", session, ByteBufUtil.hexDump(in));
        try {
            // Perform checksum escaping according to the part definition
            ByteBuf buf = unEscapeAndCheck(in);
            buf.retain();
            Header header = new Header();
            header.setMsgId(ByteBufUtil.hexDump(buf.readSlice(2)));
            header.setMsgPro(buf.readUnsignedShort());
            // Read whether subcontracting exists from message properties
            boolean isSubpackage = (header.getMsgPro() >>> 13 & 1) == 1;
            if (header.is2019Version()) {
                header.setVersion(buf.readUnsignedByte());
                String devId = ByteBufUtil.hexDump(buf.readSlice(10));
                header.setPhoneNumber(devId.replaceFirst("^0*", ""));
            } else {
                header.setPhoneNumber(ByteBufUtil.hexDump(buf.readSlice(6)).replaceFirst("^0*", ""));
            }
            header.setSn(buf.readUnsignedShort());
            if (isSubpackage) {
                int packageCount = buf.readUnsignedShort();
                int packageNumber = buf.readUnsignedShort();
                log.debug("[subcontract message] header: {}, serial number: {}, total: {}", header, packageNumber, packageCount);
                // Caching subpackaged messages with merging
                ByteBuf intactBuf = MultiPacketManager.INSTANCE.add(header, packageCount, buf);
                if (intactBuf == null) {
                    return;
                }
                buf = intactBuf;
            }
            Re handler = CodecFactory.getHandler(header.getMsgId());
            if (handler == null) {
                log.error("get msgId is null {}", header.getMsgId());
                buf.release();
                return;
            }

            Rs decode = handler.decode(buf, header, session, service);
            ApplicationEvent applicationEvent = handler.getEvent();
            if (applicationEvent != null) {
                applicationEventPublisher.publishEvent(applicationEvent);
            }
            if (decode != null) {
                out.add(decode);
            }
        } finally {
            in.skipBytes(in.readableBytes());
        }
    }




    /**
     * Escape and verify check codes
     *
     * @param byteBuf escapeBuf
     * @return escaped data
     */
    public ByteBuf unEscapeAndCheck(ByteBuf byteBuf) throws Exception {
        int low = byteBuf.readerIndex();
        int high = byteBuf.writerIndex();
        byte checkSum = 0;
        int calculationCheckSum = 0;

        byte aByte = byteBuf.getByte(high - 2);
        byte protocolEscapeFlag7d = 0x7d;
        //0x7descape
        byte protocolEscapeFlag01 = 0x01;
        //0x7eescape
        byte protocolEscapeFlag02 = 0x02;
        if (aByte == protocolEscapeFlag7d) {
            byte b2 = byteBuf.getByte(high - 1);
            if (b2 == protocolEscapeFlag01) {
                checkSum = protocolEscapeFlag7d;
            } else if (b2 == protocolEscapeFlag02) {
                checkSum = 0x7e;
            } else {
                log.error("Escape 1 exception:{}", ByteBufUtil.hexDump(byteBuf));
                throw new Exception("Escape error");
            }
            high = high - 2;
        } else {
            high = high - 1;
            checkSum = byteBuf.getByte(high);
        }
        List<ByteBuf> bufList = new ArrayList<>();
        int index = low;
        while (index < high) {
            byte b = byteBuf.getByte(index);
            if (b == protocolEscapeFlag7d) {
                byte c = byteBuf.getByte(index + 1);
                if (c == protocolEscapeFlag01) {
                    ByteBuf slice = slice0x01(byteBuf, low, index);
                    bufList.add(slice);
                    b = protocolEscapeFlag7d;
                } else if (c == protocolEscapeFlag02) {
                    ByteBuf slice = slice0x02(byteBuf, low, index);
                    bufList.add(slice);
                    b = 0x7e;
                } else {
                    log.error("Escape2Exception:{}", ByteBufUtil.hexDump(byteBuf));
                    throw new Exception("Escape error");
                }
                index += 2;
                low = index;
            } else {
                index += 1;
            }
            calculationCheckSum = calculationCheckSum ^ b;
        }

        if (calculationCheckSum == checkSum) {
            if (bufList.isEmpty()) {
                return byteBuf.slice(low, high);
            } else {
                bufList.add(byteBuf.slice(low, high - low));
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, bufList.size(), bufList);
            }
        } else {
            log.info("{} Parse check code:{}--Calculate check code:{}", ByteBufUtil.hexDump(byteBuf), checkSum, calculationCheckSum);
            throw new Exception("Check code error!");
        }
    }


    private ByteBuf slice0x01(ByteBuf buf, int low, int sign) {
        return buf.slice(low, sign - low + 1);
    }

    private ByteBuf slice0x02(ByteBuf buf, int low, int sign) {
        buf.setByte(sign, 0x7e);
        return buf.slice(low, sign - low + 1);
    }
}
