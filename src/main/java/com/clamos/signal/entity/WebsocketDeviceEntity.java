package com.clamos.signal.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Id;
import java.util.List;

@Data
@RedisHash(value = "deviceStatus")
public class WebsocketDeviceEntity {
    @Id
    private String id;
    private Integer signalStatus;
    private Integer mediaStatus;
    private List groupIds;
    private Long ttl;

    private String ip;

    @Builder
    public WebsocketDeviceEntity(String id, Integer signalStatus, Integer mediaStatus, Long ttl, List groupIds, String ip) {
        this.id = id;
        this.signalStatus = signalStatus;
        this.mediaStatus = mediaStatus;
        this.ttl = ttl;
        this.groupIds = groupIds;
        this.ip = ip;
    }
}
