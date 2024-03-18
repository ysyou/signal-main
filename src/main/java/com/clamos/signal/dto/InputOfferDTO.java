package com.clamos.signal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
public class InputOfferDTO extends InputRtspDTO {
    private String sdp;
    private String eventId;
    private String kind;
    @Builder
    public InputOfferDTO(String sdp, String eventId, String kind, String src, String session_id, String purpose, String phone_number, String m_server_ip) {
        super(src, session_id, purpose, phone_number, m_server_ip);
        this.sdp = sdp;
        this.eventId = eventId;
        this.kind = kind;
    }
}
