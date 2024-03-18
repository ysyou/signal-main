package com.clamos.signal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class InputRtspDTO {
    private String src;
    private String session_id;
    private String purpose;
    private String phone_number;
    private String m_server_ip;

    public InputRtspDTO(String src, String session_id, String purpose, String phone_number, String m_server_ip) {
        this.src = src;
        this.session_id = session_id;
        this.purpose = purpose;
        this.phone_number = phone_number;
        this.m_server_ip = m_server_ip;
    }
}
