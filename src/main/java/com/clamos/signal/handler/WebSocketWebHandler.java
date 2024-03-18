package com.clamos.signal.handler;

import com.clamos.signal.constant.Command;
import com.clamos.signal.constant.Constants;
import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.dto.*;
import com.clamos.signal.repository.WebSocketWebRpository;
import com.clamos.signal.service.*;
import com.clamos.signal.entity.WebsocketWebEntity;
import com.clamos.signal.vo.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class WebSocketWebHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;
    private final RedisMessageListenerContainer redisMessageListener;
    private final RedisSubscriberService redisSubscriberService;
    private final WebSocketMediaService webSocketMediaService;
    private final WebSocketWebRpository websocketWebRpository;
    private final CommonService cs;
    private final Environment env;
    private final WebHandlerService webHandlerService;
    private static final List<SessionDTO> sessionList = new CopyOnWriteArrayList<>();

    public WebSocketWebHandler(ObjectMapper objectMapper, RedisTemplate redisTemplate, RedisMessageListenerContainer redisMessageListener, @Lazy RedisSubscriberService redisSubscriberService, WebSocketMediaService webSocketMediaService, WebSocketWebRpository websocketWebRpository, CommonService cs, Environment env, WebHandlerService webHandlerService) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.redisMessageListener = redisMessageListener;
        this.redisSubscriberService = redisSubscriberService;
        this.webSocketMediaService = webSocketMediaService;
        this.websocketWebRpository = websocketWebRpository;
        this.cs = cs;
        this.env = env;
        this.webHandlerService = webHandlerService;
    }

    // connection opened
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(16384);
        SessionDTO sessionDto = new SessionDTO();
        sessionDto.setSession(session);
        /*sessionDto.setTimer(new Timer());*/
        sessionList.add(sessionDto);
        ServerInfoVO serverInfoVO = new ServerInfoVO("s",env.getProperty("eureka.instance.ip-address"));
        String s = objectMapper.writeValueAsString(serverInfoVO);
        send(session,s);
        log.info("session connected : " + session);
    }

    // connection closed
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("session disconnected : {}", session);
        endSession(session);
    }

    @Scheduled(fixedDelay = 5000)
    public void ping() {
        sessionList.forEach(sessionDTO -> {
            if (StringUtils.hasLength(sessionDTO.getId())) {
                send(sessionDTO.getSession(), new PingMessage());
            }
        });
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        Optional<SessionDTO> anySession = sessionList.stream().filter(sessionDTO -> sessionDTO.getSession() == session && StringUtils.hasLength(sessionDTO.getId())).findAny();
        if (anySession.isPresent()) {
            SessionDTO sessionDto = anySession.get();
            if (websocketWebRpository.existsById(sessionDto.getId())) {
                Optional<WebsocketWebEntity> wwe = websocketWebRpository.findById(sessionDto.getId());
                if (wwe.isPresent()) {
                    WebsocketWebEntity ww = wwe.get();
                    ww.setTtl(cs.getNowSecond());
                    websocketWebRpository.save(ww);
//                    sessionDto.setTtl(cs.getNowSecond());
                }
            }
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            CommandDTO commandDto = objectMapper.readValue(payload, CommandDTO.class);
            ResponseVO res = null;
            if (commandDto.getId() == null) {
                commandDto.setId("");
            }
            if (!Objects.equals(commandDto.getCmd(), Command.KEEP_ALIVE)) {
                log.info("web -> signal payload {}: ", payload);
            }
            boolean hasLogin = sessionList.stream().anyMatch(sessionDTO -> sessionDTO.getSession() == session && StringUtils.hasText(sessionDTO.getId()) && websocketWebRpository.existsById(sessionDTO.getId()));

            //기존로그인이 된
            if (hasLogin) {
                /*stop(session);
                start(env.getProperty("signal.timer"),session,se);*/
                switch (commandDto.getCmd()) {
                    case Command.MEDIA_LIST:
                        //미디어 리스트는 중간에 상태값이 변경될 수 있어서 별도의 싱크를 걸어줌으로 처리
                        synchronized (session){
                            List<MediaListDTO> mediaList = webHandlerService.mediaList();
                            res = new MediaListVO(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS, mediaList);
                            TextMessage mediaMessage = new TextMessage(objectMapper.writeValueAsString(res));
                            session.sendMessage(mediaMessage);
                            res = null;
                        }
                        break;
                    case Command.CANDIDATE:
                        Boolean result = webSocketMediaService.sendCandidate(objectMapper.readValue(payload, CandidateDTO.class), Constants.OUT_PUT);
                        if (!result) {
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.MEDIA_CANDIDATE_SESSION_IS_NULL);
                        }
                        break;
                    case Command.WEB_RELOAD:
                        redisTemplate.convertAndSend(Command.WEB_RELOAD, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.WEB_START_CAM:
                        redisTemplate.convertAndSend(Command.WEB_START_CAM, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.WEB_STOP_CAM:
                        redisTemplate.convertAndSend(Command.WEB_STOP_CAM, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.PLAY_CAM:
                        PlayCamDTO playCamDto = objectMapper.readValue(payload, PlayCamDTO.class);
                        String mediaServerUrl = webSocketMediaService.getDeviceMediaServerInfo(playCamDto);
                        if (!StringUtils.hasText(mediaServerUrl)) {
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.REDIS_MEDIA_URL_IS_NULL);
                            send(session, objectMapper.writeValueAsString(new MediaStatusVO(Command.MEDIA_STATUS, commandDto.getTag(), playCamDto.getSource(), Constants.FALSE)));
                        } else {
                            webSocketMediaService.offerOutputRtsp(mediaServerUrl, env.getProperty("rtsp.output.port"), playCamDto);
                        }
                        break;
                    case Command.SHARE_CAM:
                        redisTemplate.convertAndSend(Command.SHARE_CAM, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.WEB_ORI:
                        redisTemplate.convertAndSend(Command.WEB_ORI, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.SHARE_IMG:
                        redisTemplate.convertAndSend(Command.SHARE_IMG, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.TOGGLE_CAM:
                        redisTemplate.convertAndSend(Command.TOGGLE_CAM, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.DRAW_PEN:
                        if (objectMapper.readValue(payload, Map.class).get("code") == null) {
                            redisTemplate.convertAndSend(Command.WEB_DRAW_PEN, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                            res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        }
                        break;
                    case Command.PTZ:
                        redisTemplate.convertAndSend(Command.PTZ, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        res = new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.SUCCESS);
                        break;
                    case Command.PTZ_STAT:
                        redisTemplate.convertAndSend(Command.WEB_PTZ_STAT, objectMapper.writeValueAsString(objectMapper.readValue(payload, Map.class)));
                        break;
                    case Command.SESSION_LIST:
                        List<SessionVO> webSessionList = CommonHandlerService.sessionList(sessionList);
                        res = new SessionListVO(commandDto.getCmd(), "", MessageCode.SUCCESS, webSessionList);
                        break;
                    case Command.MEDIA_STATUS:
                        Map map = objectMapper.readValue(payload, Map.class);
                        redisTemplate.convertAndSend(Command.MEDIA_STATUS, "{\"phone_number\":" + "\"" + map.get("device") + "\"" + ",\"status\":" + "\"" + map.get("status") + "\"" + "}");
                        break;
                    case Command.WEB_AUDIO:
                        redisTemplate.convertAndSend(Command.WEB_AUDIO, payload);
                        break;
                    default:
                        return;
                }
            } else {
                switch (commandDto.getCmd()) {
                    case Command.LOGIN:
                        WebLoginDTO webLoginDto = objectMapper.readValue(payload, WebLoginDTO.class);
                        if (webLoginDto.getForce()) {
                            webLoginDto.setSessionId(session.getId());
                            redisTemplate.convertAndSend(Command.LOGIN_FORCE, objectMapper.writeValueAsString(webLoginDto));
                        } else {
                            if (websocketWebRpository.existsById(commandDto.getId())) {
                                Optional<WebsocketWebEntity> wwe = websocketWebRpository.findById(commandDto.getId());
                                if (wwe.isPresent()) {
                                    WebsocketWebEntity websocketWebEntity = wwe.get();
                                    res = (cs.getNowSecond() - websocketWebEntity.getTtl() > Constants.DUPLICATE_LOGIN_TIME) ? login(session, webLoginDto) : new ResponseVO<>(commandDto.getCmd(), commandDto.getTag(), MessageCode.DUPLICATION_LOGIN);
                                }
                            } else {
                                res = login(session, webLoginDto);
                            }
                        }
                        break;
                    case Command.SESSION_LIST:
                        List<SessionVO> webSessionList = CommonHandlerService.sessionList(sessionList);
                        res = new SessionListVO(commandDto.getCmd(), "", MessageCode.SUCCESS, webSessionList);
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
            try {
                String responseMessage = objectMapper.writeValueAsString(new ResponseVO<>("error", "", MessageCode.STRUCTURE));
                send(session, responseMessage);
            } catch (Exception e3) {
                log.error("error parsing :", e3);
            }
        } catch (Exception e1) {
            log.error("WebSocketWebHandler Exception Error : ", e1);
        }

    }


    private ResponseVO login(WebSocketSession session, WebLoginDTO webLoginDto) {
        ResponseVO res = null;
        try {
            log.info("login session:{} , id :{}", session, webLoginDto.getId());
            Optional<SessionDTO> any = sessionList.stream().filter(sessionDTO -> sessionDTO.getSession().equals(session)).findAny();
            SessionDTO sessionDto;
            if (any.isPresent()) {
                sessionDto = any.get();
            } else {
                sessionDto = new SessionDTO();
                sessionDto.setSession(session);
                sessionList.add(sessionDto);
            }

            sessionDto.setId(webLoginDto.getId());
            sessionDto.setGroupId(webLoginDto.getGroupId());

            WebsocketWebEntity ww = WebsocketWebEntity.builder().id(webLoginDto.getId()).status("connect").ttl(cs.getNowSecond()).ip(env.getProperty("eureka.instance.ip-address")).build();
            websocketWebRpository.save(ww);

            //  STOMP 처리
            /*ChannelTopic topic = new ChannelTopic(webLoginDto.getGroupId());
            redisMessageListener.addMessageListener(redisSubscriberService, topic);*/
            //로그인시 바로 ping/pong timer
            //start(env.getProperty("signal.timer"), session, sessionDto.getTimer());
            res = new ResponseVO<>(webLoginDto.getCmd(), webLoginDto.getTag(), MessageCode.SUCCESS);
        } catch (Exception e) {
            log.error("WebsocketWebHandler Error : ", e);
        }
        return res;
    }


    public void send(WebSocketSession session, String payload) {
        log.info("signal -> web payload: {}", payload);
        TextMessage message = new TextMessage(payload);
        send(session, message);
    }

    public void send(WebSocketSession session, WebSocketMessage message) {
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("send error :{}", session.getId(), e);
        } catch (IllegalStateException e1) {
            log.error("Session Error : {} ", session, e1);
            endSession(session);
        } catch (Exception e2) {
            log.error("exception error: sessionId :{}", session, e2);
        }
    }

    public void sendToEvent(Map map) throws IOException {
        for (SessionDTO sessionDto : sessionList) {
            if (!map.isEmpty()) {
                if (map.get("id").equals(sessionDto.getId())) {
                    String payload = objectMapper.writeValueAsString(map);
                    send(sessionDto.getSession(), payload);
                }
            }
        }
    }

    public void sendToAll(String payload) {
        for (SessionDTO sessionDto : sessionList) {
            if (sessionDto.getId() != null) {
                send(sessionDto.getSession(), payload);
            }
        }
    }
    
    public void sendToGroups(String payload, List<String> groupIds) {
        if (groupIds != null) {
            sessionList.forEach(sessionDTO -> {
                if (!ObjectUtils.isEmpty(sessionDTO.getId())) {
                    if (groupIds.contains(sessionDTO.getGroupId())) {
                        send(sessionDTO.getSession(), payload);
                    }
                }
            });
        }
    }

    @Override



    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Web Server transport error : " + session + ", exception : " + exception);
    }

    public void endSession(WebSocketSession session) {
        try {
            sessionList.forEach(sessionDTO -> {
                if (sessionDTO.getSession().equals(session)) {
                    if (sessionDTO.getId() != null) {
                        if (websocketWebRpository.existsById(sessionDTO.getId())) {
                            log.info("session close id : {}, session: {}", sessionDTO.getId(), session);
                            Optional<WebsocketWebEntity> byId = websocketWebRpository.findById(sessionDTO.getId());
                            if (byId.isPresent()) {
                                WebsocketWebEntity websocketWebEntity = byId.get();
                                websocketWebRpository.deleteById(websocketWebEntity.getId());
                            }
                        }
                    }
                    sessionList.remove(sessionDTO);
                }
            });
        } catch (Exception e) {
            log.error("session close error :", e);
        }
    }

    public void endSessionById(Map map) {
        sessionList.forEach(sessionDTO -> {
            if (sessionDTO.getId() != null && sessionDTO.getId().equals(map.get("id"))) {
                endSession(sessionDTO.getSession());
            }
        });
    }

    public void forceLogin(Map map) {
        sessionList.forEach(sessionDTO -> {
            if (!map.isEmpty()) {
                if (sessionDTO.getSession().getId().equals(map.get("sessionId"))) {
                    WebLoginDTO webLoginDto = new WebLoginDTO();
                    webLoginDto.setCmd("login");
                    webLoginDto.setTag((String) map.get("tag"));
                    webLoginDto.setId((String) map.get("id"));
                    webLoginDto.setGroupId((String) map.get("groupId"));
                    ResponseVO res = login(sessionDTO.getSession(), webLoginDto);
                    if (res != null) {
                        try {
                            send(sessionDTO.getSession(), objectMapper.writeValueAsString(res));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
    }

    public void start(WebSocketSession session, Timer timer) throws Exception {
        TimerTask task = new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                sessionList.stream().filter(sessionDTO -> Objects.equals(sessionDTO.getSession(), session) && StringUtils.hasLength(sessionDTO.getId())).findAny().ifPresent(sessionDTO -> {
                    log.info("ping 테스트입니다. id : {} session : {}", sessionDTO.getId(), session);
                    send(sessionDTO.getSession(), new PingMessage());
                });
            }
        };
        timer.scheduleAtFixedRate(task, 5000, 5000);
    }

    public void stop(WebSocketSession session) {
        sessionList.stream().filter(sessionDTO -> Objects.equals(sessionDTO.getSession(), session)).findAny().ifPresent(sessionDTO -> {
            sessionDTO.getTimer().cancel();
            sessionDTO.setTimer(new Timer());
        });
    }
}
