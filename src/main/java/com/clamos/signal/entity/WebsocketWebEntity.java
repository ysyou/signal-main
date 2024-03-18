package com.clamos.signal.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Id;

@Data
@RedisHash(value = "webStatus")
public class WebsocketWebEntity {

    @Id
    private String id;
    private String status;
    private Long ttl;

    private String ip;

    @Builder
    public WebsocketWebEntity(String id, String status, Long ttl,String ip) {
        this.id = id;
        this.status = status;
        this.ttl = ttl;
        this.ip = ip;
    }
}
