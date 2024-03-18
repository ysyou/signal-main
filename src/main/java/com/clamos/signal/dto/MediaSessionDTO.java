package com.clamos.signal.dto;

import lombok.Builder;
import lombok.Data;

import javax.websocket.Session;

@Data
public class MediaSessionDTO {
    private Session session;
    private String ip;
    private String status;
    private String cmd;
    private String tag;

    @Builder
    public MediaSessionDTO(Session session, String ip, String status, String cmd, String tag) {
        this.session = session;
        this.ip = ip;
        this.status = status;
        this.cmd = cmd;
        this.tag = tag;
    }
}
