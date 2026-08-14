package com.genersoft.iot.vmp.jt1078.proc;

import com.genersoft.iot.vmp.jt1078.util.Bin;
import lombok.Data;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:22
 * @email qingtaij@163.com
 */
@Data
public class Header {
    // newsID
    String msgId;

    // Message body properties
    Integer msgPro;

    // Terminal mobile phone number
    String phoneNumber;

    // Message body serial number
    Integer sn;

    // Protocol version number
    Short version = -1;


    /**
     * Determine whether it is the 2019 version
     *
     * @return true 2019later version。false 2013
     */
    public boolean is2019Version() {
        return Bin.get(msgPro, 14);
    }

    @Override
    public String toString() {
        return "Header{" +
                "newsID='" + msgId + '\'' +
                ", Message body properties=" + msgPro +
                ", Terminal mobile phone number='" + phoneNumber + '\'' +
                ", Message body serial number=" + sn +
                ", Protocol version number=" + version +
                '}';
    }
}
