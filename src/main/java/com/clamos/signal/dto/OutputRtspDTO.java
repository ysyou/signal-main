package com.clamos.signal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class OutputRtspDTO {
    private String src;
    private String session_id;
    private String purpose;
    private String id;
    private String m_server_ip;
    private String phone_number;

    public OutputRtspDTO(String src, String session_id, String purpose, String id, String m_server_ip, String phone_number) {
        this.src = src;
        this.session_id = session_id;
        this.purpose = purpose;
        this.id = id;
        this.m_server_ip = m_server_ip;
        this.phone_number = phone_number;
    }
}
