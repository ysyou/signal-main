package com.clamos.signal.service;

import com.clamos.signal.constant.Constants;
import com.clamos.signal.entity.WebsocketDeviceEntity;
import com.clamos.signal.repository.WebSocketDeviceRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    WebSocketDeviceRepository webSocketDeviceRepository;

    @Autowired
    RedisKeyValueTemplate redisKeyValueTemplate;

    @Autowired
    private CommonService cs;

    // set이 아니기에 키가 존재하면 값을 overwrite 함.
    public void addKey(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("### Redis Set Key Error !!! ::: ", e);
        }
    }

    public String getValue(String key) {
        String value = "";
        try {
            if (redisTemplate.hasKey(key)) {
                value = redisTemplate.opsForValue().get(key);
            }
        } catch (Exception e) {
            log.error("### Redis Get Key Error !!! :::", e);
        }
        return value;
    }

    public Set getPatternValue(String pattern){
        return redisTemplate.keys(pattern);
    }
    public void setSignalStatus(String id,Integer status) throws Exception {
        PartialUpdate update = new PartialUpdate<>(id, WebsocketDeviceEntity.class)
                .set("signalStatus", status);
        redisKeyValueTemplate.update(update);
    }

    public void setTTL(String id) {
        PartialUpdate partialUpdate = new PartialUpdate<WebsocketDeviceEntity>(id, WebsocketDeviceEntity.class).set("ttl",cs.getNowSecond());
        redisKeyValueTemplate.update(partialUpdate);
    }

    public void setGroupIds(String id, List<String> groupIds) {
        PartialUpdate partialUpdate = new PartialUpdate<WebsocketDeviceEntity>(id, WebsocketDeviceEntity.class).set("groupIds",groupIds);
        redisKeyValueTemplate.update(partialUpdate);
    }

}
