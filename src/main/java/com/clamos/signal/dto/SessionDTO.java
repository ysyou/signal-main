package com.clamos.signal.dto;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.List;
import java.util.Timer;

@Data
public class SessionDTO implements Serializable {
    private WebSocketSession session;
    private String id;
    private String groupId;
    private Long ttl;
    private Timer timer;
    private List groupIds;
}
