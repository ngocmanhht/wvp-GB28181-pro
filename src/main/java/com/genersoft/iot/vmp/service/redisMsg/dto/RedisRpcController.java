package com.genersoft.iot.vmp.service.redisMsg.dto;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisRpcController {
    /**
     * Request path
     */
    String value() default "";
}
