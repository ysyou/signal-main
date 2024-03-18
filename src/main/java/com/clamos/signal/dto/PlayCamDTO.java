package com.clamos.signal.dto;

import lombok.Data;

@Data
public class PlayCamDTO extends CommandDTO {
    private String source;
    private String offer;
}
