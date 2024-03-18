package com.clamos.signal.service;

import com.clamos.signal.constant.Command;
import com.clamos.signal.constant.Constants;
import com.clamos.signal.vo.MediaStatusVO;
import com.clamos.signal.handler.WebSocketWebHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
public class RedisMediaSubService implements MessageListener {

    private final WebSocketWebHandler webSocketWebHandler;
    private final ObjectMapper objectMapper;
    private final CommonService cs;
    private final ManagerService managerService;
    private final WebSocketDeviceService websocketDeviceService;

    public RedisMediaSubService(@Lazy WebSocketWebHandler webSocketWebHandler, ObjectMapper objectMapper, CommonService cs, ManagerService managerService,@Lazy WebSocketDeviceService websocketDeviceService) {
        this.webSocketWebHandler = webSocketWebHandler;
        this.objectMapper = objectMapper;
        this.cs = cs;
        this.managerService = managerService;
        this.websocketDeviceService = websocketDeviceService;
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            Map map = objectMapper.readValue(payload, Map.class);
            switch (channel) {
                case Command.MEDIA_STATUS:
                    //1 mediaStatus 보내주고
                    log.info("MediaSub mediaStatus payload: {}", payload);
                    MediaStatusVO mediaStatusVO = MediaStatusVO.builder().id((String) map.get("phone_number")).tag(cs.getUUID()).status(Objects.equals("connect",map.get("status")) ? Constants.TRUE :Constants.FALSE).cmd("mediaStatus").build();

                    List groupIds = websocketDeviceService.getGroupIds(mediaStatusVO.getId());
                    webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(mediaStatusVO),groupIds);

                    //2 events 테이블에 해당 값이 존재하면 eventStart 규격 보내주기 -> 수량문제로 일단 Event_start 시 바로 웹한테 알려줌 , 일단 주석
//                    if (StringUtils.hasText((String) map.get("eventId")) && "connect".equals(map.get("status"))) {
//                        Long eventId = Long.parseLong((String)map.get("eventId"));
//
//                        ResultDTO<Map<String, Object>> eventObject = managerService.getEventById(eventId);
//                        EventEntity eventEntity = objectMapper.readValue(objectMapper.writeValueAsString(eventObject.getData()), EventEntity.class);
//                        if (!ObjectUtils.isEmpty(eventEntity)) {
//                            Map resEventStartMap = ImmutableMap.builder()
//                                    .put("cmd", Command.EVENT_START)
//                                    .put("tag",cs.getUUID())
//                                    .put("id",map.get("phone_number"))
//                                    .put("data",eventEntity)
//                                    .build();
//                            try {
//                                webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(resEventStartMap),groupIds);
//                            } catch (JsonProcessingException e) {
//                                log.error("sendTogroup Error :",e);
//                            }
//                        }
//                    }
                    break;
                default:
                    log.info("MediaSub 규격없음 payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("RedisMediaSubService RedisMediaSubService Error :", e);
        }
    }
}
