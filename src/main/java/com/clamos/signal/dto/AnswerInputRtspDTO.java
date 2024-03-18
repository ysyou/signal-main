package com.clamos.signal.dto;

import lombok.Data;

@Data
public class AnswerInputRtspDTO {
    private String src;
    private String session_id;
    private String purpose;
    private String sdp;
    private String val;
    private String return_val;
    private String phone_number;
    private String kind;
    private String m_server_ip;
    private String v_udp_port;
    private String a_udp_port;
}
