package com.genersoft.iot.vmp.jt1078.util;

/**
 * BCDcode conversion
 */
public class BCDUtil {

    public static String transform(byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }
        // BCD
        StringBuilder stringBuffer = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            // Each time a four-digit value is taken out, a byte is eight bits. The first four bits are taken out, and the second time the four low bits are taken out.，
            // You can also start here & 0xf0Then shift 4 bits to the right, 0xf0 is 11110000 in binary. After AND operation, you can get the result that the upper 4 bits are the value and the lower four bits are cleared.
            stringBuffer.append((byte) ((bytes[i]  >>> 4 & 0xf)));
            stringBuffer.append((byte) (bytes[i] & 0x0f));
        }
        return stringBuffer.toString();
    }

    /**
     * Convert string to BCD code
     * from： https://www.cnblogs.com/ranandrun/p/BCD.html
     * @param asc ASCIIstring
     * @return BCD
     */
    public static byte[] strToBcd(String asc) {
        int len = asc.length();
        int mod = len % 2;
        if (mod != 0) {
            asc = "0" + asc;
            len = asc.length();
        }
        byte abt[] = new byte[len];
        if (len >= 2) {
            len >>= 1;
        }
        byte bbt[] = new byte[len];
        abt = asc.getBytes();
        int j, k;
        for (int p = 0; p < asc.length() / 2; p++) {
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
                j = abt[2 * p] - '0';
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
                j = abt[2 * p] - 'a' + 0x0a;
            } else {
                j = abt[2 * p] - 'A' + 0x0a;
            }
            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
                k = abt[2 * p + 1] - '0';
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
                k = abt[2 * p + 1] - 'a' + 0x0a;
            } else {
                k = abt[2 * p + 1] - 'A' + 0x0a;
            }
            int a = (j << 4) + k;
            byte b = (byte) a;
            bbt[p] = b;
        }
        return bbt;
    }
}
