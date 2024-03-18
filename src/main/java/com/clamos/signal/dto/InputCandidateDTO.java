package com.clamos.signal.dto;

import lombok.Builder;
import lombok.Data;

@Data
public class InputCandidateDTO extends InputRtspDTO {
    private String val;

    @Builder
    public InputCandidateDTO(String src, String session_id, String purpose, String phone_number, String m_server_ip, String val) {
        super(src, session_id, purpose, phone_number, m_server_ip);
        this.val = val;
    }
}
