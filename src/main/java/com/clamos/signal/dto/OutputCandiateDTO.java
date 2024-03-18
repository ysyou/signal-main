package com.clamos.signal.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class OutputCandiateDTO extends OutputRtspDTO {
    private String val;

    @Builder
    public OutputCandiateDTO(String src, String session_id, String purpose, String id, String m_server_ip, String phone_number, String val) {
        super(src, session_id, purpose, id, m_server_ip, phone_number);
        this.val = val;
    }
}