package com.clamos.signal.service;

import com.clamos.signal.constant.Constants;
import com.clamos.signal.dto.*;
import com.clamos.signal.client.WebSocketClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.websocket.Session;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class WebSocketMediaService {

    private final WebSocketClient wsc;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final CommonService cs;
    private final RedisTemplate redisTemplate;
    private final Environment env;

    // 웹소켓 커넥션
    public Session initWebSocketConnect(String serverUrl, String id, String tag, String cmd) {
        return wsc.connect(serverUrl, id, tag, cmd);
    }

    public void offerInputRtsp(String ip, StartCamDTO startCamDto) {
        //커넥션 맺고

        String url = new StringBuilder().append("ws://").append(ip).append(":").append(env.getProperty("rtsp.input.port")).append(env.getProperty("rtsp.endPoint")).toString();
        Session session = initWebSocketConnect(url, String.join(",",startCamDto.getId(), null), startCamDto.getTag(), startCamDto.getCmd());

        //모바일데이타를 미디어서버에 맞게 컨버팅
        InputOfferDTO dto = InputOfferDTO.builder().src("s_server").session_id(startCamDto.getTag()).phone_number(startCamDto.getId()).m_server_ip(ip).sdp(startCamDto.getOffer()).eventId(startCamDto.getEventId()).purpose("start").kind("MOBILE".equals(startCamDto.getType()) ? "webrtc" : "rtp").build();
        //데이터 전송
        try {
            wsc.sendMessage(session, objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            log.error("offerInputRtsp Error : ", e);
        }
    }

    public void offerOutputRtsp(String ip, String port, PlayCamDTO playCamDto) {
        try {
            String url = new StringBuilder().append("ws://").append(ip).append(":").append(port).append(env.getProperty("rtsp.endPoint")).toString();
            Session session = initWebSocketConnect(url, String.join(",",playCamDto.getId(),playCamDto.getSource()), playCamDto.getTag(), playCamDto.getCmd());
            OutputOfferDTO outputOfferDto = OutputOfferDTO.builder().id(playCamDto.getId()).purpose("offer").sdp(playCamDto.getOffer()).m_server_ip(ip).phone_number(playCamDto.getSource()).src("s_server").session_id(playCamDto.getTag()).build();
            wsc.sendMessage(session, objectMapper.writeValueAsString(outputOfferDto));
        } catch (Exception e) {
            log.error("offerOutputRtsp Error : ", e);
        }
    }

    //maria에서 서버 목록가져와서
    public String getMediaServerInfo() {
        String ip = "";
        List<String> keys = new ArrayList<>(redisService.getPatternValue("media_server_tb:*"));
        List<Map<String, String>> list = new ArrayList<>();
        for (String key : keys) {
            try {
                Map map = redisTemplate.opsForHash().entries(key);
                if (!ObjectUtils.isEmpty(map)) {
                    if (map.containsKey("num_outputs") && map.containsKey("num_inputs") && map.containsKey("ping_unixtime")) {
                        if (cs.getNowMilSecond() - (Long) map.get("ping_unixtime") <= 15000) {
                            map.put("ip", key.split(":")[1]);
                            list.add(map);
                        }
                    }
                }
            }catch (Exception e){
                log.error("Redis media_server_tb 규격이 잘못되었습니다. Key: {} ", key, e);
            }
        }

        Collections.sort(list, new Comparator<Map>() {
            @Override
            public int compare(Map o1, Map o2) {
                Integer num1 = (Integer) o1.get("num_inputs") + (Integer) o1.get("num_outputs");
                Integer num2 = (Integer) o2.get("num_inputs") + (Integer) o2.get("num_outputs");
                return num1.compareTo(num2);
            }
        });
        Socket socket;
        for (Map<String, String> m : list) {
            try {
                socket = new Socket(m.get("ip"), Integer.parseInt(env.getProperty("rtsp.input.port")));
                Boolean result = socket.isConnected();
                if (result) {
                    ip = m.get("ip");
                    socket.close();
                    break;
                }else{
                    log.info("socket connected result false");
                }
            }catch (SocketTimeoutException e){
                log.error("media server socket time out Exception", e);
            }catch (ConnectException e1){
                log.error("media server connection Exception", e1);
            } catch (IOException e2){
                log.error("media server connect Error", e2);
            }
        }
        log.info(" Input media server url :{}", ip);
        return ip;
    }
    //이미 연결된 ip를 추출
    public String getDeviceMediaServerInfo(PlayCamDTO playCamDto) {
        //redis phone_tb 에서 조회해서 가져오는 ip값을 사용
        try {
            String key = new StringBuilder().append(Constants.PHONE_TB).append(playCamDto.getSource()).toString();
            String value = redisService.getValue(key);
            if (StringUtils.hasText(value)) {
                Map map = objectMapper.readValue(redisService.getValue(key), Map.class);
                if (StringUtils.hasText((String) map.get("m_server_ip")) && map.get("playable").equals("1")) {
                    log.info("media server url : {}", map.get("m_server_ip"));
                    return (String) map.get("m_server_ip");
                }
            }
        } catch (Exception e) {
            log.error("WebSocketMediaService getDeviceMediaServerInfo Error :", e);
        }
        log.info("media server url is null");
        return "";
    }

    public Boolean sendCandidate(CandidateDTO candidateDto, String type) throws JsonProcessingException {
        MediaSessionDTO mediaSessionDto = wsc.getSession(String.join(",",candidateDto.getId(),candidateDto.getSource()));
        if (mediaSessionDto != null) {
            String msg;
            if(type.equals(Constants.IN_PUT)){
                InputCandidateDTO dto = InputCandidateDTO.builder().src("s_server").session_id(candidateDto.getTag()).purpose("candidate").phone_number(candidateDto.getId()).m_server_ip(mediaSessionDto.getIp()).val(candidateDto.getData()).build();
                msg = objectMapper.writeValueAsString(dto);
            }else{
                OutputCandiateDTO dto = OutputCandiateDTO.builder().src("s_server").m_server_ip(mediaSessionDto.getIp()).phone_number(candidateDto.getSource()).session_id(candidateDto.getTag()).val(candidateDto.getData()).id(candidateDto.getId()).purpose("candidate").build();
                msg = objectMapper.writeValueAsString(dto);
            }
            wsc.sendMessage(mediaSessionDto.getSession(), msg);
            return       true;
        } else {
            return false;
        }
    }
}
