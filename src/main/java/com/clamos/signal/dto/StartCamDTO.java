package com.clamos.signal.dto;

import lombok.Data;

@Data
public class StartCamDTO extends CommandDTO {
    private String offer;
    private String type;
    private String eventId;
}
