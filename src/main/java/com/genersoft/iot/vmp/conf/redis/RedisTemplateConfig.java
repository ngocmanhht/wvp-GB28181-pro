package com.genersoft.iot.vmp.conf.redis;

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // Serialize using fastJson
        GenericFastJsonRedisSerializer fastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        // valueValues are serialized usingfastJsonRedisSerializer
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);

        // keyThe serialization usesStringRedisSerializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, MobilePosition> getRedisTemplateForMobilePosition(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, MobilePosition> redisTemplate = new RedisTemplate<>();
        // Serialize using fastJson
        GenericFastJsonRedisSerializer fastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        // valueValues are serialized usingfastJsonRedisSerializer
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);

        // keyThe serialization usesStringRedisSerializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Long> redisLongTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serialization using String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Use GenericToStringSerializer，It can convert a Long into a plain text string and store it in Redis
        // Enter this on the Redis command line 'get key' What I see is "123" Instead of binary garbled code
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));

        template.afterPropertiesSet();
        return template;
    }

}
