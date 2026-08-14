package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Set up phone book
 */
@Setter
@Getter
@MsgId(id = "8401")
public class J8401 extends Rs {

    /**
     * Setting type:
     * 0: Delete all contacts stored on the terminal,
     * 1: Indicates updating the phone book $ Delete all contacts in the terminal and append the contacts in the message,
     * 2: Indicates adding a phone book,
     * 3: Indicates modifying the phone book $ with contacts as the index
     */
    private int type;

    /**
     * Contact person
     */
    private List<JTPhoneBookContact> phoneBookContactList;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(type);
        if (phoneBookContactList != null && !phoneBookContactList.isEmpty()) {
            buffer.writeByte(phoneBookContactList.size());
            for (JTPhoneBookContact jtPhoneBookContact : phoneBookContactList) {
                buffer.writeBytes(jtPhoneBookContact.encode());
            }
        }else {
            buffer.writeByte(0);
        }
        return buffer;
    }

}
