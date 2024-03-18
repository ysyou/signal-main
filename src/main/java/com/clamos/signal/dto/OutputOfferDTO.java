package com.clamos.signal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class OutputOfferDTO extends OutputRtspDTO {
    private String sdp;

    @Builder
    public OutputOfferDTO(String sdp, String src, String session_id, String purpose, String id, String m_server_ip, String phone_number) {
        super(src, session_id, purpose, id, m_server_ip, phone_number);
        this.sdp = sdp;
    }
}
