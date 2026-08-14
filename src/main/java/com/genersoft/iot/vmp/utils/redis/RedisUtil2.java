//package com.genersoft.iot.vmp.utils.redis;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.genersoft.iot.vmp.utils.SpringBeanFactory;
//import org.springframework.data.redis.core.*;
//import org.springframework.util.CollectionUtils;
//
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
///**
// * RedisTools
// * @author swwheihei
// * @date 2020May 6, 2018, afternoon8:27:29
// */
//@SuppressWarnings(value = {"rawtypes", "unchecked"})
//public class RedisUtil2 {
//
//    private static RedisTemplate redisTemplate;
//
//    static {
//        redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//    }
//
//	/**
//     * Specify cache expiration time
//     * @param key key
//     * @param time time (seconds）
//     * @return true / false
//     */
//    public static boolean expire(String key, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            if (time > 0) {
//                redisTemplate.expire(key, time, TimeUnit.SECONDS);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Get expiration time based on key
//     * @param key key
//     */
//    public static long getExpire(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
//    }
//
//    /**
//     * Determine whether key exists
//     * @param key key
//     * @return true / false
//     */
//    public static boolean hasKey(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.hasKey(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Delete cache
//     * @SuppressWarnings("unchecked") Ignore type conversion warnings
//     * @param key key (one or more）
//     */
//    public static boolean del(String... key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//    	try {
//    		if (key != null && key.length > 0) {
//                if (key.length == 1) {
//                    redisTemplate.delete(key[0]);
//                } else {
////                    Pass in a Collection<String> collection
//                    redisTemplate.delete(CollectionUtils.arrayToList(key));
//                }
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
////    ============================== String ==============================
//
//    /**
//     * Ordinary cache acquisition
//     * @param key key
//     * @return value
//     */
//    public static Object get(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return key == null ? null : redisTemplate.opsForValue().get(key);
//    }
//
//    /**
//     * Ordinary cache put
//     * @param key key
//     * @param value value
//     * @return true / false
//     */
//    public static boolean set(String key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForValue().set(key, value);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Normal cache put and set time
//     * @param key key
//     * @param value value
//     * @param time time (seconds) if time < 0 then set unlimited time
//     * @return true / false
//     */
//    public static boolean set(String key, Object value, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            if (time > 0) {
//                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
//            } else {
//                set(key, value);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Increment
//     * @param key key
//     * @param delta incremental size
//     * @return
//     */
//    public static long incr(String key, long delta) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        if (delta < 0) {
//            throw new RuntimeException("The increment factor must be greater than 0");
//        }
//        return redisTemplate.opsForValue().increment(key, delta);
//    }
//
//    /**
//     * Decreasing
//     * @param key key
//     * @param delta Decrement size
//     * @return
//     */
//    public static long decr(String key, long delta) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        if (delta < 0) {
//            throw new RuntimeException("The decreasing factor must be greater than 0");
//        }
//        return redisTemplate.opsForValue().increment(key, delta);
//    }
//
////    ============================== Map ==============================
//
//    /**
//     * HashGet
//     * @param key key（no null）
//     * @param item item（no null）
//     * @return value
//     */
//    public static Object hget(String key, String item) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForHash().get(key, item);
//    }
//
//    /**
//     * Get the corresponding key map
//     * @param key key（no null）
//     * @return Corresponding multiple key values
//     */
//    public static Map<Object, Object> hmget(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForHash().entries(key);
//    }
//
//    /**
//     * HashSet
//     * @param key key
//     * @param map value
//     * @return true / false
//     */
//    public static boolean hmset(String key, Map<Object, Object> map) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForHash().putAll(key, map);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * HashSet and set the time
//     * @param key key
//     * @param map value
//     * @param time time
//     * @return true / false
//     */
//    public static boolean hmset(String key, Map<?, ?> map, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForHash().putAll(key, map);
//            if (time > 0) {
//                expire(key, time);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Put data into a Hash table, create it if it does not exist
//     * @param key key
//     * @param item item
//     * @param value value
//     * @return true / false
//     */
//    public static boolean hset(String key, String item, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForHash().put(key, item, value);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Put data into a Hash table and set the time. If it does not exist, create it.
//     * @param key key
//     * @param item item
//     * @param value value
//     * @param time Time (if the original Hash table sets the time, it will be overwritten here）
//     * @return true / false
//     */
//    public static boolean hset(String key, String item, Object value, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForHash().put(key, item, value);
//            if (time > 0) {
//                expire(key, time);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Delete values in Hash table
//     * @param key key
//     * @param item Items (can be multiple，no null）
//     */
//    public static void hdel(String key, Object... item) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        redisTemplate.opsForHash().delete(key, item);
//    }
//
//    /**
//     * Determine whether there is a value for the key in the Hash table
//     * @param key key（no null）
//     * @param item value（no null）
//     * @return true / false
//     */
//    public static boolean hHasKey(String key, String item) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForHash().hasKey(key, item);
//    }
//
//    /**
//     * HashIncrement, if it does not exist, create one and return the new value
//     * @param key key
//     * @param item item
//     * @param by incremental size > 0
//     * @return
//     */
//    public static Double hincr(String key, String item, Double by) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForHash().increment(key, item, by);
//    }
//
//    /**
//     * HashDecreasing
//     * @param key key
//     * @param item item
//     * @param by Decrement size
//     * @return
//     */
//    public static Double hdecr(String key, String item, Double by) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForHash().increment(key, item, -by);
//    }
//
////    ============================== Set ==============================
//
//    /**
//     * Get all values in set based on key
//     * @param key key
//     * @return value
//     */
//    public static Set<Object> sGet(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForSet().members(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    /**
//     * From the set whose key is key, query whether it exists based on value
//     * @param key key
//     * @param value value
//     * @return true / false
//     */
//    public static boolean sHasKey(String key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForSet().isMember(key, value);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Put data into set cache
//     * @param key key value
//     * @param values value (can be multiple）
//     * @return Number of successes
//     */
//    public static long sSet(String key, Object... values) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForSet().add(key, values);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /**
//     * Put the data into the set cache and set the time
//     * @param key key
//     * @param time time
//     * @param values value (can be multiple）
//     * @return Number of successfully placed
//     */
//    public static long sSet(String key, long time, Object... values) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            long count = redisTemplate.opsForSet().add(key, values);
//            if (time > 0) {
//                expire(key, time);
//            }
//            return count;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /**
//     * Get the length of the set cache
//     * @param key key
//     * @return length
//     */
//    public static long sGetSetSize(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForSet().size(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /**
//     * Remove the set cache with value value
//     * @param key key
//     * @param values value
//     * @return Number of successful removals
//     */
//    public static long setRemove(String key, Object... values) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForSet().remove(key, values);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
////    ============================== ZSet ==============================
//
//    /**
//     * Add an element. The biggest difference between zset and set is that each element has a score, so there is an auxiliary function for sorting.;  zadd
//     *
//     * @param key
//     * @param value
//     * @param score
//     */
//    public static void zAdd(Object key, Object value, double score) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        redisTemplate.opsForZSet().add(key, value, score);
//    }
//
//    /**
//     * Delete element zrem
//     *
//     * @param key
//     * @param value
//     */
//    public static void zRemove(Object key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        redisTemplate.opsForZSet().remove(key, value);
//    }
//
//    /**
//     * scoreincrease or decrease zincrby
//     *
//     * @param key
//     * @param value
//     * @param delta -1 means subtracting 1 means adding1
//     */
//    public static Double zIncrScore(Object key, Object value, double delta) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
//    }
//
//    /**
//     * Query the corresponding valuescore   zscore
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public static Double zScore(Object key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().score(key, value);
//    }
//
//    /**
//     * Determine the ranking of value in zset  zrank
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public static Long zRank(Object key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().rank(key, value);
//    }
//
//    /**
//     * Returns the length of the collection
//     *
//     * @param key
//     * @return
//     */
//    public static Long zSize(Object key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().zCard(key);
//    }
//
//    /**
//     * Query the values in a specified sequence in a collection， 0 -1 Indicates getting all collection contents  zrange
//     *
//     * Returns an ordered set, with the smaller score at the front
//     *
//     * @param key
//     * @param start
//     * @param end
//     * @return
//     */
//    public static Set<Object> zRange(Object key, int start, int end) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().range(key, start, end);
//    }
//    /**
//     * Query the sum of values in a specified order in a collectionscore，0, -1 Indicates getting all collection contents
//     *
//     * @param key
//     * @param start
//     * @param end
//     * @return
//     */
//    public static Set<ZSetOperations.TypedTuple<String>> zRangeWithScore(Object key, int start, int end) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
//    }
//    /**
//     * Query the values in a specified sequence in a collection  zrevrange
//     *
//     * Returns an ordered set, with the one with the largest score at the front.
//     *
//     * @param key
//     * @param start
//     * @param end
//     * @return
//     */
//    public static Set<String> zRevRange(Object key, int start, int end) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().reverseRange(key, start, end);
//    }
//    /**
//     * According to the value of score, get the set that meets the conditions  zrangebyscore
//     *
//     * @param key
//     * @param min
//     * @param max
//     * @return
//     */
//    public static Set<String> zSortRange(Object key, int min, int max) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
//    }
//
//
////    ============================== List ==============================
//
//    /**
//     * Get the contents of the list cache
//     * @param key key
//     * @param start start
//     * @param end end (0 to -1 represents all values）
//     * @return
//     */
//    public static List<Object> lGet(String key, long start, long end) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForList().range(key, start, end);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    /**
//     * Get the length of the list cache
//     * @param key key
//     * @return length
//     */
//    public static long lGetListSize(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForList().size(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /**
//     * Get the element in the list with key key according to index index
//     * @param key key
//     * @param index Index
//     *              When index >= 0 time {0:Header, 1: second element}
//     *              When index < 0 time {-1:end of table, -2:penultimate element}
//     * @return value
//     */
//    public static Object lGetIndex(String key, long index) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForList().index(key, index);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    /**
//     * Insert the value value into the list with key key, or create an empty list if it does not exist list
//     * @param key key
//     * @param value value
//     * @return true / false
//     */
//    public static boolean lSet(String key, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForList().rightPush(key, value);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Insert value value into list with key key and set time
//     * @param key key
//     * @param value value
//     * @param time time
//     * @return true / false
//     */
//    public static boolean lSet(String key, Object value, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForList().rightPush(key, value);
//            if (time > 0) {
//                expire(key, time);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Insert values into list with key key
//     * @param key key
//     * @param values value
//     * @return true / false
//     */
//    public static boolean lSetList(String key, List<Object> values) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForList().rightPushAll(key, values);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Insert values into list with key key and set time
//     * @param key key
//     * @param values value
//     * @param time time
//     * @return true / false
//     */
//    public static boolean lSetList(String key, List<Object> values, long time) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForList().rightPushAll(key, values);
//            if (time > 0) {
//                expire(key, time);
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Modify the value of key according to index index
//     * @param key key
//     * @param index Index
//     * @param value value
//     * @return true / false
//     */
//    public static boolean lUpdateIndex(String key, long index, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            redisTemplate.opsForList().set(key, index, value);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Remove the element whose value is value from the list with key key
//     * @param key key
//     * @param count if count == 0 Then delete all elements in list whose value is value
//     *              if count > 0 Then delete the leftmost element in the list whose value is value
//     *              if count < 0 Then delete the rightmost element in the list whose value is value
//     * @param value
//     * @return
//     */
//    public static long lRemove(String key, long count, Object value) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            return redisTemplate.opsForList().remove(key, count, value);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /**
//     * Remove the first element from the list with key key
//     * @param key key
//     * @return
//     */
//    public static Object lLeftPop(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForList().leftPop(key);
//    }
//
//    /**
//     * Remove the last element from the list with key key
//     * @param key key
//     * @return
//     */
//    public static Object lrightPop(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        return redisTemplate.opsForList().rightPop(key);
//    }
//
//    /**
//     * fuzzy query
//     * @param key key
//     * @return true / false
//     */
//    public static List<Object> keys(String key) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        try {
//            Set<String> set = redisTemplate.keys(key);
//            return new ArrayList<>(set);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//
//    /**
//     * fuzzy query
//     * @param query query parameters
//     * @return
//     */
////    public static List<Object> scan(String query) {
////        List<Object> result = new ArrayList<>();
////        try {
////            Cursor<Map.Entry<Object,Object>> cursor = redisTemplate.opsForHash().scan("field",
////                    ScanOptions.scanOptions().match(query).count(1000).build());
////            while (cursor.hasNext()) {
////                Map.Entry<Object,Object> entry = cursor.next();
////                result.add(entry.getKey());
////                Object key = entry.getKey();
////                Object valueSet = entry.getValue();
////            }
////            //Closecursor
////            cursor.close();
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////        return result;
////    }
//
//    /**
//     * fuzzy query
//     * @param query query parameters
//     * @return
//     */
//    public static List<Object> scan(String query) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        Set<String> resultKeys = (Set<String>) redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
//            ScanOptions scanOptions = ScanOptions.scanOptions().match("*" + query + "*").count(1000).build();
//            Cursor<byte[]> scan = connection.scan(scanOptions);
//            Set<String> keys = new HashSet<>();
//            while (scan.hasNext()) {
//                byte[] next = scan.next();
//                keys.add(new String(next));
//            }
//            return keys;
//        });
//
//        return new ArrayList<>(resultKeys);
//    }
//    public static List<Object> scan2(String query) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        Set<String> keys = redisTemplate.keys(query);
//        return new ArrayList<>(keys);
//    }
//    //    ============================== Message sending and subscription ==============================
//    public static void convertAndSend(String channel, JSONObject msg) {
//        if (redisTemplate == null) {
//            redisTemplate = SpringBeanFactory.getBean("redisTemplate");
//        }
//        redisTemplate.convertAndSend(channel, msg);
//    }
//
//}
