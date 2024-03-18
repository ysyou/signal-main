package com.clamos.signal.service;

import com.clamos.signal.constant.Constants;
import com.clamos.signal.dto.ResultDTO;
import com.clamos.signal.entity.WebsocketDeviceEntity;
import com.clamos.signal.repository.WebSocketDeviceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class WebSocketDeviceService {
    final WebSocketDeviceRepository websocketDeviceRepository;
    final ManagerService managerService;
    public List getGroupIds(String id){
        Optional<WebsocketDeviceEntity> byId = websocketDeviceRepository.findById(id);
        if (byId.isPresent()) {
            WebsocketDeviceEntity websocketDeviceEntity = byId.get();
            return websocketDeviceEntity.getGroupIds();
        }else{
            log.error("Redis 동시성 조회 문제 발생  -> Fegin 조회 id : {}", id);
            ResultDTO<List<String>> deviceGroupIds = managerService.getDeviceGroupIds(id);
            List<String> groupIds = deviceGroupIds.getData();
            return groupIds;
        }
    }

    public Boolean existsById(String id){
        Optional<WebsocketDeviceEntity> byId = websocketDeviceRepository.findById(id);
        if (byId.isPresent()) {
            WebsocketDeviceEntity websocketDeviceEntity = byId.get();
            if (Objects.equals(websocketDeviceEntity.getSignalStatus(), Constants.TRUE)) {
                return true;
            }else {
                log.info("진짜 signalStatus 가 0 이여서 로그인");
            }
        }else{
            log.info("Redis 조회가 불가능해서 로그인이 False 폰 : {} ", id);
        }
        return false;
    }

}
