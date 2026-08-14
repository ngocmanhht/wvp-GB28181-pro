package com.genersoft.iot.vmp.jt1078.util;

/**
 * 32Bit integer binary reading and writing
 */
public class Bin {

    private static final int[] bits = new int[32];

    static {
        bits[0] = 1;
        for (int i = 1; i < bits.length; i++) {
            bits[i] = bits[i - 1] << 1;
        }
    }

    /**
     * Read the i-th bit of n
     *
     * @param n int32
     * @param i Value range0-31
     */
    public static boolean get(int n, int i) {
        return (n & bits[i]) == bits[i];
    }

    /**
     * The missing digits are added from the left0
     */
    public static String strHexPaddingLeft(String data, int length) {
        int dataLength = data.length();
        if (dataLength < length) {
            StringBuilder dataBuilder = new StringBuilder(data);
            for (int i = dataLength; i < length; i++) {
                dataBuilder.insert(0, "0");
            }
            data = dataBuilder.toString();
        }
        return data;
    }
}
