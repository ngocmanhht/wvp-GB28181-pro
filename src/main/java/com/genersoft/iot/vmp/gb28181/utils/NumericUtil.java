package com.genersoft.iot.vmp.gb28181.utils;

/**
 * Numeric format judgment and processing
 * @author lawrencehj
 * @date 2021January 27
 */
public class NumericUtil {
    /**
     * Determine whether it is in Double format
     * @param str
     * @return true/false
     */
    public static boolean isDouble(String str) {
        try { 
            Double num2 = Double.valueOf(str);
//            logger.debug(num2 + " is a valid numeric string!");
            return true;
        } catch (Exception e) { 
//            logger.debug(str + " is an invalid numeric string!");
            return false;
        }
    }

    /**
     * Determine whether it is in Double format
     * @param str
     * @return true/false
     */
    public static boolean isInteger(String str) {
        try { 
            int num2 = Integer.valueOf(str); 
//            logger.debug(num2 + " is an integer!");
            return true;
        } catch (Exception e) { 
//            logger.debug(str + " is not an integer!");
            return false;
        }
    }
}
