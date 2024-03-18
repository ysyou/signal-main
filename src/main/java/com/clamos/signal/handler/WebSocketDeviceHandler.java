package com.clamos.signal.handler;

import com.clamos.signal.constant.Command;
import com.clamos.signal.constant.Constants;
import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.dto.*;
import com.clamos.signal.entity.WebsocketDeviceEntity;
import com.clamos.signal.repository.WebSocketDeviceRepository;
import com.clamos.signal.service.*;
import com.clamos.signal.dto.ResultDTO;
import com.clamos.signal.dto.SignalStatusDTO;
import com.clamos.signal.dto.CommandDTO;
import com.clamos.signal.vo.ConfigVO;
import com.clamos.signal.vo.ResponseVO;
import com.clamos.signal.dto.SessionDTO;
import com.clamos.signal.vo.SessionListVO;
import com.clamos.signal.vo.SessionVO;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class WebSocketDeviceHandler extends TextWebSocketHandler {
    final ObjectMapper objectMapper;
    final WebSocketMediaService webSocketMediaService;
    final RedisMessageListenerContainer redisMessageListener;
    final RedisSubscriberService redisSubscriberService;
    final RedisPublisherService redisPublisherService;
    final WebSocketDeviceRepository websocketDeviceRepository;
    final RedisTemplate redisTemplate;
    final RedisService redisService;
    final CommonService cs;
    final Environment env;
    final ManagerService managerService;
    final WebSocketDeviceService websocketDeviceService;
    final static List<SessionDTO> sessionList = new CopyOnWriteArrayList<>();

    public WebSocketDeviceHandler(ObjectMapper objectMapper, WebSocketMediaService webSocketMediaService, RedisMessageListenerContainer redisMessageListener, @Lazy RedisSubscriberService redisSubscriberService, RedisPublisherService redisPublisherService, WebSocketDeviceRepository websocketDeviceRepository, RedisTemplate redisTemplate, RedisService redisService, CommonService cs, Environment env, ManagerService managerService, WebSocketDeviceService websocketDeviceService) {
        this.objectMapper = objectMapper;
        this.webSocketMediaService = webSocketMediaService;
        this.redisMessageListener = redisMessageListener;
        this.redisSubscriberService = redisSubscriberService;
        this.redisPublisherService = redisPublisherService;
        this.websocketDeviceRepository = websocketDeviceRepository;
        this.redisTemplate = redisTemplate;
        this.redisService = redisService;
        this.cs = cs;
        this.env = env;
        this.managerService = managerService;
        this.websocketDeviceService = websocketDeviceService;
    }

    // connection opened
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("session connected : {}", session);
        session.setTextMessageSizeLimit(16384);
        //세션 blocking send timeout 설정 -> wifi -> lte나 변경됬을경우 끊어지면 10초 blocking (기존 20 -> 10초)
        if (session instanceof NativeWebSocketSession) {
            final Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
            if (nativeSession != null) {
                nativeSession.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 10_000L);
            }
        }
        SessionDTO sessionDto = new SessionDTO();
        sessionDto.setSession(session);
        sessionList.add(sessionDto);
    }

    @Scheduled(fixedDelay = 5000)
    public void ping() {
        sessionList.forEach(sessionDTO -> {
            if (StringUtils.hasLength(sessionDTO.getId())) {
                send(sessionDTO.getSession(), new PingMessage());
                if (!ObjectUtils.isEmpty(sessionDTO.getTtl())) {
                    if ((cs.getNowSecond() - sessionDTO.getTtl() >= Constants.REDIS_PING_TIME)) {
                        endSession(sessionDTO.getSession(), "pingDelte");
                    }
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("session disconnected : {}, reason : {}, code {}", session, closeStatus.getReason(), closeStatus.getCode());
        endSession(session, "FromDeviceDelte");
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        Optional<SessionDTO> anySession = sessionList.stream().filter(sessionDTO -> sessionDTO.getSession() == session && StringUtils.hasLength(sessionDTO.getId())).findAny();
        if (anySession.isPresent()) {
            SessionDTO sessionDto = anySession.get();
            Optional<WebsocketDeviceEntity> wee = websocketDeviceRepository.findById(sessionDto.getId());
            if (wee.isPresent()) {
                sessionDto.setTtl(cs.getNowSecond());
                redisService.setTTL(sessionDto.getId());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            CommandDTO commandDto = objectMapper.readValue(payload, CommandDTO.class);
            if (!Objects.equals(commandDto.getCmd(), Command.DEVICE_STATUS)) {
                log.info("device -> signal payload {}: Sesssion : {}", payload, session);
            }
            ResponseVO res = null;
            if (commandDto.getId() == null) {
                commandDto.setId("");
            }
            Boolean hasLogin = sessionList.stream().anyMatch(sessionDTO -> sessionDTO.getSession() == session && StringUtils.hasText(sessionDTO.getId()));

            if (hasLogin) {
                switch (commandDto.getCmd()) {
                    case Command.PLAY_CAM:
                        PlayCamDTO playCamDto = objectMapper.readValue(payload, PlayCamDTO.class);
                        String url = webSocketMediaService.getDeviceMediaServerInfo(playCamDto);
                        if (!StringUtils.hasText(url)) {
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.REDIS_MEDIA_DEVICE_URL_IS_NULL);
                        } else {
                            webSocketMediaService.offerOutputRtsp(url, env.getProperty("rtsp.output.port"), playCamDto);
                        }
                        break;
                    case Command.DEVICE_STATUS:
                        // manger 에서 저장 할 수 있게 deviceStatus 도 전송
                        redisTemplate.convertAndSend(Command.DEVICE_STATUS, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        //stomp
                        Optional<SessionDTO> anySessionDto = sessionList.stream().filter(sessionDTO -> Objects.equals(sessionDTO.getSession(), session)).findAny();
                        anySessionDto.ifPresent(sessionDTO -> sessionDTO.getGroupIds().forEach(o -> redisPublisherService.publish(new ChannelTopic((String) o), payload)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.DRAW_PEN:
                        Map value = objectMapper.readValue(payload, Map.class);
                        if (value.get("code") == null) {
                            redisTemplate.convertAndSend(Command.DEVICE_DRAW_PEN, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        }
                        break;
                    case Command.CANDIDATE:
                        CandidateDTO candidateDto = objectMapper.readValue(payload, CandidateDTO.class);
                        if (!webSocketMediaService.sendCandidate(candidateDto, Constants.IN_PUT)) {
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.MEDIA_CANDIDATE_SESSION_IS_NULL);
                        }
                        break;
                    case Command.LOGIN:
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.SESSION_LIST:
                        List<SessionVO> deviceSessionList = CommonHandlerService.sessionList(sessionList);
                        res = new SessionListVO(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS, deviceSessionList);
                        break;
                    case Command.START_CAM:
                        StartCamDTO startCamDto = objectMapper.readValue(payload, StartCamDTO.class);
                        String mediaServerUrl = webSocketMediaService.getMediaServerInfo();
                        if (Objects.equals("", mediaServerUrl)) {
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.REDIS_MEDIA_URL_IS_NULL);
                        } else {
                            webSocketMediaService.offerInputRtsp(mediaServerUrl, startCamDto);
                        }
                        break;
                    case Command.CONFIG:
                        Map config = objectMapper.readValue(payload, Map.class);
                        if (ObjectUtils.isEmpty(config.get("code"))) {
                            ResultDTO<Map<String, Object>> resultConfig = managerService.getDeviceConfigById((String) config.get("id"));
                            res = (resultConfig.getCode() > 1) ? new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), resultConfig.getCode(), resultConfig.getMessage()) : new ConfigVO(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS, commandDto.getId(), (String) resultConfig.getData().get("rtsp"), (String) resultConfig.getData().get("type"), (Integer) resultConfig.getData().get("resolution"), (Integer) resultConfig.getData().get("fps"), (Boolean) resultConfig.getData().get("alchera"), (Double) resultConfig.getData().get("lat"), (Double) resultConfig.getData().get("lon") );
                        }
                        break;
                    case Command.PTZ_STAT:
                        redisTemplate.convertAndSend(Command.DEVICE_PTZ_STAT, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        break;
                    case Command.AUDIO:
                        Map audio = objectMapper.readValue(payload, Map.class);
                        sessionList.stream().filter(sessionDTO -> sessionDTO.getSession() == session)
                                .findFirst()
                                .ifPresent(sessionDTO -> audio.put("groupIds", sessionDTO.getGroupIds()));
                        redisTemplate.convertAndSend(Command.AUDIO, objectMapper.writeValueAsString(audio));
                        break;
                    default:
                        break;
                }
            } else {
                switch (commandDto.getCmd()) {
                    case Command.LOGIN:
                        log.info("login: {}, sessionId {}", message.getPayload(), session.getId());
                        if (websocketDeviceService.existsById(commandDto.getId())) {
                            Optional<WebsocketDeviceEntity> wde = websocketDeviceRepository.findById(commandDto.getId());
                            if (wde.isPresent()) {
                                WebsocketDeviceEntity websocketDeviceEntity = wde.get();
                                if(cs.getNowSecond() - websocketDeviceEntity.getTtl() > Constants.DUPLICATE_LOGIN_TIME){
                                    log.info("현재시간 : {}", cs.getNowSecond());
                                    log.info("레디스에 저장된 시간 : {}", websocketDeviceEntity.getTtl());
                                    log.info("타임아웃 새 로그인 Session : {}, id : {}", session, commandDto.getId());
                                    res = login(session, commandDto);
                                }else{
                                    res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.DUPLICATION_LOGIN);
                                }
                            }
                        } else {
                            res = login(session, commandDto);
                        }
                        break;
                    case Command.SESSION_LIST:
                        List<SessionVO> deviceSessionList = CommonHandlerService.sessionList(sessionList);
                        res = new SessionListVO(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS, deviceSessionList);
                        break;
                    case Command.KEEP_ALIVE:
                        break;
                    default:
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.DO_NOT_LOGIN);
                }
            }
            if (res != null) {
                String responseMessage = objectMapper.writeValueAsString(res);
                send(session, responseMessage);
            }
        } catch (JsonParseException e) {
            //파싱오류
            ResponseVO<Object> res = new ResponseVO<>("error", "", MessageCode.STRUCTURE);
            try {
                String responseMessage = objectMapper.writeValueAsString(res);
                send(session, responseMessage);
            } catch (Exception e3) {
                log.info(e3.getMessage());
            }
        } catch (Exception e1) {
            log.error("WebSocketDeviceHandler Exception Error : ", e1);
        }
    }

    public void updateSessionGroupIds(String id) {
        sessionList.stream()
                .filter(sessionDTO -> Objects.equals(sessionDTO.getId(), id))
                .findAny()
                .ifPresent(sessionDTO -> {
                    ResultDTO<List<String>> deviceGroupIds = managerService.getDeviceGroupIds(id);
                    List<String> groupIds = deviceGroupIds.getData();
                    sessionDTO.setGroupIds(groupIds);
                    Optional<WebsocketDeviceEntity> wee = websocketDeviceRepository.findById(id);
                    if (wee.isPresent()) {
                        redisService.setGroupIds(id, groupIds);
                    }
                });
    }

    private ResponseVO login(WebSocketSession session, CommandDTO commandDto) {
        ResponseVO res = null;
        try {
            ResultDTO<List<String>> deviceGroupIds = managerService.getDeviceGroupIds(commandDto.getId());
            List<String> data = deviceGroupIds.getData();
            log.info("sessionId : {}, LoginId : {}", session.getId(), commandDto.getId());

            Optional<SessionDTO> any = sessionList.stream().filter(sessionDTO -> sessionDTO.getSession().equals(session)).findAny();
            SessionDTO sessionDto;
            if (any.isPresent()) {
                sessionDto = any.get();
            } else {
                sessionDto = new SessionDTO();
                sessionDto.setSession(session);
                sessionList.add(sessionDto);
            }
            sessionDto.setTtl(cs.getNowSecond());
            sessionDto.setId(commandDto.getId());
            sessionDto.setGroupIds(data);

            //레디스에 있으면 업데이트만 해주고 없으면 신규 저장 해주자
            Optional<WebsocketDeviceEntity> byId = websocketDeviceRepository.findById(commandDto.getId());
            if (byId.isPresent()) {
                redisService.setSignalStatus(commandDto.getId(),Constants.TRUE);
            }else{
                WebsocketDeviceEntity we = WebsocketDeviceEntity.builder().id(commandDto.getId()).signalStatus(Constants.TRUE).mediaStatus(Constants.FALSE).ttl(cs.getNowSecond()).groupIds(data).ip(env.getProperty("eureka.instance.ip-address")).build();
                websocketDeviceRepository.save(we);
            }

            // Redis Pub 시그널 Status True 전달
            SignalStatusDTO signalStatusDto = SignalStatusDTO.builder().id(commandDto.getId()).status(Constants.TRUE).cmd("signalStatus").tag(cs.getUUID()).groupIds(sessionDto.getGroupIds()).build();
            redisTemplate.convertAndSend(Command.SIGNAL_STATUS, objectMapper.writeValueAsString(signalStatusDto));
            //STOMP 토픽생성
            data.forEach(s -> {
                ChannelTopic topic = new ChannelTopic(s);
                redisMessageListener.addMessageListener(redisSubscriberService, topic);
            });

            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
        } catch (Exception e) {
            log.error("login Error : ", e);
        }
        return res;
    }

    public void send(WebSocketSession session, String payload) {
        try {
            Map map = objectMapper.readValue(payload, Map.class);
            if (!Objects.equals(map.get("cmd"), Command.DEVICE_STATUS)) {
                log.info("signal -> device payload: {}", payload);
            }
            TextMessage message = new TextMessage(payload);
            send(session, message);
        } catch (Exception e) {
            log.error("send error: ", e);
        }
    }

    private void send(WebSocketSession session, WebSocketMessage message) {
        try {
            synchronized (session) {
                if (!ObjectUtils.isEmpty(message)) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("exception error: sessionId :{}", session.getId(), e);
            endSession(session, "sendErrorDelete");
        }
    }


    public void sendToEvent(Map map) throws IOException {
        for (SessionDTO sessionDto : sessionList) {
            if (!map.isEmpty()) {
                if (String.valueOf(sessionDto.getId()).equals(map.get("id"))) {
                    String responseMessage = objectMapper.writeValueAsString(map);
                    send(sessionDto.getSession(), responseMessage);
                }
            }
        }
    }

    public void endSession(WebSocketSession session, String logData) {
        try {
            sessionList.forEach(sessionDTO -> {
                if (sessionDTO.getSession().equals(session)) {
                    if (sessionDTO.getId() != null) {
                        SignalStatusDTO signalStatusDto = SignalStatusDTO.builder().id(sessionDTO.getId()).status(0).cmd("signalStatus").tag(cs.getUUID()).groupIds(sessionDTO.getGroupIds()).build();
                        try {
                            redisTemplate.convertAndSend(Command.SIGNAL_STATUS, objectMapper.writeValueAsString(signalStatusDto));
                        } catch (JsonProcessingException e) {
                            log.error("endSession objectmapper error :", e);
                        }
                        if (websocketDeviceService.existsById(sessionDTO.getId())) {
                            try {
                                redisService.setSignalStatus(sessionDTO.getId(), Constants.FALSE);
                            } catch (Exception e) {
                                log.error("디바이스 엔드 세션 에러 :", e);
                            }
                        }
                    }
                    log.info("session remove dto :{}, session: {}, logData: {}", sessionDTO, session, logData);
                    sessionList.remove(sessionDTO);
                }
            });
        } catch (Exception e) {
            log.error("session close error :", e);
        }
    }

    public List<String> getSessionList() {
        List<String> ids = new ArrayList<>();
        for (SessionDTO sessionDto : sessionList) {
            ids.add(sessionDto.getId());
        }
        return ids;
    }


    // transport error
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        try {
            log.error("Device transport error : " + session + ", exception : " + exception);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}