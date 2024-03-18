package com.clamos.signal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriberService implements MessageListener {
    private final RedisTemplate redisTemplate;
    private final SimpMessageSendingOperations messagingTemplate;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = (String) redisTemplate.getStringSerializer().deserialize(message.getBody());
            String channel = (String) redisTemplate.getStringSerializer().deserialize(message.getChannel());
            messagingTemplate.convertAndSend("/sub/" + channel, payload);
        } catch (Exception e) {
            log.error("",e);
        }
    }
}