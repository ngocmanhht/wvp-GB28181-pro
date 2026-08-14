package com.genersoft.iot.vmp.conf.redis;


import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.service.redisMsg.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


/**
 * @description:RedisMiddleware configuration class, usespring-data-redisIntegrated, automatically loads redis configuration from application.yml
 * @author: swwheihei
 * @date: 2019May 30, morning10:58:25
 *
 */
@Configuration
@Order(value=1)
public class RedisMsgListenConfig {

	@Autowired
	private RedisGpsMsgListener redisGPSMsgListener;

	@Autowired
	private RedisAlarmMsgListener redisAlarmMsgListener;

	@Autowired
	private RedisPushStreamStatusMsgListener redisPushStreamStatusMsgListener;

	@Autowired
	private RedisPushStreamListMsgListener pushStreamListMsgListener;

	@Autowired
	private RedisGroupMsgListener groupMsgListener;

	@Autowired
	private RedisGroupChangeListener groupChangeListener;

	@Autowired
	private RedisCloseStreamMsgListener redisCloseStreamMsgListener;

	@Autowired
	private RedisRpcConfig redisRpcConfig;

	@Autowired
	private RedisPushStreamResponseListener redisPushStreamCloseResponseListener;


	/**
	 * redisThe message listener container can add multiple redis listeners that listen to different topics. You only need to bind the message listener to the corresponding message subscription processor. The message listener
	 * Call the relevant methods of the message subscription processor through reflection technology to perform some business processing
	 *
	 * @param connectionFactory
	 * @return
	 */
	@Bean
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
		container.addMessageListener(redisGPSMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_GPS));
		container.addMessageListener(redisAlarmMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM_RECEIVE));
		container.addMessageListener(redisPushStreamStatusMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_PUSH_STREAM_STATUS_CHANGE));
		container.addMessageListener(pushStreamListMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_PUSH_STREAM_LIST_CHANGE));
		container.addMessageListener(redisCloseStreamMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE));
		container.addMessageListener(redisRpcConfig, new PatternTopic(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY));
		container.addMessageListener(redisPushStreamCloseResponseListener, new PatternTopic(VideoManagerConstants.VM_MSG_STREAM_PUSH_RESPONSE));
		container.addMessageListener(groupMsgListener, new PatternTopic(VideoManagerConstants.VM_MSG_GROUP_LIST_RESPONSE));
		container.addMessageListener(groupChangeListener, new PatternTopic(VideoManagerConstants.VM_MSG_GROUP_LIST_CHANGE));
        return container;
    }
}
