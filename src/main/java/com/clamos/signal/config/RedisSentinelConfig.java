package com.clamos.signal.config;

import com.clamos.signal.constant.Command;
import com.clamos.signal.service.*;
import com.clamos.signal.service.RedisManagerSubService;
import com.clamos.signal.service.RedisMediaSubService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@EnableAutoConfiguration
@Configuration
@AllArgsConstructor
public class RedisSentinelConfig {

    final RedisWebSubService redisWebSubService;
    final RedisDeviceSubService redisDeviceSubService;
    final RedisMediaSubService redisMediaSubService;
    final RedisManagerSubService redisManagerSubService;

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        /*web*/
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_START_CAM));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_DRAW_PEN));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_STOP_CAM));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.SHARE_CAM));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.SHARE_IMG));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.TOGGLE_CAM));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.PTZ));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_PTZ_STAT));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.LOGIN_FORCE));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_RELOAD));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_AUDIO));
        container.addMessageListener(new MessageListenerAdapter(redisWebSubService), new ChannelTopic(Command.WEB_ORI));

        /*device*/
        container.addMessageListener(new MessageListenerAdapter(redisDeviceSubService), new ChannelTopic(Command.DEVICE_PTZ_STAT));
        /*container.addMessageListener(new MessageListenerAdapter(redisDeviceSubService), new ChannelTopic(Command.DEVICE_STATUS));*/
        container.addMessageListener(new MessageListenerAdapter(redisDeviceSubService), new ChannelTopic(Command.DEVICE_DRAW_PEN));
        container.addMessageListener(new MessageListenerAdapter(redisDeviceSubService), new ChannelTopic(Command.SIGNAL_STATUS));
        container.addMessageListener(new MessageListenerAdapter(redisDeviceSubService), new ChannelTopic(Command.AUDIO));

        /*media*/
        container.addMessageListener(new MessageListenerAdapter(redisMediaSubService), new ChannelTopic(Command.MEDIA_STATUS));

        /*manager*/
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.EVENT_START));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.EVENT_STOP));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.DEVICE_CONFIG));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.EMPOWERMENT_START));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.EMPOWERMENT_STOP));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.ALCHERA));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.REMOTE_PLAY_START));
        container.addMessageListener(new MessageListenerAdapter(redisManagerSubService), new ChannelTopic(Command.REMOTE_PLAY_STOP));
        return container;
    }

    // sentinal 사용
   /* @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration()
                .master("mymaster")
                *//*.sentinel("172.16.50.110", 26379);*//*
                .sentinel(env.getProperty("signal.redis.ip.1"), Integer.valueOf(env.getProperty("signal.redis.port")))
                .sentinel(env.getProperty("signal.redis.ip.2"), Integer.valueOf(env.getProperty("signal.redis.port")));
                *//*.sentinel("192.168.0.142", 26379);*//*
        redisSentinelConfiguration.setPassword("Ksncio!");
        redisSentinelConfiguration.setSentinelPassword("Ksncio!");
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisSentinelConfiguration);
        return lettuceConnectionFactory;
    }*/
    // 단독형 설정
    /*@Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
        return lettuceConnectionFactory;
    }*/
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return redisTemplate;
    }
}
