package com.clamos.signal.dto;

import lombok.Data;

@Data
public class EventStartManagerDTO {
    private String cmd;
    private String tag;
    private String device;
    private EventStartMangerItemDTO data;
}
