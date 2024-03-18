package com.clamos.signal.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventStopMangerDTO {
    private String cmd;
    private String tag;
    private List<String> devices;
    private EventStopManagerItemDTO data;
}
