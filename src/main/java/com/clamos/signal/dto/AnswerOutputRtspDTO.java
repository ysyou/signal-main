package com.clamos.signal.dto;

import lombok.Data;

@Data
public class AnswerOutputRtspDTO {
    private String src;
    private String session_id;
    private String purpose;
    private String sdp;
    private String phone_number;
    private String return_val;
    private String id;
    private String val;
}
