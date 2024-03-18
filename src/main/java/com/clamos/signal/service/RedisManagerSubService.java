package com.clamos.signal.service;

import com.clamos.signal.constant.Command;
import com.clamos.signal.dto.*;
import com.clamos.signal.handler.WebSocketDeviceHandler;
import com.clamos.signal.handler.WebSocketWebHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RedisManagerSubService implements MessageListener {
    private final ObjectMapper objectMapper;
    private final WebSocketDeviceHandler webSocketDeviceHandler;
    private final WebSocketWebHandler webSocketWebHandler;
    private final RedisService redisService;
    private final CommonService cs;
    private final WebSocketDeviceService websocketDeviceService;

    public RedisManagerSubService(ObjectMapper objectMapper, @Lazy WebSocketDeviceHandler webSocketDeviceHandler, @Lazy WebSocketWebHandler webSocketWebHandler, @Lazy RedisService redisService, CommonService cs,@Lazy WebSocketDeviceService websocketDeviceService) {
        this.objectMapper = objectMapper;
        this.webSocketDeviceHandler = webSocketDeviceHandler;
        this.webSocketWebHandler = webSocketWebHandler;
        this.redisService = redisService;
        this.cs = cs;
        this.websocketDeviceService = websocketDeviceService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

            switch (channel) {
                case Command.EVENT_START:
                    log.info("ManagerSub eventStart payload: {}", payload);
                    EventStartManagerDTO eventStartManagerDto = objectMapper.readValue(payload, EventStartManagerDTO.class);
                    Map eventStartReturnMap = Maps.newHashMap(
                            ImmutableMap.builder()
                                    .put("cmd", eventStartManagerDto.getCmd())
                                    .put("tag", cs.getUUID())
                                    .put("id", eventStartManagerDto.getDevice())
                                    .build());

                    EventStartMangerItemDTO eventStartMangerItemDto = eventStartManagerDto.getData();
                    Map data = Maps.newHashMap(ImmutableMap.builder()
                            .put("eventId", String.valueOf(eventStartMangerItemDto.getEventId()))
                            .put("lv", eventStartMangerItemDto.getLv())
                            .put("caseNum", eventStartMangerItemDto.getCaseNum())
                            .put("evtNo", eventStartMangerItemDto.getEvtNo())
                            .put("evtClCd", eventStartMangerItemDto.getEvtClCd())
                            .put("evtClCdNm", eventStartMangerItemDto.getEvtClCdNm())
                            .put("ts", eventStartMangerItemDto.getTs())
                            .build());

                    eventStartReturnMap.put("data", data);
                    webSocketDeviceHandler.sendToEvent(eventStartReturnMap);

                    //playable이 1이면 web한테 전송
//                    String key = new StringBuilder().append(Constants.PHONE_TB).append(eventStartManagerDto.getDevice()).toString();
//                    String value = redisService.getValue(key);
//                    if (StringUtils.hasText(value)) {
//                        Map map = objectMapper.readValue(redisService.getValue(key), Map.class);
//                        if (StringUtils.hasText((String) map.get("m_server_ip")) && map.get("playable").equals("1")) {
                            List groupIds =  websocketDeviceService.getGroupIds(eventStartManagerDto.getDevice());
                            webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(eventStartReturnMap),groupIds);
//                        }
//                    }
                    break;
                case Command.EVENT_STOP:
                    log.info("ManagerSub eventStop payload: {}", payload);
                    EventStopMangerDTO eventStopMangerDto = objectMapper.readValue(payload, EventStopMangerDTO.class);
                    if (eventStopMangerDto.getDevices().size() > 0) {
                        for (String device : eventStopMangerDto.getDevices()) {
                            Map eventStopReturnMap = ImmutableMap.builder()
                                    .put("cmd", eventStopMangerDto.getCmd())
                                    .put("tag", cs.getUUID())
                                    .put("id", device)
                                    .put("caseNum", eventStopMangerDto.getData().getCaseNum())
                                    .build();
                            webSocketDeviceHandler.sendToEvent(eventStopReturnMap);
                        }
                    }
                    Map eventStopReturnMap = ImmutableMap.builder()
                            .put("cmd", eventStopMangerDto.getCmd())
                            .put("tag", cs.getUUID())
                            .put("ids", eventStopMangerDto.getDevices())
                            .put("data", eventStopMangerDto.getData())
                            .build();

                    webSocketWebHandler.sendToAll(objectMapper.writeValueAsString(eventStopReturnMap));
                    break;
                case Command.DEVICE_CONFIG:
                    log.info("ManagerSub config payload: {}", payload);
                    Map configMap = objectMapper.readValue(payload, Map.class);
                    List<String> ids = webSocketDeviceHandler.getSessionList();
                    for (String id : ids) {
                        if (!ObjectUtils.isEmpty(configMap.get(id))) {
                            Map configRetrunMap = ImmutableMap.builder()
                                    .put("cmd", "config")
                                    .put("tag", cs.getUUID())
                                    .put("id", id)
                                    .putAll((Map) configMap.get(id))
                                    .build();
                            webSocketDeviceHandler.sendToEvent(configRetrunMap);
                        }
                    }
                    break;
                case Command.EMPOWERMENT_START:
                case Command.EMPOWERMENT_STOP:
                    log.info("ManagerSub empowerment payload: {}", payload);
                    EmpowermentDTO empowermentDTO = objectMapper.readValue(payload, EmpowermentDTO.class);

                    webSocketDeviceHandler.updateSessionGroupIds(empowermentDTO.getId());

                    Map<String, Object> empowermentReturnMap = Maps.newLinkedHashMap();
                    empowermentReturnMap.put("cmd", channel);
                    empowermentReturnMap.put("tag", cs.getUUID());
                    empowermentReturnMap.put("id", empowermentDTO.getId());
                    empowermentReturnMap.put("groupId", empowermentDTO.getGroupId());
                    empowermentReturnMap.put("groupIds", empowermentDTO.getGroupIds());


//                    List empowermentGroupIds = webSocketDeviceHandler.getGroupIds(empowermentDTO.getId());
                    List empowermentGroupIds = websocketDeviceService.getGroupIds(empowermentDTO.getId());
                    webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(empowermentReturnMap),empowermentGroupIds);
                    break;
                case Command.ALCHERA:
                    log.info("ManagerSub alchera payload: {}", payload);
                    AlcheraDTO alcheraDTO = objectMapper.readValue(payload, AlcheraDTO.class);

                    Map<String, Object> alcheraReturnMap = Maps.newLinkedHashMap();
                    alcheraReturnMap.put("cmd", channel);
                    alcheraReturnMap.put("tag", cs.getUUID());
                    alcheraReturnMap.put("data", alcheraDTO);


                    List alcheraGroupIds = websocketDeviceService.getGroupIds(alcheraDTO.getDeviceId());
                    webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(alcheraReturnMap),alcheraGroupIds);
                    break;
                case Command.REMOTE_PLAY_START:
                case Command.REMOTE_PLAY_STOP:
                    log.info("ManagerSub remotePlay payload: {}", payload);
                    RemotePlayDTO remotePlayDTO = objectMapper.readValue(payload, RemotePlayDTO.class);

                    Map<String, Object> remoteReturnMap = Maps.newLinkedHashMap();
                    remoteReturnMap.put("cmd", channel);
                    remoteReturnMap.put("tag", cs.getUUID());
                    remoteReturnMap.put("id", remotePlayDTO.getId());
                    remoteReturnMap.put("device",remotePlayDTO.getDevice());
                    remoteReturnMap.put("userId",remotePlayDTO.getUserId());
                    remoteReturnMap.put("userName",remotePlayDTO.getUserName());

                    webSocketWebHandler.sendToEvent(remoteReturnMap);
                    break;
                default:
                    log.info("ManagerSub 규격없음 payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("RedisManagerSubService onMessage Error : ", e);
        }
    }
}
