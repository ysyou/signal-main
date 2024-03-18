package com.clamos.signal.service;

import com.clamos.signal.constant.Constants;
import com.clamos.signal.dto.*;
import com.clamos.signal.entity.PhoneTBEntity;
import com.clamos.signal.entity.WebsocketDeviceEntity;
import com.clamos.signal.repository.WebSocketDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebHandlerService {
    private final WebSocketDeviceRepository websocketDeviceRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;
    public List<MediaListDTO> mediaList() {
        List<MediaListDTO> list = new ArrayList<>();
        try {
            //signalStatus 데이터 가져오기
            List<WebsocketDeviceEntity> entityList = (List<WebsocketDeviceEntity>) websocketDeviceRepository.findAll();
            for (WebsocketDeviceEntity websocketDeviceEntity : entityList) {
                if (!ObjectUtils.isEmpty(websocketDeviceEntity) && StringUtils.hasText(websocketDeviceEntity.getId())) {
                    MediaListDTO mediaListDTO = new MediaListDTO(websocketDeviceEntity.getId(), websocketDeviceEntity.getSignalStatus(), Constants.FALSE);
                    String value = redisService.getValue(Constants.PHONE_TB + websocketDeviceEntity.getId());
                    if (StringUtils.hasText(value)) {
                        PhoneTBEntity phoneTBEntity = objectMapper.readValue(value, PhoneTBEntity.class);
                        Integer status = Objects.equals("1", phoneTBEntity.getPlayable()) ? Constants.TRUE : Constants.FALSE;
                        mediaListDTO.setM(status);
                    }
                    list.add(mediaListDTO);
                }
            }

            //mediaStatus 데이터 가져오기
            String keyPattern = Constants.PHONE_TB + "*";
            List<String> keys = new ArrayList<>(redisTemplate.keys(keyPattern));
            for (String key : keys) {
                if (list.stream().noneMatch(mediaListDTO -> Objects.equals(mediaListDTO.getId(), key.replaceAll(Constants.PHONE_TB, "")))) {
                    String value = redisService.getValue(key);
                    PhoneTBEntity phoneTBEntity = objectMapper.readValue(value, PhoneTBEntity.class);
                    if (Objects.equals("1",phoneTBEntity.getPlayable())) { //영상이 재생중일떄
                        MediaListDTO mediaListDTO = new MediaListDTO(key.replaceAll(Constants.PHONE_TB, ""), Constants.FALSE, Constants.TRUE);
                        list.add(mediaListDTO);
                    }
                }
            }
        } catch (Exception e) {
            log.error("mediaList Exception : ", e);
        }
        return list;
    }
}
