package com.clamos.signal.repository;

import com.clamos.signal.entity.WebsocketWebEntity;
import org.springframework.data.repository.CrudRepository;

public interface WebSocketWebRpository extends CrudRepository<WebsocketWebEntity, String> {
}
