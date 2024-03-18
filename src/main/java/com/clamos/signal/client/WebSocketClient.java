package com.clamos.signal.client;

import com.clamos.signal.service.CommonService;
import com.clamos.signal.dto.AnswerOutputRtspDTO;
import com.clamos.signal.dto.AnswerInputRtspDTO;
import com.clamos.signal.dto.MediaSessionDTO;
import com.clamos.signal.handler.WebSocketDeviceHandler;
import com.clamos.signal.handler.WebSocketWebHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ClientEndpoint
@Component
public class WebSocketClient {
    private WebSocketContainer container;
    private ConcurrentMap<String, MediaSessionDTO> sessions = new ConcurrentHashMap<>();
    private final WebSocketDeviceHandler webSocketDeviceHandler;
    private final WebSocketWebHandler webSocketWebHandler;
    private final CommonService cs;
    private final ObjectMapper objectMapper;


    public WebSocketClient(@Lazy WebSocketDeviceHandler webSocketDeviceHandler, @Lazy WebSocketWebHandler webSocketWebHandler, CommonService cs, ObjectMapper objectMapper) {
        this.webSocketDeviceHandler = webSocketDeviceHandler;
        this.webSocketWebHandler = webSocketWebHandler;
        this.cs = cs;
        this.objectMapper = objectMapper;
        container = ContainerProvider.getWebSocketContainer();
    }


    public Session connect(String server, String id, String tag, String cmd) {
        Session session = null;
        try {
            if(sessions.containsKey(id)){
                disconnect(sessions.get(id).getSession(),id);
            }
            session = container.connectToServer(this, new URI(server));
            sessions.put(id, MediaSessionDTO.builder().ip(server).session(session).cmd(cmd).tag(tag).build());
            Timer timer = new Timer();
            Session finalSession = session;
            TimerTask task = new TimerTask() {
                @SneakyThrows
                @Override
                public void run() {
                    if (sessions.containsKey(id)) {
                        disconnect(finalSession, id);
                    }
                }
            };
            timer.schedule(task, 10000);
        } catch (Exception e) {
            log.error("[error] client connect", e);
        }
        log.info("client connect : media phone: {}", id);
        return session;
    }

    private void returnDeviceEr(MediaSessionDTO mediaSessionDto, String id) throws IOException {
        if (ObjectUtils.isEmpty(mediaSessionDto.getStatus())) {
            Map map = ImmutableMap.builder()
                    .put("msg", "연결이 이루어지지 않았습니다")
                    .put("code", 1)
                    .put("id", id.split(",")[0])
                    .put("tag", mediaSessionDto.getTag())
                    .put("cmd", mediaSessionDto.getCmd())
                    .build();
            webSocketDeviceHandler.sendToEvent(map);
            webSocketWebHandler.sendToEvent(map);
        }
    }

    public void disconnect(Session session, String id) {
        log.info("client disconnect : {}", id);
        try {
            if (sessions.containsKey(id)) {
                if (session.equals(sessions.get(id).getSession())) {
                    MediaSessionDTO mediaSessionDto = sessions.get(id);
                    returnDeviceEr(mediaSessionDto, id);
                    sessions.remove(id);
                    session.close();
                }
            }
        } catch (Exception e) {
            log.error("disconnect Error :", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("client open: {}", session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws IOException {
        log.info("client close: {}", closeReason);
        sessions.entrySet().stream()
                .filter(pair -> pair.getValue().getSession() == session)
                .findAny().ifPresent(pair -> disconnect(session, pair.getKey()));

    }

    @OnMessage
    public void onMessage(Session session, String message) throws JsonProcessingException {
        log.info("media -> signal payload: {}, session :{}", message,session);
        try {
            synchronized (session) {
                Map map = objectMapper.readValue(message, Map.class);
                switch ((String) map.get("src")) {
                    case "rtp_forwarder": //인풋
                        AnswerInputRtspDTO answerInputRtspDto = objectMapper.readValue(message, AnswerInputRtspDTO.class);
                        Map resDeviceMap = new HashMap();
                        switch (String.valueOf(map.get("purpose"))) {
                            case "ret_start":
                                if ("0".equals(answerInputRtspDto.getReturn_val())) {
                                    resDeviceMap.put("cmd", "startCam");
                                    resDeviceMap.put("tag", answerInputRtspDto.getSession_id());
                                    resDeviceMap.put("id", answerInputRtspDto.getPhone_number());
                                    resDeviceMap.put("type", "webrtc".equals(answerInputRtspDto.getKind()) ? "MOBILE" : "CCTV");
                                    resDeviceMap.put("m_server_ip",answerInputRtspDto.getM_server_ip());
                                    resDeviceMap.put("vPort",answerInputRtspDto.getV_udp_port());
                                    resDeviceMap.put("aPort",answerInputRtspDto.getA_udp_port());
                                    resDeviceMap.put("answer", answerInputRtspDto.getSdp());
                                } else {
                                    //추가규격 손보국선임님과 상의
                                    resDeviceMap.put("cmd", "startCam");
                                    resDeviceMap.put("tag", answerInputRtspDto.getSession_id());
                                    resDeviceMap.put("id", answerInputRtspDto.getPhone_number());
                                    resDeviceMap.put("code", 1);
                                    resDeviceMap.put("msg", "미디어서버 에러");
                                }
                                break;
                            case "candidate":
                                resDeviceMap.put("cmd", "candidate");
                                resDeviceMap.put("tag", answerInputRtspDto.getSession_id());
                                resDeviceMap.put("id", answerInputRtspDto.getPhone_number());
                                resDeviceMap.put("data", answerInputRtspDto.getVal());
                                break;
                            case "connected":
                                log.info("Input Signal Connect to Media Success");
                                MediaSessionDTO mediaSessionDto = sessions.get(String.join(",",answerInputRtspDto.getPhone_number(),null));
                                mediaSessionDto.setStatus("connect");
                                if (sessions.containsKey(String.join(",",answerInputRtspDto.getPhone_number(),null))) {
                                    sessions.put(String.join(",",answerInputRtspDto.getPhone_number(),null), mediaSessionDto);
                                }
                                break;
                        }

                        webSocketDeviceHandler.sendToEvent(resDeviceMap);
                        break;
                    case "rtp_2_webrtc": //아웃풋
                        AnswerOutputRtspDTO answerOutputRtspDto = objectMapper.readValue(message, AnswerOutputRtspDTO.class);
                        Map resMap = new HashMap();
                        switch ((String) map.get("purpose")) {
                            case "ret_offer":
                                if ("0".equals(answerOutputRtspDto.getReturn_val())) {
                                    resMap = ImmutableMap.builder()
                                            .put("cmd", "playCam")
                                            .put("tag", answerOutputRtspDto.getSession_id())
                                            .put("id", answerOutputRtspDto.getId())
                                            .put("device", answerOutputRtspDto.getPhone_number())
                                            .put("answer", answerOutputRtspDto.getSdp())
                                            .build();

                                } else {
                                    resMap = ImmutableMap.builder()
                                            .put("cmd", "error")
                                            .put("tag", cs.getUUID())
                                            .put("id", answerOutputRtspDto.getId())
                                            .put("code", 1)
                                            .put("msg", "미디어서버 에러 answer값 없음")
                                            .build();
                                }
                                break;
                            case "candidate":
                                resMap = ImmutableMap.builder()
                                        .put("cmd", "candidate")
                                        .put("tag", answerOutputRtspDto.getSession_id())
                                        .put("id", answerOutputRtspDto.getId())
                                        .put("device", answerOutputRtspDto.getPhone_number())
                                        .put("data", answerOutputRtspDto.getVal())
                                        .build();
                                break;
                            case "connected":
                                log.info("Output Signal Connect to Media Success");
                                MediaSessionDTO mediaSessionDto = sessions.get(String.join(",",answerOutputRtspDto.getId(),answerOutputRtspDto.getPhone_number()));
                                mediaSessionDto.setStatus("connect");
                                if (sessions.containsKey(String.join(",",answerOutputRtspDto.getId(),answerOutputRtspDto.getPhone_number()))) {
                                    sessions.put(String.join(",",answerOutputRtspDto.getId(),answerOutputRtspDto.getPhone_number()), mediaSessionDto);
                                }
                                break;
                        }
                        webSocketDeviceHandler.sendToEvent(resMap);
                        webSocketWebHandler.sendToEvent(resMap);
                        break;
                }
            }
        } catch (Exception e) {
            log.error("WebSocketClient return Error :", e);
        }
    }

    //여기가 보낼떄
    public void sendMessage(Session session, String message) {

        synchronized (session) {
            log.info("signal -> media payload : {}, session:{}", message,session);
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("Websocket SendError :", e);
            }
        }
    }

    public MediaSessionDTO getSession(String id) {
        if (sessions.containsKey(id)) {
            return sessions.get(id);
        }
        return null;
    }
}
