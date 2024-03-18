package com.clamos.signal.service;

import com.clamos.signal.constant.Command;
import com.clamos.signal.handler.WebSocketDeviceHandler;
import com.clamos.signal.handler.WebSocketWebHandler;
import com.clamos.signal.repository.WebSocketDeviceRepository;
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
public class RedisWebSubService implements MessageListener {

    private final CommonService cs;
    private final ObjectMapper objectMapper;
    private final WebSocketDeviceHandler webSocketDeviceHandler;
    private final WebSocketWebHandler webSocketWebHandler;
    final WebSocketDeviceRepository websocketDeviceRepository;
    final WebSocketDeviceService websocketDeviceService;
    public RedisWebSubService(CommonService cs, ObjectMapper objectMapper, @Lazy WebSocketDeviceHandler webSocketDeviceHandler, @Lazy WebSocketWebHandler webSocketWebHandler, @Lazy WebSocketDeviceRepository websocketDeviceRepository, @Lazy WebSocketDeviceService websocketDeviceService) {
        this.cs = cs;
        this.objectMapper = objectMapper;
        this.webSocketDeviceHandler = webSocketDeviceHandler;
        this.webSocketWebHandler = webSocketWebHandler;
        this.websocketDeviceRepository = websocketDeviceRepository;
        this.websocketDeviceService = websocketDeviceService;
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);


            Map map = objectMapper.readValue(payload, Map.class);
            switch (channel) {
                case Command.LOGIN_FORCE:
                    log.info("WebSub force payload: {}", payload);
                    if(!map.isEmpty()){
                        webSocketWebHandler.endSessionById(map);
                        webSocketWebHandler.forceLogin(map);
                    }
                    break;
                case Command.WEB_RELOAD:
                    log.info("WebSub reload payload: {}", payload);
                    List ids = (List) map.get("source");
                    for (Object id : ids) {
                        Map reloadMap = ImmutableMap.builder()
                                .put("cmd","reload")
                                .put("tag",cs.getUUID())
                                .put("id",id)
                                .build();
                        webSocketWebHandler.sendToEvent(reloadMap);
                    }
                    break;
                case Command.WEB_START_CAM:
                    log.info("WebSub startCam payload: {}", payload);
                    List startDeviceList = (ArrayList) map.get("devices");
                    for (Object device : startDeviceList) {
                        Map resStartMap = ImmutableMap.builder()
                                .put("cmd", map.get("cmd"))
                                .put("tag", cs.getUUID())
                                .put("id", device)
                                .build();
                        webSocketDeviceHandler.sendToEvent(resStartMap);
                    }
                    break;
                case Command.WEB_STOP_CAM:
                    log.info("WebSub webStopCam payload: {}", payload);
                    List stopDeviceList = (ArrayList) map.get("devices");
                    for (Object device : stopDeviceList) {
                        Map resStopMap = ImmutableMap.builder()
                                .put("cmd", map.get("cmd"))
                                .put("tag", cs.getUUID())
                                .put("id", device)
                                .build();
                        webSocketDeviceHandler.sendToEvent(resStopMap);
                    }
                    break;
                case Command.SHARE_CAM:
                    log.info("WebSub shareCam payload: {}", payload);
                    List shareCamList = (ArrayList) map.get("devices");

                    for (Object device : shareCamList) {
                        Map resShareCamMap = ImmutableMap.builder()
                                .put("cmd",map.get("cmd"))
                                .put("tag",cs.getUUID())
                                .put("id", device)
                                .put("source",map.get("source"))
                                .build();
                        webSocketDeviceHandler.sendToEvent(resShareCamMap);
                    }
                    break;
                case Command.SHARE_IMG:
                    log.info("WebSub shareImg payload: {}", payload);
                    List shareImgDeviceList = (ArrayList) map.get("devices");
                    for (Object device : shareImgDeviceList) {
                        Map resShareImgMap = ImmutableMap.builder()
                                .put("cmd",map.get("cmd"))
                                .put("tag",cs.getUUID())
                                .put("id",device)
                                .put("url",map.get("url"))
                                .build();
                        webSocketDeviceHandler.sendToEvent(resShareImgMap);
                    }
                    break;
                case Command.WEB_ORI:
                    log.info("WebSub ori payload: {}", payload);
                    Map resOriMap = new HashMap();
                    resOriMap.put("cmd", map.get("cmd"));
                    resOriMap.put("tag", map.get("tag"));
                    resOriMap.put("id", map.get("device"));
                    resOriMap.put("source", map.get("id"));
                    resOriMap.put("dir", map.get("dir"));
                    webSocketDeviceHandler.sendToEvent(resOriMap);
                    break;
                case Command.TOGGLE_CAM:
                    log.info("WebSub toggleCam payload: {}", payload);
                    Map resToggleMap = ImmutableMap.builder()
                            .put("cmd", map.get("cmd"))
                            .put("tag", cs.getUUID())
                            .put("id", map.get("device"))
                            .put("type",map.get("type"))
                            .build();
                    webSocketDeviceHandler.sendToEvent(resToggleMap);
                    break;
                case Command.WEB_DRAW_PEN:
                    log.info("WebSub drawPen payload: {}", payload);
                    Map resDrawPenMap;
                    if("add".equals(map.get("type"))){
                        resDrawPenMap = ImmutableMap.builder()
                                .put("cmd",map.get("cmd"))
                                .put("tag",cs.getUUID())
                                .put("id",map.get("source"))
                                .put("source",map.get("id"))
                                .put("type",map.get("type"))
                                .put("act",map.get("act"))
                                .put("data",map.get("data"))
                                .build();
                    }else{
                        resDrawPenMap = ImmutableMap.builder()
                                .put("cmd",map.get("cmd"))
                                .put("tag",cs.getUUID())
                                .put("id",map.get("source"))
                                .put("source",map.get("id"))
                                .put("type",map.get("type"))
                                .put("act",map.get("act"))
                                .build();
                    }
                    webSocketDeviceHandler.sendToEvent(resDrawPenMap);
                    if (!ObjectUtils.isEmpty(resDrawPenMap.get("id"))) {
                        List groupIds = websocketDeviceService.getGroupIds((String) resDrawPenMap.get("id"));
                        webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(resDrawPenMap),groupIds);
                    }
                    break;
                case Command.PTZ:
                    log.info("WebSub ptz payload: {}", payload);
                    Map ptzMap = ImmutableMap.builder()
                            .put("cmd",map.get("cmd"))
                            .put("tag",cs.getUUID())
                            .put("id",map.get("device"))
                            .put("pan",map.get("pan"))
                            .put("tilt",map.get("tilt"))
                            .put("zoom",map.get("zoom"))
                            .build();
                    webSocketDeviceHandler.sendToEvent(ptzMap);
                    if (!ObjectUtils.isEmpty(ptzMap.get("id"))) {
                        List groupIds = websocketDeviceService.getGroupIds((String) ptzMap.get("id"));
                        webSocketWebHandler.sendToGroups(objectMapper.writeValueAsString(ptzMap),groupIds);
                    }
                    break;
                case Command.WEB_PTZ_STAT:
                    log.info("WebSub ptzStat payload: {}", payload);
                    Map ptzStatMap = ImmutableMap.builder()
                            .put("cmd",map.get("cmd"))
                            .put("tag",map.get("tag"))
                            .put("id",map.get("device"))
                            .put("source",map.get("id"))
                            .build();
                    webSocketDeviceHandler.sendToEvent(ptzStatMap);
                    break;
                case Command.WEB_AUDIO:
                    log.info("WebSub audio payload: {}", payload);
                    map.put("cmd", Command.AUDIO);
                    webSocketDeviceHandler.sendToEvent(map);
                    break;
                default:
                    log.info("WebSub 규격없음 payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("onMessage error :", e);
        }
    }
}
