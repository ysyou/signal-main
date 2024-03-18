package com.clamos.signal.repository;

import com.clamos.signal.entity.WebsocketDeviceEntity;
import org.springframework.data.repository.CrudRepository;

public interface WebSocketDeviceRepository extends CrudRepository<WebsocketDeviceEntity, String> {
}
