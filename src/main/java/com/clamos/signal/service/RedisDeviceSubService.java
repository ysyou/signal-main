package com.clamos.signal.service;

import com.clamos.signal.constant.Command;
import com.clamos.signal.handler.WebSocketWebHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class RedisDeviceSubService implements MessageListener {
    private final WebSocketWebHandler webSocketWebHandler;
    private final WebSocketDeviceService websocketDeviceService;
    private final CommonService cs;

    private final ObjectMapper objectMapper;

    public RedisDeviceSubService(@Lazy WebSocketWebHandler webSocketWebHandler , @Lazy WebSocketDeviceService websocketDeviceService, CommonService cs, ObjectMapper objectMapper) {
        this.webSocketWebHandler = webSocketWebHandler;
        this.websocketDeviceService = websocketDeviceService;
        this.cs = cs;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

            Map map = new ObjectMapper().readValue(payload, Map.class);
            switch (channel) {
                case Command.SIGNAL_STATUS:
                    log.info("DeviceSub signalStatus payload: {}", payload);
                    if (!ObjectUtils.isEmpty(map.get("id"))) {
                        List<String> groupIds = (List<String>) map.get("groupIds");
                        map.remove("groupIds");
                        webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(map),groupIds);
                    }
                    break;
                case Command.DEVICE_DRAW_PEN:
                    log.info("DeviceSub drawPen payload: {}", payload);
                    Map resDrawPenMap = new HashMap();
                    if("add".equals(map.get("type"))){
                        resDrawPenMap.put("cmd",map.get("cmd"));
                        resDrawPenMap.put("tag",cs.getUUID());
                        resDrawPenMap.put("id",map.get("source"));
                        resDrawPenMap.put("source",map.get("id"));
                        resDrawPenMap.put("type",map.get("type"));
                        resDrawPenMap.put("data",map.get("data"));
                    }else{
                        resDrawPenMap.put("cmd",map.get("cmd"));
                        resDrawPenMap.put("tag",cs.getUUID());
                        resDrawPenMap.put("id",map.get("source"));
                        resDrawPenMap.put("source",map.get("id"));
                        resDrawPenMap.put("type",map.get("type"));
                    }
                    if (!ObjectUtils.isEmpty(map.get("id"))) {
                        List groupIds = websocketDeviceService.getGroupIds((String) map.get("id"));
                        webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(resDrawPenMap),groupIds);
                    }
                    break;
                case Command.DEVICE_PTZ_STAT:
                    log.info("DeviceSub ptzStat payload: {}", payload);
                    Map resPtzStatMap = ImmutableMap.builder()
                            .put("cmd",map.get("cmd"))
                            .put("tag",map.get("tag"))
                            .put("id",map.get("source"))
                            .put("device",map.get("id"))
                            .put("pan",map.get("pan"))
                            .put("tilt",map.get("tilt"))
                            .put("zoom",map.get("zoom"))
                            .build();
                    webSocketWebHandler.sendToEvent(resPtzStatMap);
                    break;
                case Command.AUDIO:
                    if (!ObjectUtils.isEmpty(map.get("id"))) {
                        Object groupIds = map.remove("groupIds");
                        webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(map), (List) groupIds);
                    }
                    break;
                default:
                    log.info("DeviceSub 규격없음 payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("onMessage error :", e);
        }
    }
}
