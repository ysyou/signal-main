package com.clamos.signal;

import com.clamos.signal.dto.CommandDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;

import javax.websocket.ClientEndpoint;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

@SpringBootTest
@Slf4j
@ClientEndpoint
class SignalApplicationTests {
    @Autowired
    ObjectMapper objectMapper;
    @Test
    void contextLoads() throws Exception{
        Map map = new HashMap<>();
        map.put("1","fsafa");
        map.put("2","2fsf");
        map.put("3","2fas");
        map.put("4","2fsd");
        map.put("5","2asdfasdf");
        map.put("6","2qreqwrq");
        map.put("7","2gsdfgdfsg");
        map.put("8","2bcxbcx");
        map.put("9","2aasa");
        map.put("10","2ww");
        map.put("11","2e");
        map.put("12","2r");
        List<Map> list = new ArrayList<>();
        list.add(map);





    }

}

